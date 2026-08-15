# Contract: shared modules (`core-common`, `core-network`, `core-design`, `app-root`)

**Branch**: `feature/001-login-screen` | **Feature ID**: `001-login-screen` | **Refreshed**: 2026-08-15

## Version catalogue

The single `navigation3` reference splits, because the runtime and the UI artefact come from
different publishers and the UI is built against a newer runtime than itself:

```toml
navigation3Runtime = "1.2.0-alpha04"   # androidx.navigation3:navigation3-runtime
navigation3Ui = "1.2.0-alpha02"        # org.jetbrains.androidx.navigation3:navigation3-ui
```

Rationale and compatibility checks: [research.md](../research.md) R19. It is the one deliberate
exception to the "pin a stable release" rule of R11.

## `core-common` — navigation intent

```kotlin
package app.yap.core.common.navigation

interface Navigator {

    fun navigate(key: NavKey)

    fun back()
}
```

A view model reports intent and never touches a back stack. `core-common` gains
`api(libs.navigation3.runtime)` for `NavKey` — the same narrow dependency `feature-auth/api`
declares, and for the same reason.

## `core-common` — platform capabilities

```kotlin
package app.yap.core.common.platform

expect fun currentPlatform(): Platform     // ANDROID | IOS

interface MotionPreferences { fun isReduced(): Boolean }
```

`MotionPreferences` is a contract in `commonMain` with a narrow adapter per platform, not an
`expect class`: the Android reading needs a `Context` and the iOS one does not, and a mapper test
must be able to hand the mapper a chosen answer ([research.md](../research.md) R7).
`ActivityProvider` exists in `androidMain` only, holding the resumed Activity for Credential
Manager (R1).

## `core-network` — typed HTTP outcomes

```kotlin
package app.yap.core.network

sealed interface ApiResult<out T> {
    data class Success<out T>(val value: T) : ApiResult<T>
    data class Failure(val error: ApiError) : ApiResult<Nothing>
}

sealed interface ApiError {
    data class Rejected(val code: String?) : ApiError   // 4xx carrying a shared ApiErrorCode
    data object Unauthorized : ApiError                 // 401 / 403
    data object Unavailable : ApiError                  // transport failure, 5xx, 408, 429
    data object Malformed : ApiError                    // 2xx the caller cannot decode
}

class ApiClient(networkClient: NetworkClient) {
    suspend fun call(method: HttpMethod, path: String, authenticated: Boolean,
                     configure: HttpRequestBuilder.() -> Unit): ApiCall
}

suspend inline fun <reified R> ApiClient.get/post/put/delete(
    path: String, authenticated: Boolean = true, noinline configure: HttpRequestBuilder.() -> Unit = {},
): ApiResult<R>

suspend fun ApiClient.send(...): ApiResult<Unit>
```

Every feature calls `ApiClient` and consumes `ApiResult`; no feature reads a status code and none
declares its own failure type. Requests are authenticated by default — the auth endpoints pass
`authenticated = false` explicitly. `ApiClient` reads a refusal body as `ErrorResponseDto` from
`shared/contract/common`, so the code the server names is the code the client matches on.

The split between `Unauthorized`/`Rejected` and `Unavailable` is what FR-025 rests on: only the
first two are the server answering about the session, and only they may sign a user out.

## `core-design` — theme

```kotlin
package app.yap.core.design.theme

@Immutable
data class YapColors(
    val accent: Color, val action: Color, val background: Color, val bodyMuted: Color,
    val caption: Color, val handle: Color, val highlight: Color, val link: Color,
    val notice: Color, val onAction: Color, val onBackground: Color, val onHighlight: Color,
    val onNotice: Color, val onSurface: Color, val outline: Color, val scrim: Color,
    val sectionLabel: Color, val surface: Color,
)

object YapTheme {
    val colors: YapColors @Composable @ReadOnlyComposable get() = LocalYapColors.current
}

@Composable
fun YapTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit)
```

`YapTheme` provides `LocalYapColors` and a `ColorScheme` derived from the same values. Values per
role and theme, with the design key each was lifted from: [research.md](../research.md) R23. This is
the one file where colour literals belong (FR-042, FR-043).

## `core-design` — bottom-sheet destinations

```kotlin
package app.yap.core.design.navigation

class BottomSheetSceneStrategy<T : Any> : SceneStrategy<T>

fun bottomSheetScene(properties: ModalBottomSheetProperties = ModalBottomSheetProperties()): Map<String, Any>
```

`bottomSheetScene()` builds entry metadata; a feature attaches it in its own Koin module:

```kotlin
navigation<AuthNavKey.SelectAuthProvider>(metadata = bottomSheetScene()) { SelectAuthProviderScreen() }
```

The strategy follows `AnimatedBottomSheetSample` from the `navigation3-ui` samples: an
`OverlayScene` whose `onRemove()` awaits `sheetState.hide()`, so the sheet animates out before the
entry leaves composition. It applies the design's sheet chrome — 24 dp top corners, `outline` top
border, the `handle` drag indicator, and the `scrim` — so no feature repeats them. `BottomSheetScene`
carries value `equals`/`hashCode`, as `DialogScene` does, so a recomposition does not re-key the
scene.

It lives here, not in `app-root`: `app-root` composes `NavDisplay` with it and the feature attaches
the metadata, and a feature may not depend on `app-root`.

## `app-root` — back stack, `NavDisplay`, launch refresh

`RootBackStack` is a `single` implementing `Navigator`:

- keeps its session-derived base — `Login` when logged out, `Main` when logged in, empty when
  unknown;
- gains a mutable tail that `navigate` pushes and `back` pops;
- resets the tail only when the base actually changes, comparing against the base a destination was
  pushed onto, so a lifecycle STOP-START does not drop an open sheet;
- does not push a destination already on top, so one key never occupies two entries.

```kotlin
NavDisplay(
    backStack = keys,
    onBack = rootBackStack::back,
    entryDecorators = entryDecorators,      // saveable state, view-model store, result event bus
    sceneStrategies = sceneStrategies,      // BottomSheetSceneStrategy, SinglePaneSceneStrategy
    entryProvider = koinEntryProvider(),
)
```

`entryDecorators` must list the saveable-state decorator explicitly: passing the parameter replaces
the default list rather than adding to it. The view-model decorator is listed with it so a
destination's view models are scoped to its entry and cleared when the entry pops. Both lists are
`remember`ed: a fresh list on each composition re-keys `rememberSceneState` and tears down an open
overlay. `NavDisplay` is composed only once `keys` is non-empty, which is what keeps the splash up
while session state is `Unknown`.

`app-root` also owns `RootNavKey.Main`, registered by `appRootModule()` so `koinEntryProvider()` can
resolve it, and `LaunchSessionRefresh`, which awaits the first resolved session state and calls
`RefreshSessionUseCase` when it is `LoggedIn` ([research.md](../research.md) R15).

Restoration: the singleton survives configuration change, not process death. A sheet open at process
death reopens as the login screen — stated rather than implied (R22).

## `app-root` — composition root

```kotlin
fun initKoin(
    baseUrl: String,
    googleServerClientId: String,
    privacyUrl: String?,
    termsUrl: String?,
    googleAndroidClientId: String = "",
    googleRedirectUri: String = "",
    appDeclaration: KoinAppDeclaration = {},
): KoinApplication
```

Every environment value is a parameter, never global state. `appModules` lists `featureAuthModule()`
ahead of `coreNetworkModule(baseUrl)`, because `NetworkClient` installs the access-token modifier
from `getOrNull<AccessTokenProvider>()` at construction time.
