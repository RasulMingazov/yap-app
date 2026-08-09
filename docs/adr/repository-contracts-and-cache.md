# Repository Contracts and Cache Semantics

- Status: Accepted
- Date: 2026-08-08
- Scope: Domain repositories in mobile feature modules under `apps/mobile/*`

## Context

Repositories are the source of domain data and the boundary between domain and external representations. Their names, granularity, cache policy, concurrency, and mutation semantics must be predictable independently of transport or storage technology.

Data-source representations are covered by [Mobile Data Source Boundaries](mobile-data-sources.md), and physical database ownership by [Mobile Persistence Composition](mobile-persistence.md).

## Decision

Use one repository per primary domain model or cohesive aggregate.

- Do not model endpoints, screens, DTOs, tables, value objects, commands, or result types as repositories.
- Keep operations that mutate one aggregate in the same repository.
- Split repositories when state, lifetime, cache policy, or reasons to change are independent.
- Keep operations that must remain atomic together, such as rotating a refresh token with its session.

## Contract naming

| Shape | Observe | Fetch |
| --- | --- | --- |
| Collection | `observeAll(): Flow<List<T>>` | `getAll(forceUpdate: Boolean): List<T>` |
| Single current value | `observe(): Flow<T?>` | `get(forceUpdate: Boolean): T?` |
| Value addressed by ID | `observe(id: Id): Flow<T?>` | `get(id: Id, forceUpdate: Boolean): T?` |

- Use collection names only for collections and singular names for one current aggregate.
- Add an identifier only when the repository owns independently addressable instances.
- Fetch methods return the fetched domain value, never `Unit`.
- Do not repeat the model name in methods.
- Name mutations after domain intent: `start()`, `send(command)`, `complete(id)`, `delete(id)`.
- Return a domain entity or typed domain result when the caller needs a mutation outcome.
- Expose `StateFlow` only when synchronous current-value access is part of the domain contract; otherwise expose `Flow`.
- Do not give `forceUpdate` a default value. Every caller states its cache choice.

```kotlin
internal interface ItemRepository {

    fun observeAll(): Flow<List<Item>>

    suspend fun getAll(forceUpdate: Boolean): List<Item>

    suspend fun create(command: CreateItemCommand): CreateItemResult
}
```

## Cache and observation

Persistent storage is the cached and observable source. Do not duplicate its snapshot in a repository-owned `MutableStateFlow` or collection.

With `forceUpdate = false`, a fetch returns the initialized local snapshot, including an empty one, and performs no network request.

With `forceUpdate = true`, a fetch:

1. requests the latest source value even when cache exists;
2. maps it and replaces cache only after success;
3. publishes and returns the same domain value;
4. preserves the previous cache on failure.

Observation methods do not initiate network or storage work. They emit the local snapshot and subsequent successful changes as read-only flows, without UI loading flags or localized errors.

Use a focused DAO directly when it already provides the required boundary. Add a `LocalDataSource` only when it coordinates several storage APIs or adds real platform behavior.

## Concurrency and mutations

- Serialize operations that replace local state so concurrent work cannot publish stale values out of order.
- Coordinate identical concurrent refreshes as single-flight or equivalent.
- Publish complete immutable snapshots.
- Scope each repository to its data. Destroy user-owned repositories with the authenticated branch.
- Allow application scope only for genuinely application-scoped state such as authentication session state.
- Publish mutations after the authoritative source confirms success unless an explicitly specified optimistic flow includes rollback.
- Update or invalidate cache before returning from a successful mutation.
- Map and publish a returned authoritative representation instead of fetching it again.
- Refresh only when a response lacks enough authoritative data or a conflict changes the aggregate.
- Preserve the last successful snapshot on failure unless invalidation is the explicit domain outcome.

Presentation reaches repositories through use cases that represent actor intent or a useful test boundary. Do not generate one use case per repository method automatically. Observation and fetch remain separate operations so a model can collect state and explicitly own loading or retry work.

## Verification

Repository tests cover initial load, cache reuse, forced refresh, failed-refresh preservation, mutation consistency, actionable results, concurrency coordination, and cancellation. Use data-source and storage stubs; follow the [unit-testing rules](../testing/README.md).

## Consequences

One-shot fetches and observed state cannot silently diverge, and repository APIs are predictable from aggregate shape. Implementations must explicitly coordinate cache and concurrency.

## Compliance

New repositories follow this ADR. Existing repositories migrate when their behavior changes. Intentional exceptions must be justified in the pull request or superseded by a later ADR.
