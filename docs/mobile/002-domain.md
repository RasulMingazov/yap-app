# Domain

Domain expresses product language and policy without frameworks or infrastructure. Keep Compose, navigation, Koin, Ktor, serialization, database, resources, and platform SDKs out of it.

## Use cases

The contract lives in `api`; its implementation lives in `impl` and is `internal`.

```kotlin
// api
interface GetUserUseCase {

    suspend operator fun invoke(id: UserId): User
}

// impl
internal class DefaultGetUserUseCase(
    private val repository: UserRepository,
) : GetUserUseCase {

    override suspend fun invoke(id: UserId): User = repository.getById(
        id = id,
        forceUpdate = false,
    )
}
```

- Use the `UseCase` suffix and the `Default...UseCase` implementation name.
- Expose every repository operation used by presentation through a use-case contract. A thin delegation is fine — the use case is the presentation test boundary.
- Presentation depends on use cases only and never receives a repository.
- Add a second production implementation only for a real substitution boundary.

## Entities and ports

- Keep entities immutable and enforce only genuine business invariants.
- An entity appearing in a use-case signature belongs to `api`; the rest stay `internal` in `impl`.
- Describe repository ports with domain types and read-only flows. Expose no DTOs, credentials, status codes, storage models, or mutable flows.
- Ports stay in `impl`: only the feature's own use cases and repositories see them.
- Use typed results only when expected alternatives change caller behavior.

Repository contracts follow [Repositories](data/001-repositories.md). Infrastructure translation follows [Data Sources](data/002-data-sources.md).
