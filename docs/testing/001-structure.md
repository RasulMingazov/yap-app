# Test Structure

One test describes one scenario and fails for one reason. Framework selection and harness setup follow the official `testing-setup` skill.

## What to test

Write a test only when the code owns observable behavior: a branch, mapping, state transition, orchestration rule, fallback, or error translation.

- Do not create a test class only to increase coverage.
- Do not test pass-through use cases, constructors, data-class accessors, or trivial delegation.
- Do not test behavior owned by an SDK, a framework, or another library.
- Test a wrapper only when it adds project-owned behavior such as serialization, key selection, validation, fallback, or error mapping.
- Test behavior once at the boundary that owns it; do not repeat the same scenario at every layer.

## Test shape

```text
GIVEN precondition WHEN action THEN outcome
```

- Name test functions with Kotlin backticks and use product language; name the class after the implementation under test.
- Separate arrange, act, and assert visually and keep scenario-specific values visible.
- Allow multiple assertions only when they verify one outcome. Prefer comparing one object or collection over checking each field.
- Write expected values independently from production logic; do not compute them with the algorithm under test.
- Use `assertFailsWith` for an expected failure.
- Keep branching, loops, `try`/`catch`, and production algorithms out of tests.
- Use multiple invocations only when testing repetition, concurrency, or idempotency.
- Prefer observable state or results. Verify calls only when the interaction is part of the contract.
- Drive view-model tests with `runViewModelTest` from `core-test`; plain `runTest` is enough elsewhere.

## Environment

When tests repeat the same object graph, add a `private class Environment` at the bottom of the test class:

```kotlin
@Test
fun `GIVEN remote user WHEN refreshing THEN returns user`() = runTest {
    val user = StubUser.stubUser()
    val env = Environment(remoteUser = user)

    val result = env.repository.getById(id = UserId(StubUser.ID), forceUpdate = true)

    assertEquals(expected = user, actual = result)
}

private class Environment(
    remoteUser: User,
) {

    val remoteDataSource = StubUserRemoteDataSource(user = remoteUser)
    val localDataSource = StubUserLocalDataSource()
    val repository: UserRepository = DefaultUserRepository(
        remoteDataSource = remoteDataSource,
        localDataSource = localDataSource,
    )
}
```

- Create a fresh `Environment` inside each test; do not share mutable fixtures through fields or setup hooks.
- Let it construct the system under test and its collaborators, declaring collaborators first.
- Pass required initial state as named constructor arguments; use defaults only for a neutral baseline.
- Keep actions, assertions, and control flow in the test, never in the environment.
- Skip it when setup is simple and not repeated.
