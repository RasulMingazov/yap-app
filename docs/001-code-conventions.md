# Code Conventions

Project-wide additions to the official Kotlin coding conventions. Everything the Kotlin style guide already covers is out of scope here.

## `when` on subtypes

When a `when` dispatches on subtypes, use `is` for every branch — including singletons. Do not mix a value branch with type branches in the same dispatch.

```kotlin
when (result) {
    is LoginResult.Cancelled -> onCancelled()   // not `LoginResult.Cancelled ->`
    is LoginResult.Failure -> onFailure(result)
}
```

Equality branches stay appropriate when the dispatch is genuinely about enum values, constants, or strings.

## Alphabetical ordering

Prefer alphabetical order within the same semantic group: constructor parameters, injected dependencies and stored properties, function parameters with no meaningful domain order, named arguments, fields in presentation state and test builders, and sibling bindings with equivalent roles.

```kotlin
internal class LoginViewModel(
    private val authenticateUseCase: AuthenticateUseCase,
    private val navigator: Navigator,
    private val observeSessionUseCase: ObserveSessionUseCase,
)
```

Alphabetize required and defaulted parameters within their own groups. Do not alphabetize when order is part of a wire format, serialization contract, database schema, public compatibility requirement, lifecycle sequence, UI order, or domain narrative.

## Early returns

Prefer guard clauses when they remove nesting and make preconditions visible before the happy path. Do not split a short, already-clear expression into several returns, do not use labeled or non-local returns that obscure control flow, and preserve exhaustive `when` expressions when every outcome must be handled.
