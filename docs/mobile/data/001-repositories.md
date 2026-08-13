# Repositories

Both the port and its implementation live in `impl`: the interface in `domain/repository`, the implementation, mapping, cache, and coordination in `data/repository`. A repository never crosses into `api`.

## Boundary

Use one repository per primary domain model or cohesive aggregate.

- Do not model endpoints, screens, DTOs, tables, value objects, commands, or result types as repositories.
- Keep operations that mutate one aggregate together.
- Split repositories when state, lifetime, cache policy, or reasons to change are independent.
- Keep operations that must remain atomic together, such as rotating a refresh token with its session.

## Naming

| Shape | Observe | Fetch |
| --- | --- | --- |
| Collection | `observeAll(): Flow<List<T>>` | `getAll(forceUpdate: Boolean): List<T>` |
| Current value | `observe(): Flow<T?>` | `get(forceUpdate: Boolean): T?` |
| Value by ID | `observeById(id: Id): Flow<T?>` | `getById(id: Id, forceUpdate: Boolean): T?` |

- Use the Kotlin-style `ById` suffix for independently addressable values; do not hide the lookup shape in a bare `observe(id)` or `get(id)`.
- Fetch methods return the fetched value, never `Unit`.
- Do not repeat the model name in methods.
- Name mutations after domain intent: `start()`, `send(command)`, `complete(id)`, `delete(id)`.
- Expose `StateFlow` only when synchronous access is part of the domain contract; otherwise expose `Flow`.
- Do not give `forceUpdate` a default value.

## Cache and observation

Persistent storage is the cached and observable source. Do not duplicate its snapshot in a repository-owned mutable collection or flow.

- `forceUpdate = false` returns the initialized local snapshot, including an empty one, without a network request.
- `forceUpdate = true` requests the latest value and replaces cache only after success.
- Return and publish the same mapped value.
- Preserve the last successful cache on failure unless invalidation is an explicit domain outcome.
- Observation emits local snapshots and never starts network or storage work.

Add a `LocalDataSource` only when it coordinates several storage APIs or real platform behavior.

## Concurrency and mutations

- Serialize operations that replace local state and coordinate identical refreshes as single-flight or equivalent.
- Publish complete immutable snapshots.
- Scope repositories to the lifetime of their data and recreate user-owned repositories when the active identity changes.
- Publish mutations after authoritative success unless an optimistic flow specifies rollback.
- Update or invalidate cache before returning from a successful mutation.
- Publish an authoritative response instead of fetching it again when it contains enough data.

Every repository operation consumed by presentation is wrapped in a use-case contract. View models receive those use cases instead of repositories, so presentation tests stub only the use-case boundary. Observation and fetch remain separate so the view model owns loading and retry.

## Verification

Cover initial load, cache reuse, forced refresh, failed-refresh preservation, mutation consistency, concurrency, and cancellation. Use data-source and storage stubs; follow [Test Structure](../../testing/001-structure.md).

Representations and mapping follow [Data Sources](002-data-sources.md).
