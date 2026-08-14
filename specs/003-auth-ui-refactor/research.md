# Research: Auth Provider Selection and Login Theming Refactor

Every decision below was checked against artifacts that actually resolve — `koin` 4.2.2,
`composeMaterial3` 1.9.0, and both the current and the proposed `navigation3` versions — and against
the design project itself, not against documentation for a version we do not use.

**Design source**: Claude Design project `0c49e08b-d7ab-4cd3-88be-8483024790e5`, file
`screen_login.dc.html`. Its `themeVals()` block holds the exact light and dark palette and its markup
holds the sheet chrome, the provider marks, and the snackbar. Values below are lifted from it rather
than invented.

## R1 — The shape of `AuthProvider`

**Decision**: `AuthProvider` becomes a sealed hierarchy carrying both flags per instance, constructed
at runtime. Shown here because the rationale below argues about its exact shape; the normative copy is
[contracts/feature-auth-api.md](contracts/feature-auth-api.md).

```kotlin
sealed interface AuthProvider {

    val isEnabled: Boolean
    val isVisible: Boolean

    data class Apple(override val isEnabled: Boolean, override val isVisible: Boolean) : AuthProvider

    data class Google(override val isEnabled: Boolean, override val isVisible: Boolean) : AuthProvider

    data class TId(override val isEnabled: Boolean, override val isVisible: Boolean) : AuthProvider
}
```

**Rationale**: This is the requester's shape and it resolves the tension an enum could not. Both flags
sit on the provider exactly as `fix.md` asked, and because the members are *instances* rather than
constants, both are runtime values — the roster sets them per platform today and a backend can set
them later. `ObserveAuthProvidersUseCase(): Flow<List<AuthProvider>>` then reads literally.

**Sealed interface with `data class` members rather than `sealed class` with constructor
parameters**: the codebase already expresses every sealed hierarchy this way — `AuthState`,
`LoginOutcome`, `GoogleCredential` — and `data class` members give `equals` and `hashCode` for free.
A `sealed class` holding the flags in its own constructor cannot have `data class` subclasses without
redeclaring the flags, which would leave tests comparing rows by identity instead of by value.

**Consequence — identity for the registry**: with instances rather than constants, "which provider is
this" is the subclass, so `ProviderLogin` keys on `KClass<out AuthProvider>` (R2). The selection
mapper branches with `is` on every arm, as `docs/001-code-conventions.md` requires.

**Alternatives considered**:

- An enum with both flags, exactly as `fix.md` wrote it. Rejected during clarification: `APPLE` would
  appear on Android, and a constant cannot be driven by a backend.
- An enum plus a separate availability model. Rejected by the requester in favour of this shape.

## R2 — Registering a login path per provider

**Decision**: A domain port carries its own identity:

```kotlin
internal interface ProviderLogin {
    val provider: KClass<out AuthProvider>
    suspend fun login(): LoginOutcome
}
```

Each implementation is bound as `single<ProviderLogin> { ... }`. `DefaultLoginUseCase` receives them
through Koin's `getAll<ProviderLogin>()` and indexes them by that key. Adding a provider is one Koin
declaration plus one implementation.

**Rationale**: `getAll<T>()` is public Koin API — `koin-compose-navigation3` uses it internally to
collect `EntryProviderInstaller`s — so this is the same collection mechanism the navigation DSL
already relies on. The map is built where the handlers are declared, not in a view model.

**Alternatives considered**:

- `Map<AuthProvider, LoginUseCase>` in the graph. Rejected: with instance-carrying providers the enum
  key no longer exists, and every new provider would edit one central `mapOf`.
- Qualified bindings resolved on demand. Rejected: failures would surface at tap time instead of at
  wiring-verification time.

**Consequence**: `LoginUseCase` becomes `suspend operator fun invoke(provider: AuthProvider)`, and
`LoginOutcome` gains `Unavailable` for a provider with no registered handler. The 60-second attempt
bound moves from `GoogleLoginUseCase` into `DefaultLoginUseCase`, covering every provider instead of
Google alone.

**Guard**: a wiring test asserts that every provider the roster marks `isEnabled` resolves a handler.

## R3 — Navigation 3 upgrade

**Decision**: raise Navigation 3 so the official result API is available.

| Artefact | Now | After |
| --- | --- | --- |
| `androidx.navigation3:navigation3-runtime` | 1.1.1 | **1.2.0-alpha04** |
| `org.jetbrains.androidx.navigation3:navigation3-ui` | 1.1.1 | **1.2.0-alpha02** |

The two versions differ on purpose: JetBrains publishes only the UI artefact — a request for
`org.jetbrains.androidx.navigation3:navigation3-runtime` returns 404 — and `navigation3-ui`
1.2.0-alpha02 declares `androidx.navigation3:navigation3-runtime:1.2.0-alpha04`. Pinning the runtime
to what the UI was built against is more honest than letting conflict resolution pick it. The single
`navigation3` version reference in the catalogue therefore splits into two.

**Why an alpha**: `androidx.navigation3.runtime.result` — the official result API — does not exist in
1.1.1 and first appears in 1.2.0-alpha02. There is no stable release carrying it. The requester chose
the official mechanism over a hand-rolled one, and this is its cost.

**Verified as unchanged across the bump** (checked by diffing the published sources):

- The runtime's file set is identical apart from the added `result` package.
- `NavEntry.metadata` is still `Map<String, Any>`, so the scene strategy still reads
  `entry.metadata[Key]`. The `(K) -> Map<String, Any>` signatures added in 1.2.0 are new
  `entryProvider` overloads, not a change to `NavEntry`.
- `EntryProviderScope.entry(content, metadata: Map<String, Any>)` still exists, which is the exact
  call `koin-compose-navigation3` 4.2.2 compiles against.
- `NavDisplay(backStack, onBack, entryDecorators, sceneStrategies, entryProvider)` is unchanged;
  `OverlayScene.onRemove()` and `SceneStrategyScope.onBack` are both still present.

**New transitive dependency**: `org.jetbrains.androidx.navigationevent:navigationevent-compose` for
predictive back. `navigation3-ui` also declares lifecycle 2.10.0, which resolves up to the project's
pinned 2.11.0.

**Risk**: Koin 4.2.2 and `lifecycle-viewmodel-navigation3` 2.11.0 were built against 1.1.x. The API
surface each uses is unchanged, so this should link, but it is a link-time fact, not a reading-time
one — `./gradlew build` plus the iOS compile is the check, and it runs first in the phase. Koin 4.2.2
is the latest release, so there is no newer Koin to escape to. **If the check fails, work stops and
the decision returns to the requester**; no fallback is pre-authorised (clarified 2026-08-14).

## R4 — Returning the chosen provider, officially

**Decision**: use `androidx.navigation3.runtime.result`. No feature-owned channel, no extra use
cases.

- `App()` adds `rememberResultEventBusNavEntryDecorator<NavKey>()` to `NavDisplay`'s
  `entryDecorators`, alongside the default `rememberSaveableStateHolderNavEntryDecorator()`.
- The selection screen sends the choice: `LocalResultEventBus.current.sendResult<AuthProvider>(...)`,
  then asks its view model to navigate back.
- The login screen receives it: `ResultEffect<AuthProvider> { provider -> onEvent(ProviderChosen(provider)) }`.

**Rationale**: The requester asked for the official solution rather than an invented one. The bus
keys results by `T::class.toString()`, so the type argument must be written explicitly as
`AuthProvider` — with a sealed hierarchy, an inferred `Google` would key the result by the subclass
and the effect listening for `AuthProvider` would never fire. That is the one sharp edge of this API
and it is worth a comment in the code.

**What this deletes from the earlier plan**: `AuthProviderChoice`, `ChooseAuthProviderUseCase`,
`ObserveChosenAuthProviderUseCase`, and with them the whole question of whether a view model may hold
a data source. The result now arrives in the composable and enters the view model as an ordinary
event, which is the shape the view-model guide already describes.

**Alternatives considered**: `ResultEventBus.conflateAsState`, the state-based variant from the same
API. Rejected: a login is a one-shot action, and a conflated state would replay the last choice on
recomposition.

## R5 — The selection screen as a destination

**Decision**: `AuthNavKey.SelectAuthProvider` is a real destination rendered by a custom
`BottomSheetSceneStrategy`, following `AnimatedBottomSheetSample` shipped in the `navigation3-ui`
samples.

**Rationale**: Navigation 3 ships no bottom-sheet scene strategy at any version; the official sample
writes one over `OverlayScene`, `SceneStrategy`, and `SceneStrategyScope`. Its `onRemove()` override
is the piece that matters — it awaits `sheetState.hide()` before the entry leaves composition, so the
sheet animates out instead of vanishing.

`metadata { put(key, value) }` builds a `Map<String, Any>`, and Koin's
`Module.navigation<T>(metadata: Map<String, Any>, ...)` takes exactly that type, so the marker is
attached from the feature's own Koin module with no change to the DSL.

**Alternatives considered**: keeping the sheet as a composable driven by a flag in `UiState`.
Rejected twice over — the requester asked for a full destination, and
`docs/mobile/presentation/002-ui-compose.md` already forbids a sheet visibility flag in `UiState`,
which is what the current screen has.

## R6 — A mutable back stack and a `Navigator`

**Decision**: `RootBackStack` becomes a `single` owning a mutable list: an auth-derived base
(`Login` or `Main`) plus pushed keys. It implements a `Navigator` contract in `core-common`
(`navigate(key)` / `back()`), bound in `app-root`. `App()` passes `onBack`, the bottom-sheet scene
strategy, and the result decorator to `NavDisplay`.

**Rationale**: `docs/mobile/presentation/001-view-models.md` already prescribes `Navigator` and
forbids navigation through `News`, but no module provides one — the current back stack is a derived
`Flow<List<NavKey>>` with no push. This is the first feature that needs it, which is when the
constitution allows the abstraction to appear.
`docs/mobile/003-dependency-injection.md` requires the back stack to stay in `app-root` and never be
passed to a feature, so the feature sees only the contract.

**Alternatives considered**: `rememberNavBackStack` in `App()`. Rejected: the base of the stack is
derived from auth state inside a Koin-owned component, and a composable-owned stack cannot be reset
from there without duplicating that logic.

**Known limitation, stated rather than hidden**: the singleton survives configuration change but not
process death, so a sheet open at process death reopens as the login screen. Acceptable for a
transient chooser.

**Module placement**: `Navigator` in `core-common`, which gains `api(libs.navigation3.runtime)` — the
same narrow dependency `feature-auth/api` already declares, and for the same reason.
`BottomSheetSceneStrategy` and its metadata helper in `core-design`, the shared Compose module, since
`app-root` composes `NavDisplay` with it while `feature-auth` attaches the metadata, and a feature
may not depend on `app-root`.

## R7 — Colours, lifted from the design

**Decision**: `core-design` gains `YapColors`, an immutable set of role-named colours provided
through `LocalYapColors`, plus a Material `ColorScheme` derived from the same values. The login slice
reads `YapTheme.colors`; `LoginColors.kt` and `LocalIsDarkTheme` are deleted.

| Role | Light | Dark | Design key |
| --- | --- | --- | --- |
| `background` / `onBackground` | `FFFEF7` / `0B0A0D` | `08070A` / `FAF9F6` | `bg` / `fg` |
| `surface` / `onSurface` | `FFFEF7` / `0B0A0D` | `15141A` / `FAF9F6` | `sheetBg` / `providerColor` |
| `accent` | `5E3689` | `D9FF57` | `topicColor`, also `providerHover` |
| `action` / `onAction` | `0B0A0D` / `FFFAFC` | `D9FF57` / `0B0A0D` | `buttonBg` / `buttonFg` |
| `bodyMuted` | `5F5A6B` | `8F8899` | `bodyColor` |
| `caption` | `8B8496` | `5B5765` | `captionColor` |
| `sectionLabel` | `8B8496` | `7C7787` | `sheetLabelColor` |
| `link` | `0B0A0D` | `FAF9F6` | anchor colour |
| `notice` / `onNotice` | `5E3689` / `FFFAFC` | **`5E3689` / `FFFAFC`** | `snackBg` / `snackFg` |
| `highlight` / `onHighlight` | `D9FF57` / `0B0A0D` | `D9FF57` / `0B0A0D` | marquee band |
| `outline` | `0B0A0D` at 10% | `E2E2E2` at 14% | `sheetBorder` |
| `handle` | `0B0A0D` at 20% | `E2E2E2` at 25% | `sheetHandle` |
| `scrim` | `3C3742` at 35% | `050406` at 55% | `sheetOverlay` |

The single value that changes from today's code is `notice` in dark theme: `26232C` becomes
`5E3689`. The design has always specified one snackbar colour for both themes — the current dark
value was never in it.

The last three roles are new because the old sheet used Material defaults for its scrim, border, and
drag handle. The rebuilt sheet takes them from the design.

**Rationale**: the palette does not fit Material's slot set — the primary action is near-black in
light while `accent` is purple, the snackbar is fixed across themes, and the marquee band has no slot
at all. Bending it to fit produces mappings nobody can read back. Keeping `YapColors` as the source
of truth and deriving the `ColorScheme` from it gives `ModalBottomSheet`, `Snackbar`, and `Button`
correct defaults without inventing meanings for slots.

## R8 — Snackbar mechanism, motion, and timing

**Decision**: use `SnackbarHostState` for queueing, one-at-a-time display, and dismissal; render it
with a feature-owned host that enters and exits with vertical motion.

**Rationale**: `SnackbarHostState` is the standard mechanism — its mutex is documented as a fair
queue and `showSnackbar` suspends until the current snackbar disappears. What it cannot provide is
the motion: `SnackbarHost` renders through a private `FadeInFadeOutWithScale` carrying the upstream
comment *"TODO: to be replaced with the public customizable implementation"*. There is no parameter
to change it. Owning roughly 25 lines of host is the smallest way to get the required motion while
keeping the standard state machine.

**Values from the design** (`ssSnackIn`, and the snackbar block):

| Property | Value |
| --- | --- |
| Position | top of the screen, 10 dp below the safe area, 20 dp side margins |
| Shape | 14 dp corner radius, padding 12 dp × 18 dp |
| Type | 14 sp, weight 600, centred |
| Enter | 220 ms ease-out, from 8 dp below with fade |
| Exit | upward with fade — the requested change; the prototype has no exit |
| Duration | **2600 ms** |

The duration is the design's, not the current code's 4000 ms and not Material's `Short` (also 4000
ms). Since the host owns the timer anyway — `SnackbarDuration.toMillis` is `internal` — following the
design costs nothing.

**Reduced motion**: the host swaps both transitions for `EnterTransition.None` / `ExitTransition.None`
so the message still displays for its full duration without motion. The preference governs the
message only — the selection sheet keeps `ModalBottomSheet`'s standard animation (clarified
2026-08-14), so `MotionPreferences` does not reach the selection slice at all.

## R9 — Provider marks, lifted from the design

**Decision**: three vector drawables in `feature-auth/impl/src/commonMain/composeResources/drawable/`,
converted verbatim from the design's markup.

| Provider | Mark | Size | Tint |
| --- | --- | --- | --- |
| Google | the four-path multicolour "G" — `#4285F4`, `#34A853`, `#FBBC05`, `#EA4335` | 20 dp | none, the mark carries its own colours |
| Apple | the single-path logo, drawn in the row's content colour | 19 dp | `onSurface` |
| T-ID | a 20 dp rounded square, 6 dp radius, `#FFDD2D`, with a dark "T" | 20 dp | none |

The initial-letter-on-accent circle currently in `AuthProviderSheet` appears nowhere in the design and
is removed.

**Consequence for the row model**: the marks are not uniform — Google and T-ID carry brand colours,
Apple follows the theme. The row therefore carries `isMonochrome`, set by the selection mapper. That
is a fact about the asset, not a theme value, so the mapper may own it; the composable branches on
the flag rather than on the provider, which is what
`docs/mobile/presentation/002-ui-compose.md` requires.

Brand colours live inside the drawables, never in `YapColors` — the theme describes the product's
roles, not other companies' palettes.

**Rationale**: the previous plan dropped the icon field because the repository holds no drawable and
`DesignSync.list_projects` returned nothing. Both were wrong conclusions from incomplete checks: the
design project is reachable by id, and it specifies all three marks exactly.

## R10 — Sheet chrome, lifted from the design

| Element | Value |
| --- | --- |
| Corner radius | 24 dp, top corners only |
| Top border | 1 dp, `outline` |
| Drag handle | 36 × 4 dp, fully rounded, `handle` |
| Padding | 10 dp top, 20 dp sides, 20 dp bottom plus the safe area |
| Section label | 13 sp, weight 700, uppercase, 0.04 em tracking, `sectionLabel` |
| Row | 52 dp minimum height, 12 dp gap, 4 dp side padding, 16 sp weight 600, `onSurface` |
| Pressed row | `accent` |
| Scrim | `scrim` |

## R11 — Behaviour changes the tests must follow

Three user-visible behaviours change, and existing tests encode the old ones:

- Provider rows gain real marks and the sheet gains a handle, a border, and design paddings.
- The transient message becomes a snackbar with vertical motion and a 2600 ms duration;
  `LoginScreenBannerTest` and `LoginScreenMotionTest` assert the current banner.
- The sheet becomes its own destination, so `LoginScreenContentTest` and `LoginScreenSemanticsTest`
  lose their sheet assertions to the new slice's tests.

Per Principle III each lands as a failing test first. `StubAuthProviderDeclaration` disappears with
the catalogue and is replaced by a stub of the roster use case.

## R12 — The unavailable-provider path

**Decision**: exactly the design's flow. Every visible row is tappable; choosing one closes the sheet
and returns the provider as a navigation result; `LoginViewModel` calls `loginUseCase(provider)` and
gets `Unavailable` back for a provider with no login path; the message appears on the login screen.
The selection screen only reports the choice — it never calls a use case and never decides an
outcome.

**The copy problem and its resolution**: the design's wording is per provider — *"Вход через Apple
скоро появится"*. Branching over providers inside `LoginViewModel` to pick it would break the rule
that adding a provider touches neither the login view model nor its screen. Instead the wording is
one parameterised string, `login_provider_soon` = *"Вход через %1$s скоро появится"*, and its
argument is the provider's label, taken from the one table that already maps a provider to its
display data:

```kotlin
internal object AuthProviderResources {
    fun labelOf(provider: AuthProvider): StringResource
    fun markOf(provider: AuthProvider): ProviderMark    // iconRes + isMonochrome
}
```

`SelectAuthProviderUiStateMapper` uses it to build rows; `LoginViewModel` uses `labelOf` to fill the
message. Adding a provider adds one entry to this table and nothing else — no message branch
anywhere, and the login view model is untouched. `News.ShowMessage` gains an optional
`argument: StringResource?`; the screen already resolves nested resources this way for
`login_provider_semantics`.

`login_provider_not_available`, today's generic wording, is replaced by `login_provider_soon`.

**Alternatives considered**:

- Keeping the generic message. Rejected: the design specifies the wording, and matching it turned out
  to cost one optional field rather than a per-provider branch.
- Keeping the sheet open and showing the message from its own mapper. Rejected by the requester: the
  sheet closes on selection, as the design stages it.
- Per-provider message resources. Rejected: three strings that differ only by a name, and every new
  provider would add a fourth.
