# Contract: login presentation

Both view models follow `docs/mobile/presentation/001-view-models.md`: nested `DataState`, `UiState`,
`News`, `Event`; `internal`; `BaseViewModel`; use cases and `Navigator` only.

## `LoginViewModel`

```kotlin
internal class LoginViewModel(
    private val loginUseCase: LoginUseCase,
    private val motionPreferences: MotionPreferences,
    private val navigator: Navigator,
    private val privacyUrl: String?,
    private val termsUrl: String?,
) : BaseViewModel()

data class DataState(val isLoggingIn: Boolean = false)

data class UiState(
    val isLoggingIn: Boolean,
    val isMotionReduced: Boolean,
    val privacyUrl: String?,
    val termsUrl: String?,
    val topics: List<StringResource>,
)

sealed interface News {
    data class ShowMessage(
        val message: StringResource,
        val argument: StringResource? = null,
    ) : News
}

sealed interface Event {
    data object PrimaryActionClicked : Event
    data class ProviderChosen(val provider: AuthProvider) : Event
}
```

Behaviour:

| Trigger | Effect |
| --- | --- |
| `PrimaryActionClicked` while idle | `navigator.navigate(AuthNavKey.SelectAuthProvider)` |
| `PrimaryActionClicked` while logging in | ignored |
| `ProviderChosen` while idle | set `isLoggingIn`, `loginUseCase(provider)`, clear it, handle the outcome |
| `ProviderChosen` while logging in | ignored |
| `Success` / `Cancelled` | no news |
| `Failed` | `ShowMessage(login_failed)` |
| `Unavailable` | `ShowMessage(login_provider_soon, AuthProviderResources.labelOf(provider))` |

Gone: `ProviderSheetDismissed`, `isProviderSheetVisible`, `providers`, `platform`, `declarations`,
`Map<AuthProvider, LoginUseCase>`.

`ProviderChosen` reaches the view model from the composable, which reads it off the navigation result
bus. The view model itself knows nothing about navigation results.

## `SelectAuthProviderViewModel`

```kotlin
internal class SelectAuthProviderViewModel(
    private val navigator: Navigator,
    observeAuthProvidersUseCase: ObserveAuthProvidersUseCase,
) : BaseViewModel()

data class DataState(val providers: List<AuthProvider> = emptyList())

data class UiState(val providers: List<Provider>) {
    data class Provider(
        val iconRes: DrawableResource,
        val isEnabled: Boolean,
        val isMonochrome: Boolean,
        val labelRes: StringResource,
        val provider: AuthProvider,
    )
}

sealed interface Event { data object ProviderChosen : Event }
```

Behaviour:

| Trigger | Effect |
| --- | --- |
| collection starts | subscribes to `observeAuthProvidersUseCase()`, updates `DataState` |
| `ProviderChosen` | `navigator.back()` — the composable has already sent the result |
| dismissal by back, gesture, or scrim | nothing sent; the login screen stays idle |

## `AuthProviderResources`

The one table mapping a provider to display data. Internal to `presentation`, shared by the two
slices so that neither owns a second copy.

```kotlin
internal object AuthProviderResources {

    fun labelOf(provider: AuthProvider): StringResource

    fun markOf(provider: AuthProvider): ProviderMark   // iconRes + isMonochrome
}

is AuthProvider.Apple  -> login_provider_apple,  ic_provider_apple,  isMonochrome = true
is AuthProvider.Google -> login_provider_google, ic_provider_google, isMonochrome = false
is AuthProvider.TId    -> login_provider_t_id,   ic_provider_t_id,   isMonochrome = false
```

`is` on every branch, per `docs/001-code-conventions.md`. Marks come from the design; their
specifications are in [research.md](../research.md) R9. Adding a provider adds one entry here and
changes no other presentation file.

## `SelectAuthProviderUiStateMapper`

Builds rows from the roster using `AuthProviderResources`.

- Providers with `isVisible = false` are dropped.
- `isEnabled` is copied from the instance — carried, never rendered. Every visible row is tappable;
  the flag is read by `DefaultLoginUseCase`, which returns `Unavailable` for a disabled provider, and
  by the wiring guard. No composable may branch on it.
- No platform rule, no ordering, no theme value — the roster owns the first two and the theme the
  third.

## Composables

| Composable | Package | Note |
| --- | --- | --- |
| `LoginScreen`, `LoginScreenContent` | `presentation/login/ui` | loses the sheet block; gains the snackbar host and the `ResultEffect` |
| `LoginSnackbarHost` | `presentation/login/ui` | `SnackbarHostState` + vertical entry and exit, reduced-motion aware, design timing |
| `SelectAuthProviderScreen`, `SelectAuthProviderContent` | `presentation/selectprovider/ui` | sheet body only — the scene owns `ModalBottomSheet` and its chrome |
| `AuthProviderSheet` | — | deleted; its rows move to the new slice |

Receiving the result, on the login screen:

```kotlin
ResultEffect<AuthProvider> { provider -> onEvent(Event.ProviderChosen(provider)) }
```

Sending it, on the selection screen:

```kotlin
val resultEventBus = LocalResultEventBus.current
SelectAuthProviderContent(
    uiState = uiState,
    onProviderChosen = { provider ->
        resultEventBus.sendResult<AuthProvider>(provider)
        onEvent(Event.ProviderChosen)
    },
)
```

The explicit `<AuthProvider>` type argument is required — see [feature-auth-api.md](feature-auth-api.md).

Test tags: `LoginTestTags.BANNER` becomes `SNACKBAR`; provider row tags keep the
`login_provider_<name>` shape and move to `SelectAuthProviderTestTags`.

## Koin (`featureAuthModule`)

```kotlin
single<ProviderLogin> { GoogleProviderLogin(authRepository = get()) }

factory<LoginUseCase> { DefaultLoginUseCase(providerLogins = getAll<ProviderLogin>()) }
factory<ObserveAuthProvidersUseCase> { DefaultObserveAuthProvidersUseCase(platform = get()) }

viewModel { LoginViewModel(...) }
viewModel { SelectAuthProviderViewModel(...) }

navigation<AuthNavKey.Login> { LoginScreen() }
navigation<AuthNavKey.SelectAuthProvider>(metadata = bottomSheetScene()) { SelectAuthProviderScreen() }
```

Adding a provider is one `single<ProviderLogin>`, one roster entry, one `AuthProviderResources`
entry, and one drawable — no view model, no screen, and no message.
