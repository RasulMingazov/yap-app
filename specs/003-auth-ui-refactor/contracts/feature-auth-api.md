# Contract: `feature-auth/api`

The public surface other modules may see. Everything else in the feature stays `internal` in `impl`.

## Changed

```kotlin
package app.yap.feature.auth.api.entity

sealed interface AuthProvider {

    val isEnabled: Boolean
    val isVisible: Boolean

    data class Apple(override val isEnabled: Boolean, override val isVisible: Boolean) : AuthProvider

    data class Google(override val isEnabled: Boolean, override val isVisible: Boolean) : AuthProvider

    data class TId(override val isEnabled: Boolean, override val isVisible: Boolean) : AuthProvider
}

sealed interface LoginOutcome {
    data object Success : LoginOutcome
    data object Cancelled : LoginOutcome
    data object Failed : LoginOutcome
    data object Unavailable : LoginOutcome   // new
}
```

```kotlin
package app.yap.feature.auth.api.usecase

fun interface LoginUseCase {

    suspend operator fun invoke(provider: AuthProvider): LoginOutcome   // was invoke()
}
```

```kotlin
package app.yap.feature.auth.api

@Serializable
sealed interface AuthNavKey : NavKey {

    @Serializable data object Login : AuthNavKey

    @Serializable data object SelectAuthProvider : AuthNavKey   // new
}
```

## Added

```kotlin
package app.yap.feature.auth.api.usecase

fun interface ObserveAuthProvidersUseCase {

    operator fun invoke(): Flow<List<AuthProvider>>
}
```

Emits every provider the app knows, in display order, each carrying its own `isVisible` and
`isEnabled` for this device. Cold; performs no network or storage work. Re-emits when the roster
changes.

## Unchanged

`ObserveAuthStateUseCase`, `RenewSessionUseCase`, `AuthState`, `UserId`, `GoogleCredentialProvider`,
`GoogleCredential`, `LoginCancelledException`.

## Consumer impact

| Consumer | Change |
| --- | --- |
| `app-root` | binds `Navigator`; adds the result decorator and the bottom-sheet scene strategy to `NavDisplay` |
| `feature-auth/impl` | implements the two new contracts; `GoogleLoginUseCase` becomes a `ProviderLogin` |

## Rules this contract keeps

- No Compose, resource, or platform type crosses into `api` — `AuthProvider` carries no label, mark,
  or platform.
- Use-case contracts only; `Default...UseCase` implementations stay `internal` in `impl`.
- `LoginOutcome.Unavailable` is a domain outcome, not an error type: the caller decides the copy.

## The one sharp edge

`AuthProvider` instances travel on Navigation 3's result bus, which keys results by
`T::class.toString()`. Sending must name the type explicitly:

```kotlin
resultEventBus.sendResult<AuthProvider>(provider)   // not sendResult(provider)
```

An inferred type argument would key the result by the concrete subclass, and the
`ResultEffect<AuthProvider>` on the login screen would never fire.
