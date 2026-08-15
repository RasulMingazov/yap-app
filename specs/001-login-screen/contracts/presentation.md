# Contract: auth presentation

**Branch**: `feature/001-login-screen` | **Feature ID**: `001-login-screen` | **Refreshed**: 2026-08-15

Both view models follow `docs/mobile/presentation/001-view-models.md`: nested `DataState`,
`UiState`, `News`, `Event`; `internal`; `BaseViewModel`; use cases, preferences, and `Navigator`
only. Each slice keeps its state code at the top and its rendering in a nested `ui` package
(FR-052 … FR-054); what both slices share sits in `presentation/common`.

## `LoginViewModel`

```kotlin
internal class LoginViewModel(
    private val getLegalLinksUseCase: GetLegalLinksUseCase,
    private val loginUseCase: LoginUseCase,
    private val motionPreferences: MotionPreferences,
    private val navigator: Navigator,
    private val newsMapper: LoginNewsMapper,
    private val uiStateMapper: LoginUiStateMapper,
) : BaseViewModel()

data class DataState(
    val isLoggingIn: Boolean = false,
    val legalLinks: LegalLinks = LegalLinks(privacyUrl = null, termsUrl = null),
)

data class UiState(
    val isLoggingIn: Boolean,
    val isMotionReduced: Boolean,
    val privacyUrl: String?,
    val termsUrl: String?,
    val topics: List<StringResource>,
)

sealed interface News {
    data class ShowMessage(val message: StringResource, val argument: StringResource? = null) : News
}

sealed interface Event {
    data object PrimaryActionClicked : Event
    data class ProviderChosen(val provider: AuthProvider) : Event
}
```

| Trigger | Effect |
| --- | --- |
| construction | reads `getLegalLinksUseCase()` into `DataState` (FR-051) |
| `PrimaryActionClicked` while idle | `navigator.navigate(AuthNavKey.SelectAuthProvider)` (FR-003, FR-035) |
| `PrimaryActionClicked` while logging in | ignored (FR-031) |
| `ProviderChosen` while idle | set `isLoggingIn`, `loginUseCase(provider)`, clear it, map the outcome to news |
| `ProviderChosen` while logging in | ignored (FR-031) |

`ProviderChosen` reaches the view model from the composable, which reads it off the navigation
result bus. The view model knows nothing about navigation results. Required to stay absent by FR-040
and SC-020: a sheet visibility flag, a provider list, `platform`, and any provider-to-login map.

## `LoginNewsMapper`

The outcome-to-message table, so the view model carries no copy decision:

| Outcome | News |
| --- | --- |
| `Success`, `Cancelled` | none (FR-029) |
| `Failed` | `ShowMessage(login_failed)` (FR-030) |
| `Unavailable` | `ShowMessage(login_provider_soon, argument = AuthProviderUiMapper(type).labelRes)` (FR-005) |

## `LoginUiStateMapper`

Derives `UiState` from `DataState` and the reduced-motion preference, and supplies the rotating
topic resources. It holds no provider rule: platform visibility and selectability belong to the
roster.

## `SelectAuthProviderViewModel`

```kotlin
internal class SelectAuthProviderViewModel(
    private val navigator: Navigator,
    observeAuthProvidersUseCase: ObserveAuthProvidersUseCase,
    uiStateMapper: SelectAuthProviderUiStateMapper,
) : BaseViewModel()

data class DataState(val providers: List<AuthProvider> = emptyList())

data class UiState(val providers: List<Provider>) {
    data class Provider(val provider: AuthProvider, val ui: AuthProviderUi)
}

sealed interface Event { data object ProviderChosen : Event }
```

| Trigger | Effect |
| --- | --- |
| construction | collects `observeAuthProvidersUseCase()` into `DataState` |
| `ProviderChosen` | `navigator.back()` — the composable has already sent the result (FR-039) |
| dismissal by back, gesture, swipe, or scrim | nothing sent; the login screen stays idle (FR-032) |

## `SelectAuthProviderUiStateMapper`

Builds rows from the roster through `AuthProviderUiMapper`. Providers with `isVisible = false` are
dropped; the row carries the provider itself, so `isEnabled` travels with it rather than being
copied out. Every visible row is tappable and no composable branches on that flag — it is read by
`DefaultLoginUseCase` and the wiring guard. No platform rule, no ordering, no theme value.

## `AuthProviderUi` / `AuthProviderUiMapper` (`presentation/common`)

```kotlin
internal data class AuthProviderUi(
    val iconRes: DrawableResource,
    val isMonochrome: Boolean,
    val labelRes: StringResource,
    val testTag: String,
)

internal class AuthProviderUiMapper {
    operator fun invoke(type: AuthProviderType): AuthProviderUi   // when over the enum
}
```

The one place a provider identity becomes display data (FR-038), shared by both slices so neither
owns a second copy. Marks come from the design ([research.md](../research.md) R25). Adding a
provider adds one branch here and one drawable, and changes no other presentation file (FR-011).

## Composables

| Composable | Package | Note |
| --- | --- | --- |
| `LoginScreen`, `LoginScreenContent` | `presentation/login/ui` | hosts the snackbar and the `ResultEffect`; owns no sheet |
| `LoginSnackbarHost` | `presentation/login/ui` | renders `SnackbarHostState` with vertical entry, upward exit, 2600 ms, reduced-motion aware (FR-048 … FR-050) |
| `LegalLine` | `presentation/login/ui` | two links through `LocalUriHandler`; an unset destination renders but does not navigate (FR-051) |
| `SelectAuthProviderScreen`, `SelectAuthProviderContent` | `presentation/selectprovider/ui` | sheet body only — the scene owns `ModalBottomSheet` and its chrome |

`LoginScreenContent` shows every message with `SnackbarDuration.Indefinite` and lets
`LoginSnackbarHost` own the timer, because the host is the only thing that can also own the motion.
The host holds its own visibility flag and lets an exit finish before dismissing, so a queued
message never replaces the one on screen mid-animation — the queue is one-out-one-in.

Result wiring, receiving on the login screen and sending on the selection screen:

```kotlin
ResultEffect<AuthProvider>(resultKey = AuthResultKeys.PROVIDER_SELECTION) { provider ->
    viewModel.onEvent(LoginViewModel.Event.ProviderChosen(provider))
}

resultEventBus.sendResult(resultKey = AuthResultKeys.PROVIDER_SELECTION, result = provider)
viewModel.onEvent(SelectAuthProviderViewModel.Event.ProviderChosen)
```

The shared explicit key is required — see [feature-auth-api.md](feature-auth-api.md).

Design values the sheet renders: 52 dp minimum row height, 12 dp mark gap, 4 dp side padding, 16 sp
weight 600 in `onSurface` with `accent` when pressed, mark tinted only when `isMonochrome`, and an
uppercased 13 sp section label ([research.md](../research.md) R23, R25).

Test tags: the login screen's tags live in `LoginTestTags` (`login_snackbar`, `login_legal_line`,
`login_primary_action`, …); the section label is in `SelectAuthProviderTestTags`; each provider row
carries the tag its `AuthProviderUi` declares (`login_provider_google`, …).

## Koin (`featureAuthModule`)

```kotlin
fun featureAuthModule(
    googleServerClientId: String,
    privacyUrl: String?,
    termsUrl: String?,
    googleAndroidClientId: String = "",
    googleRedirectUri: String = "",
): Module

single { GoogleProviderLogin(googleAuthRepository = get()) } bind ProviderLogin::class

factory<LoginUseCase> { DefaultLoginUseCase(providerLogins = getAll<ProviderLogin>()) }
factory<ObserveAuthProvidersUseCase> { DefaultObserveAuthProvidersUseCase(platform = get()) }
factory<GetLegalLinksUseCase> { DefaultGetLegalLinksUseCase(LegalLinks(privacyUrl, termsUrl)) }

viewModel { LoginViewModel(...) }
viewModel { SelectAuthProviderViewModel(...) }

navigation<AuthNavKey.Login> { LoginScreen() }
navigation<AuthNavKey.SelectAuthProvider>(metadata = bottomSheetScene()) { SelectAuthProviderScreen() }
```

Each login path is declared by its own type and bound to `ProviderLogin`: two unqualified
`single<ProviderLogin>` definitions would share one Koin index key and override each other.

The wiring guard resolves `LoginUseCase` and `ObserveAuthProvidersUseCase` for real rather than
relying on `verify()` alone — `verify()` examines no dependency of a definition written as
`factory<Contract> { Default(...) }`, because an interface has no constructors. It also asserts that
every selectable provider resolves a handler and that no two handlers claim one provider type.

Adding a provider is one `single … bind ProviderLogin::class`, one roster entry, one
`AuthProviderUiMapper` branch, and one drawable — no view model, no screen, no message (FR-011,
SC-016).
