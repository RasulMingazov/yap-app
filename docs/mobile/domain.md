# Domain

The domain layer expresses product language and policy without depending on frameworks or infrastructure.

- Limit domain packages to `entity`, `repository`, and `usecase`; create only those that own behavior or a required boundary.
- Keep entities and value objects immutable and enforce only genuine business invariants.
- Keep Compose, Decompose, Ktor, serialization, database, resources, and platform SDKs out of domain code.
- Describe repository ports with domain types and read-only flows. Expose no DTOs, credentials, status codes, storage models, or mutable flows.
- Use typed results only when expected alternatives change caller behavior.
- Expose every repository operation used by presentation through a dedicated use-case contract. A use case may be a thin delegation because it is the presentation test boundary.

## Use cases

Keep each use-case contract and its default implementation in one file named after the contract:

```kotlin
internal interface GetUserUseCase {

    suspend operator fun invoke(id: UserId): User
}

internal class DefaultGetUserUseCase(
    private val repository: UserRepository,
) : GetUserUseCase {

    override suspend fun invoke(id: UserId): User = repository.getById(
        id = id,
        forceUpdate = false,
    )
}
```

- Use the `UseCase` suffix and `Default...UseCase` implementation name.
- Declare the default implementation immediately below the interface; do not create a separate implementation file.
- Keep both `internal` unless the contract crosses a real module boundary.
- Presentation depends on use cases only and never receives a repository directly.
- Add another production implementation only when a real substitution boundary requires it.

Repository contracts follow [Repositories](data/repositories.md). Infrastructure translation follows [Data Sources](data/data-sources.md).
