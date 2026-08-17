# Contract: `feature-auth/api`

**Branch**: `feature/001-login-screen` | **Feature ID**: `001-login-screen` | **Refreshed**: 2026-08-15

The public surface other modules may see. Everything else stays `internal` in `impl`. The HTTP
surface and the feature's internal credential port are in [auth-api.md](auth-api.md).

## Navigation keys

```kotlin
package app.yap.feature.auth.api

@Serializable
sealed interface AuthNavKey : NavKey {

    @Serializable data object Login : AuthNavKey

    @Serializable data object SelectAuthProvider : AuthNavKey
}
```

Both are composed by `featureAuthModule()` in `impl`; `SelectAuthProvider` carries
`bottomSheetScene()` metadata (FR-035).

## Entities

```kotlin
package app.yap.feature.auth.api.entity

sealed interface AuthSessionState {
    data object Unknown : AuthSessionState
    data object LoggedOut : AuthSessionState
    data class LoggedIn(val userId: UserId) : AuthSessionState
}

enum class AuthProviderType { APPLE, GOOGLE, T_ID }

data class AuthProvider(
    val type: AuthProviderType,
    val isEnabled: Boolean,
    val isVisible: Boolean,
)

sealed interface LoginOutcome {
    data object Success : LoginOutcome
    data object Cancelled : LoginOutcome
    data object Failed : LoginOutcome
    data object Unavailable : LoginOutcome
}

data class LegalLinks(val privacyUrl: String?, val termsUrl: String?)
```

`AuthProvider` is one data class carrying identity plus both facts as instance values, so the roster
sets them per device today and a backend can set them later (FR-006). The type carries no label,
mark, order, or platform. `LoginOutcome.Unavailable` is a domain outcome, not an error type: the
caller decides the copy (FR-012).

## Use-case contracts

```kotlin
package app.yap.feature.auth.api.usecase

fun interface ObserveAuthSessionStateUseCase { operator fun invoke(): Flow<AuthSessionState> }

fun interface ObserveAuthProvidersUseCase { operator fun invoke(): Flow<List<AuthProvider>> }

fun interface LoginUseCase { suspend operator fun invoke(provider: AuthProvider): LoginOutcome }

fun interface RefreshSessionUseCase { suspend operator fun invoke() }

fun interface GetLegalLinksUseCase { suspend operator fun invoke(): LegalLinks }
```

- `ObserveAuthProvidersUseCase` emits every provider the app knows, in display order, each carrying
  its own `isVisible` and `isEnabled` for this device. Cold; no network or storage work (FR-007).
- `LoginUseCase` takes the chosen provider and is the login screen's only login dependency
  (FR-008). Its implementation applies the 60-second attempt bound to every provider (FR-029) and
  returns `Unavailable` for a provider with no registered handler or one the roster marks not
  selectable (FR-012).
- `RefreshSessionUseCase` takes no parameter and returns nothing — the rationale is in
  [auth-api.md](auth-api.md) under *Client-side ports*.
- `GetLegalLinksUseCase` serves the two configured destinations; either may be null (FR-051).

`Default…UseCase` implementations stay `internal` in `impl`.

## No platform port

Each platform's Google login lives in its own source set inside `impl`, so
`GoogleCredentialProvider`, `GoogleCredential`, and `LoginCancelledException` are `internal` there —
see [auth-api.md](auth-api.md). Nothing outside the feature implements them, and the iOS framework
exports no feature auth type at all. Its `IosGoogleSignInBridge` belongs to `shared-app` and carries
only a nonce in and a nullable ID-token string out.

## Consumer impact

| Consumer | Surface it uses |
| --- | --- |
| `app-root` | `ObserveAuthSessionStateUseCase` for the back stack base, `RefreshSessionUseCase` for the launch refresh, `AuthNavKey.Login` as the logged-out base |
| `ios-app` | `IosGoogleSignInBridge` from `shared-app`; no `feature-auth` declaration |
| `feature-auth/impl` | implements every contract above; `GoogleProviderLogin` serves `AuthProviderType.GOOGLE` |

## Rules this contract keeps

- No Compose, resource, or platform type crosses into `api` — `AuthProvider` carries no label,
  mark, or platform (FR-013).
- Use-case contracts only; implementations are `internal` in `impl`.
- `featureAuthModule()` is public in **`impl`**, not here: a function in `api` cannot bind
  declarations that are `internal` to `impl`, and `app-root` depends on `impl` precisely to load it.

## The one sharp edge

`AuthProvider` instances travel on Navigation 3's result bus, which keys results by type name unless
an explicit key is given. Rather than rely on the type argument being written identically at both
call sites, both name one shared constant:

```kotlin
internal object AuthResultKeys { const val PROVIDER_SELECTION = "auth.provider.selection" }

// sending, on the selection screen
resultEventBus.sendResult(resultKey = AuthResultKeys.PROVIDER_SELECTION, result = provider)

// receiving, on the login screen
ResultEffect<AuthProvider>(resultKey = AuthResultKeys.PROVIDER_SELECTION) { onEvent(ProviderChosen(it)) }
```

See [research.md](../research.md) R20.
