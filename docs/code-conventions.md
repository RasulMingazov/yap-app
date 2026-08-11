# Code Conventions

These rules apply to project-owned Kotlin in mobile, server, shared contracts, convention
plugins, and tests. They complement the official Kotlin coding conventions.

## `when` branches

When a `when` dispatches on subtypes, use an explicit `is` check for every subtype branch. Do not
mix a singleton value branch with type branches in the same subtype dispatch.

Avoid:

```kotlin
when (result) {
    LoginResult.Cancelled -> onCancelled()
    is LoginResult.Failure -> onFailure(result)
}
```

Prefer:

```kotlin
when (result) {
    is LoginResult.Cancelled -> onCancelled()
    is LoginResult.Failure -> onFailure(result)
}
```

This is a project consistency rule for sealed hierarchies. Equality-based `when` branches remain
appropriate when the dispatch is genuinely about enum values, constants, strings, or other values
rather than subtypes.

## Early returns

Prefer guard clauses and early returns when they remove nesting and make preconditions or failure
paths visible before the happy path.

Avoid:

```kotlin
fun startLogin(provider: Provider?) {
    if (provider != null) {
        if (provider.isEnabled) {
            launch(provider)
        }
    }
}
```

Prefer:

```kotlin
fun startLogin(provider: Provider?) {
    val configuredProvider = provider ?: return
    if (!configuredProvider.isEnabled) return

    launch(configuredProvider)
}
```

Do not split a short, already-clear expression into several returns, and do not use labeled or
non-local returns when they make control flow harder to follow. Preserve exhaustive `when`
expressions when every outcome must be handled.

Detekt's `ReturnCount` runs with `excludeGuardClauses: true`, so leading guard clauses never count
against the limit. A `ReturnCount` violation therefore means returns are scattered through the body
of a function, not that preconditions are checked up front — restructure the body rather than
suppress the rule.

## Alphabetical ordering

Prefer alphabetical order for declarations and arguments within the same semantic group:

- constructor parameters;
- injected dependencies and stored properties;
- function parameters when no domain order is more meaningful;
- named arguments at call sites;
- fields in presentation state, test builders, and configuration objects;
- sibling factory bindings or registrations with equivalent roles.

```kotlin
internal class LoginViewModel(
    private val authenticateUseCase: AuthenticateUseCase,
    private val observeSessionUseCase: ObserveSessionUseCase,
    private val providerRegistry: ProviderRegistry,
    coroutineDispatchers: CoroutineDispatchers,
)
```

Stored `private val` parameters form the first group and plain parameters the second; alphabetize
within each group.

Alphabetize required and defaulted parameters within their own groups when Kotlin requires those
groups to remain separate. Do not alphabetize when order is part of a wire format, serialization
contract, database schema, public compatibility requirement, lifecycle sequence, UI order, or
domain narrative. Semantic correctness and readability take precedence over mechanical sorting.
