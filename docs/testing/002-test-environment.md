# Test Environment

## Context

Repeated construction of the same object graph makes unit tests noisy. Shared mutable setup can leak state and hide the scenario.

## Decision

When tests repeat the same object graph, add a `private class Environment` at the bottom of the test class:

```kotlin
@Test
fun `GIVEN remote user WHEN refreshing THEN returns user`() = runTest {
    val user = StubUser.stubUser()
    val env = Environment(remoteUser = user)

    val result = env.repository.get(
        id = UserId(StubUser.ID),
        forceUpdate = true,
    )

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

- Create a fresh `val env = Environment(...)` inside each test. Do not share mutable fixtures through fields or setup hooks.
- Let the environment construct the system under test and its collaborators. Declare collaborators before the system under test.
- Pass required initial state as named constructor arguments. Use defaults only for a neutral baseline.
- Configure behavior that can change after construction directly in the test.
- Keep actions and scenario orchestration in the test.
- Do not add assertions, verification helpers, `if`/`else`, `when`, or loops to the environment.
- Skip the environment when setup is simple and not repeated.
