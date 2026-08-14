# Test Stubs

## Context

Tests need reusable values and configurable collaborators without hiding the scenario. Each stub also needs a clear owner.

## Decision

### Value stubs

Build repeated domain entities and data models through an object named after the value:

```kotlin
internal object StubUser {

    const val ID = "user-id"
    const val NAME = "User"
    const val EMAIL_VALUE = "user@example.com"

    fun stubUser(
        id: String = ID,
        name: String = NAME,
        email: Email = stubEmail(),
    ): User = User(
        id = id,
        name = name,
        email = email,
    )

    fun stubEmail(
        value: String = EMAIL_VALUE,
        isVerified: Boolean = true,
    ): Email = Email(
        value = value,
        isVerified = isVerified,
    )
}
```

- Name the object `StubX` and its base builder `stubX`.
- Keep every field a named, overridable parameter.
- Store reused primitive values as `const val` in the same object; use `val` for other types.
- Build nested values in the same object and use them as defaults in the root builder.
- Do not make nested builders private; tests should be able to call them and override their fields.
- Add named scenario builders only for recurring states, such as `stubVerifiedUser()`.

### Behavioral stubs

Use `stubcall` for behavioral collaborators. Reusable presentation stubs implement use-case contracts:

```kotlin
internal class StubGetUserUseCase(
    user: User = StubUser.stubUser(),
) : GetUserUseCase {

    val invokeCall = StubCall1.returns<UserId, User>(user)

    override suspend fun invoke(id: UserId): User = invokeCall.invoke(id)
}
```

- Name the implementation `StubX` and keep it in its own file, in the package of the production
  code that owns the contract — a use-case stub under `domain`, a repository or data-source stub
  under `data` — not in the package of whichever layer happens to consume it.
- Expose a `StubCallN` property named after the delegated method, such as `invokeCall` or `loadCall`.
- Accept the default result in the constructor and build it through the corresponding value stub.
- Match `N` to the method arity; use `unit()` for `Unit` methods and `returns(...)` for a default result.
- Delegate the method directly to `StubCallN`; do not duplicate return values, errors, counters, or captured arguments.
- Configure results and failures and verify calls through that property in tests.
- Presentation tests stub use cases, never repositories.
- Keep repository and data-source test doubles local to tests of the layer that owns those contracts; do not export them as presentation fixtures.
- Expose stubbed observation as `MutableStateFlow` behind a read-only `StateFlow` or `Flow`, matching the contract being stubbed.
