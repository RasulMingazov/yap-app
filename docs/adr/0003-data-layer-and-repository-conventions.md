# ADR-0003: Data Layer and Repository Conventions

- Status: Accepted
- Date: 2026-08-08
- Scope: Domain repository contracts and data implementations in `yap-app` mobile feature modules (`apps/mobile/*`)

## Agent essentials

For ordinary data work, read this section and only the detailed sections governing the changed behavior. Read the full ADR when redefining repository ownership, cache semantics, persistence composition, or shared networking boundaries.

- Use one repository per primary domain model or cohesive aggregate; do not model endpoints, screens, DTOs, or database tables as repositories.
- Persistent storage is the cached and observable source. A repository coordinates refresh and mutation consistency without duplicating the snapshot in mutable in-memory state.
- `forceUpdate = false` returns the initialized local snapshot without network work; `forceUpdate = true` fetches, maps, and replaces cache only after success.
- Data sources use primitives and their own transport, persistence, identity, or platform representations. Convert to domain only at repository boundaries.
- Repositories own caching, synchronization, retry, fallback, and actionable result mapping. Infrastructure details and exceptions do not cross domain contracts.
- Coordinate concurrent refreshes, rethrow coroutine cancellation, preserve successful cache on failure, and destroy user-owned repositories with the authenticated branch.

## Context

Repositories are the feature's source of domain data and the boundary between domain and external representations. Without consistent ownership and method semantics, repositories tend to become endpoint collections, unrelated models become coupled, callers cannot tell whether a method reads cache or network, and observable state diverges from one-shot results.

`feature-auth` has no repository implementations yet — it is currently a build-only scaffold (`apps/mobile/feature-auth/build.gradle.kts`). This ADR defines the target shape those implementations converge on, and the shared infrastructure they build on that already exists: an application-scoped Ktor `HttpClient` in `core-network`, an `AccessTokenProvider` port in `core-common`, and (once a feature needs local persistence) Room, described below.

This ADR defines repository granularity, naming, cache behavior, data-source boundaries, mutation consistency, and verification requirements.

## Decision

Use one repository per primary domain model or cohesive aggregate.

- Name the contract after the owned domain model, for example `AuthSessionRepository` for the current authenticated session.
- Do not combine distinct models merely because one backend endpoint or screen returns them together.
- Do not create repositories for value objects, commands, result types, DTOs, database tables, or individual endpoints.
- Keep operations that mutate the same aggregate in its repository.
- Split a repository when its state, lifetime, cache policy, or reasons to change are independent.

For example, the current authenticated session and a locally cached user profile use separate repositories when they have independent refresh policies and lifetimes. Keep operations that must stay atomic — such as rotating a refresh token together with the session it belongs to — in one repository rather than splitting them merely to shrink a constructor.

## Contract naming

Choose method names from the shape of the owned domain state:

| Shape | Observe | Fetch |
| --- | --- | --- |
| Collection | `observeAll(): Flow<List<T>>` | `getAll(forceUpdate: Boolean): List<T>` |
| Single current value | `observe(): Flow<T?>` | `get(forceUpdate: Boolean): T?` |
| Value addressed by ID | `observe(id: Id): Flow<T?>` | `get(id: Id, forceUpdate: Boolean): T?` |

Rules:

- Use `observeAll()` and `getAll()` only for collections.
- Use singular `observe()` and `get()` for one current aggregate — `AuthSessionRepository` is a "single current value" repository.
- Add an identifier only when the repository owns multiple independently addressable instances.
- A fetch method returns the fetched domain value directly, never `Unit`.
- Do not repeat the repository model in method names: prefer `AuthSessionRepository.observe()` over `observeAuthSession()`.
- Name mutations after domain intent: `start()`, `send(command)`, `complete(id)`, `delete(id)`.
- Return a domain entity or typed domain result from a mutation when the caller needs the outcome.
- Use `StateFlow` in a contract only when synchronous access to a current value is itself part of the domain contract, such as application authentication state. Otherwise expose `Flow`.

A collection repository normally has this shape:

```kotlin
internal interface ItemRepository {

    fun observeAll(): Flow<List<Item>>

    suspend fun getAll(forceUpdate: Boolean): List<Item>

    suspend fun create(command: CreateItemCommand): CreateItemResult
}
```

Do not add default values to `forceUpdate`; every call site must state whether it accepts cache or requests refresh.

## Fetch and observation semantics

The repository owns cache policy and consistency for its domain model. Persistent storage is the single physical source of cached and observable data; the repository itself does not duplicate that snapshot in a `MutableStateFlow` or collection field.

`getAll(forceUpdate = false)` and singular `get(...)` behave as follows:

1. Return the current local snapshot, including a valid empty snapshot.
2. Do not initiate a remote request.

`getAll(forceUpdate = true)` and singular `get(...)` behave as follows:

1. Request the latest value from the source even when cache exists.
2. Replace the cached snapshot only after a successful fetch and mapping.
3. Publish and return the same new domain value.
4. Preserve the previous cached snapshot when refresh fails.

Observation methods:

- never initiate network or storage work by themselves;
- emit the repository's local-storage snapshot and subsequent successful changes;
- expose read-only flows, never `MutableStateFlow`;
- do not encode UI loading flags or localized errors.

Each repository uses dedicated local storage for its primary domain model. A Room repository may depend directly on a focused DAO. Add a `LocalDataSource` wrapper only when it provides real behavior beyond forwarding DAO methods, such as coordinating several storage APIs or mapping a platform-specific contract. Do not combine independently owned models into screen-shaped storage.

Loading, retry visibility, and screen-level error state belong to presentation. Whether a repository has loaded its first value is an internal cache concern unless the domain explicitly models that distinction.

## Concurrency and lifetime

- Serialize repository operations that replace local state so concurrent fetches and mutations cannot publish stale data out of order.
- Concurrent requests for the same refresh should be single-flight or otherwise coordinated instead of performing duplicate calls.
- Publish complete immutable snapshots rather than mutating lists or entities in place.
- Scope the repository to the lifetime of its data. User-owned repositories must be destroyed with the authenticated branch and must not retain data across logout/login.
- Application-scoped repositories are allowed only for genuinely application-scoped state such as authentication session state.

## Mutation semantics

- By default, publish a mutation only after the authoritative source confirms success.
- After success, update or invalidate the repository cache before returning so subsequent observers and fetches cannot see older state.
- When the server returns the updated representation, map and publish it directly instead of issuing an unnecessary second fetch.
- Refresh after a mutation only when the response does not contain enough authoritative data or when a conflict changes the expected aggregate.
- Use optimistic updates only when the product requires them and rollback behavior is explicitly defined and tested.
- Expected alternatives that change caller behavior use typed domain results. HTTP codes and provider exceptions do not cross the repository contract.

## Data sources and representations

Data sources are specific to their external representation:

- remote data sources use primitives and `*Request` / `*Response` models;
- local data sources use primitives and serialized `*Db` models;
- identity and platform adapters use focused data-layer tokens or platform models;
- no data source accepts or returns a domain entity.

The application-scoped Ktor `HttpClient`, JSON configuration, timeouts, and platform engines are already created once in `core-network`: `NetworkContainer`/`DefaultNetworkContainer`/`createNetworkContainer(baseUrl)` build the client, and `platformHttpClientEngine()` supplies the platform engine per target (`PlatformHttpClient.android.kt`, `PlatformHttpClient.ios.kt`). The application host owns the container's lifetime — `NetworkClient` is `AutoCloseable` — and closes it when the host lifecycle has an explicit destruction event; feature containers receive it through manual DI and must not build or close their own clients.

Persistence is intended to eventually share one physical database across the authenticated app the same way networking already shares one `HttpClient`, but this does not exist yet: no `apps/mobile/core-database` module is registered in `settings.gradle.kts`, Room is not in `gradle/libs.versions.toml`, and there is no authenticated-shell module comparable to `feature-main` — `app-root` currently composes `feature-auth` directly. When a feature first needs local persistence, introduce `apps/mobile/core-database` mirroring the generic-bootstrap role `services/server/core-database` already plays on the backend (`DatabaseFactory` owns connection lifecycle only, no entities or DAOs — see [ADR-0005: Feature-First Backend Architecture](0005-feature-first-backend-architecture.md)): it should provide platform driver/dispatcher bootstrapping such as `createRoomDatabase<T>(databasePath)` and own no entities, DAOs, or `@Database` class itself. Room requires a `@Database` class to have compile-time visibility of every entity and DAO it registers, and core modules must never depend on feature modules, so the concrete database can only be assembled where that visibility legitimately exists — the module that already depends on every authenticated feature needing local storage. Introduce that composition module only once a second authenticated feature actually needs shared local persistence; until then, `feature-auth` may open its own focused Room database directly if it needs local storage first. The application host should only ever see a raw `databasePath: String`, matching the existing rule that hosts pass platform inputs, not storage, into feature factories.

Declare KMP HTTP calls as focused suspend functions on a small feature-owned remote data source in `data.remote`, built on the shared `NetworkClient.httpClient` from `core-network`. Mark authenticated calls with the existing `HttpRequestBuilder.authenticated()` extension (`core-network`'s `Authenticated.kt`) instead of reimplementing bearer-token attachment. Keep status handling, cancellation, and transport-to-data result conversion beside those calls in `data.remote`; do not add a forwarding `RemoteDataSource` abstraction with no behavior of its own. A remote data source may inspect `HttpResponse`/`HttpStatusCode` internally when a status code changes repository orchestration, but Ktor types must not cross the `data.remote` boundary.

Keep protocol concerns inside the relevant data source: HTTP status handling, JSON, database APIs, secure storage, and platform SDK calls. Introduce a small internal data result only when a transport distinction changes repository orchestration, such as a conflict triggering reload.

## Mapping

- Convert between data representations and domain only at repository boundaries.
- Put pure mapping extensions in `data.mapper`, grouped by the resulting domain model.
- Use `toDomain()` for remote/local-to-domain conversion and `toDb()` for domain-to-persistence conversion when applicable.
- Do not map inline in DTOs, data sources, repositories, use cases, models, or composables.
- Apply serialization annotations only to actual transport or persisted representations.

## Errors and cancellation

- Always rethrow coroutine `CancellationException`.
- Convert expected alternatives into typed domain results only when callers handle them differently.
- Generic fetch failures may fail the suspend call; callers treat them as a generic retryable failure and must not branch on data-layer exception types.
- Never expose HTTP status codes, Ktor exceptions, database exceptions, provider errors, or data-layer result types through a repository contract.
- A failed fetch or mutation must not erase the last successful cached snapshot unless invalidation is the explicit domain outcome.

For authenticated requests, the 401-retry-and-refresh mechanism already lives at the network layer, not in repositories:

- mark the request with `core-network`'s `HttpRequestBuilder.authenticated()` extension instead of attaching a bearer token manually;
- `core-network`'s `installAccessTokenModifier` — an `HttpSend` plugin installed on the feature's `NetworkClient` — transparently attaches the current access token, retries once with a freshly obtained token when the first attempt returns `401 Unauthorized`, and gives up if the retry also fails; repositories and remote data sources never see or handle 401 themselves;
- feature containers supply `installAccessTokenModifier` with a `getAccessToken: suspend (rejectedAccessToken: String?) -> String?` implementation backed by the shared `AccessTokenProvider` port (`core-common`), which must serialize refresh, reuse a token already refreshed by another caller, persist a newly refreshed session before returning it, and publish signed-out state when refresh cannot recover;
- a remote data source only needs to translate the final response (success, or the still-401 response after the shared retry) into its own result type — it does not implement retry or refresh logic itself.

## Use cases and consumers

Presentation accesses repository behavior through use cases representing actor intent or an intentional test boundary. Do not generate a use case automatically for every repository method when it adds no semantic or testing value.

Observe and fetch remain separate operations:

- observe use cases provide continuous repository state;
- get use cases explicitly trigger initial load or refresh and return the fetched value;
- a model may collect observation and separately call fetch while owning loading/error UI facts.

## Testing

Detailed test naming, verification, and test-double conventions are defined in [ADR-0001: Unit Test Conventions](0001-unit-test-conventions.md).

Repository implementation tests use fake data-source, storage, identity, or platform contracts and verify:

- first fetch loads, maps, publishes, and returns the same value;
- `forceUpdate = false` reuses an initialized cache;
- `forceUpdate = true` requests and publishes fresh data;
- refresh failure preserves the last successful snapshot;
- successful mutations update observed state before returning;
- conflicts and other actionable alternatives map to domain results;
- concurrent refreshes do not race or duplicate work when coordination is required;
- coroutine cancellation is preserved;
- transport and persistence representations never escape the repository.

Data-source tests cover protocol behavior and serialization separately. Mapper tests cover non-trivial conversion, defaults, invalid input, and time/value-object parsing.

## Consequences

Benefits:

- Repository contracts are predictable from the shape of their domain model.
- Unrelated models, caches, and lifetimes remain independent.
- One-shot fetch results and observed state cannot silently diverge.
- Transport and persistence changes remain isolated from domain and presentation.
- The 401-retry-and-refresh concern is solved once at the network layer instead of being re-implemented per repository.

Costs:

- A screen combining multiple domain models may depend on multiple use cases and repositories.
- Repository implementations must coordinate cache state and concurrent operations explicitly.

These costs are accepted. A screen-shaped catch-all repository is not used to reduce constructor parameters.

## Compliance and migration

New repositories follow this ADR immediately. `yap-app` has no repository implementations yet, so there is no legacy naming to migrate — this ADR governs repositories from their first implementation.

Any intentional exception must be justified in the pull request description, per the constitution's Governance section, or superseded by a later ADR.
