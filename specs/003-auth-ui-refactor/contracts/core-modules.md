# Contract: shared modules (`core-common`, `core-design`, `app-root`)

## Version catalogue

The single `navigation3` reference splits, because the runtime and the UI artefact come from
different publishers and the UI is built against a newer runtime than itself:

```toml
navigation3Runtime = "1.2.0-alpha04"   # androidx.navigation3:navigation3-runtime
navigation3Ui = "1.2.0-alpha02"        # org.jetbrains.androidx.navigation3:navigation3-ui
```

Rationale and the compatibility checks behind these numbers: [research.md](../research.md) R3.

## `core-common` — navigation intent

```kotlin
package app.yap.core.common.navigation

interface Navigator {

    fun navigate(key: NavKey)

    fun back()
}
```

A view model reports intent and never touches a back stack. `core-common` gains
`api(libs.navigation3.runtime)` for `NavKey` — the same narrow dependency `feature-auth/api` already
declares, and for the same reason: the type appears on the module's own API surface.

## `core-design` — theme

```kotlin
package app.yap.core.design.theme

@Immutable
data class YapColors(
    val accent: Color,
    val action: Color,
    val background: Color,
    val bodyMuted: Color,
    val caption: Color,
    val handle: Color,
    val highlight: Color,
    val link: Color,
    val notice: Color,
    val onAction: Color,
    val onBackground: Color,
    val onHighlight: Color,
    val onNotice: Color,
    val onSurface: Color,
    val outline: Color,
    val scrim: Color,
    val sectionLabel: Color,
    val surface: Color,
)

object YapTheme {
    val colors: YapColors
        @Composable @ReadOnlyComposable get() = LocalYapColors.current
}

@Composable
fun YapTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit)
```

- `YapTheme` provides `LocalYapColors` and a `ColorScheme` derived from the same values.
- `LocalIsDarkTheme` is removed.
- Values per role and theme, with the design key each was lifted from:
  [research.md](../research.md) R7.

## `core-design` — bottom-sheet destinations

```kotlin
package app.yap.core.design.navigation

class BottomSheetSceneStrategy<T : Any> : SceneStrategy<T>

fun bottomSheetScene(
    properties: ModalBottomSheetProperties = ModalBottomSheetProperties(),
): Map<String, Any>
```

`bottomSheetScene()` builds entry metadata; a feature attaches it in its own Koin module:

```kotlin
navigation<AuthNavKey.SelectAuthProvider>(metadata = bottomSheetScene()) {
    SelectAuthProviderScreen()
}
```

The strategy follows `AnimatedBottomSheetSample` from the `navigation3-ui` samples: an `OverlayScene`
whose `onRemove()` awaits `sheetState.hide()`, so the sheet animates out before the entry leaves
composition. It applies the design's sheet chrome — 24 dp top corners, `outline` top border, the
`handle` drag indicator, and the `scrim` — so no feature repeats them.

`core-design` gains the `yap.navigation3` plugin. The strategy lives here, not in `app-root`:
`app-root` composes `NavDisplay` with it and the feature attaches the metadata, and a feature may not
depend on `app-root`.

## `app-root` — back stack and `NavDisplay`

`RootBackStack` becomes a `single` implementing `Navigator`:

- keeps its auth-derived base — `Login` when logged out, `Main` when logged in, empty when unknown;
- gains a mutable tail that `navigate` pushes and `back` pops;
- resets the tail whenever the base changes.

```kotlin
NavDisplay(
    backStack = keys,
    onBack = rootBackStack::back,
    entryDecorators = listOf(
        rememberSaveableStateHolderNavEntryDecorator(),
        rememberViewModelStoreNavEntryDecorator(),
        rememberResultEventBusNavEntryDecorator(),
    ),
    sceneStrategies = listOf(BottomSheetSceneStrategy(), SinglePaneSceneStrategy()),
    entryProvider = koinEntryProvider<NavKey>(),
)
```

`entryDecorators` must list the saveable-state decorator explicitly: passing the parameter replaces
the default list rather than adding to it. The view-model decorator is listed with it so a
destination's view models are scoped to its entry and cleared when the entry pops — without it the
selection sheet's view model would live on the Activity's store and outlive every sheet. Both lists
are `remember`ed: a fresh list on each composition re-keys `rememberSceneState` and tears down an
open overlay.

Restoration: the singleton survives configuration change, not process death. A sheet open at process
death reopens as the login screen — stated in [research.md](../research.md) R6 rather than implied.
