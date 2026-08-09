# Mobile Data Source Boundaries

- Status: Accepted
- Date: 2026-08-09
- Scope: Data sources, representations, mapping, and failures under `apps/mobile/*`

## Context

Transport, persistence, identity, and platform representations must not leak into domain contracts. The repository is the conversion and orchestration boundary; data sources remain specific to one external protocol.

Repository cache behavior is defined in [Repository Contracts and Cache Semantics](repository-contracts-and-cache.md).

## Decision

Data sources use primitives and their own representations:

- remote models end in `Request` or `Response`;
- persisted serialized models end in `Db`;
- identity and platform adapters use focused data-layer tokens or platform models;
- no data source accepts or returns a domain entity.

Keep protocol behavior beside the relevant source: HTTP status handling, JSON, database APIs, secure storage, and platform SDK calls. A remote source may inspect `HttpResponse` or `HttpStatusCode` internally when the distinction changes orchestration, but Ktor types cannot cross `data.remote`.

Create a focused suspend API under `data.remote` using the shared `NetworkClient`. Do not add a forwarding `RemoteDataSource` layer with no behavior. Introduce a small internal data result only when a protocol distinction changes repository orchestration, such as a conflict that requires reload.

## Mapping

- Convert data representations to domain only at repository boundaries.
- Put pure extensions in `data.mapper`, grouped by the domain model they produce.
- Use `toDomain()` for data-to-domain and `toDb()` for domain-to-persistence conversion.
- Do not map inline in DTOs, data sources, repositories, use cases, models, or composables.
- Apply serialization annotations only to transport or persisted representations.

## Errors and cancellation

- Always rethrow coroutine `CancellationException`.
- Use typed domain results only for expected alternatives callers handle differently.
- Generic fetch failures may fail the suspend call and are treated as retryable without exposing data-layer exception types.
- Never expose HTTP codes, Ktor exceptions, database exceptions, provider errors, or internal data results through repository contracts.
- Failed work must not erase the last successful cache unless invalidation is the domain result.

## Shared authenticated networking

The application creates one Ktor client through `NetworkContainer` and owns its `AutoCloseable` lifetime. Feature containers receive `NetworkClient`; they neither build nor close it.

- Mark authenticated requests with `HttpRequestBuilder.authenticated()` instead of adding bearer headers manually.
- `installAccessTokenModifier` attaches the current token and retries once after a `401 Unauthorized` with a refreshed token.
- Repositories and remote sources do not implement token refresh or 401 retry.
- The installed `AccessTokenProvider` implementation serializes refresh, reuses a token already refreshed by another caller, persists a new session before returning it, and publishes signed-out state when recovery fails.
- A remote source translates only the final response after the shared retry mechanism has completed.

## Verification

Test protocol behavior and serialization at the data-source boundary. Test non-trivial mapping, defaults, invalid input, and time or value-object parsing independently. Follow the [test-structure rules](../testing/001-test-structure.md).

## Consequences

Domain and presentation remain independent from protocols, while authentication retry is implemented once. Data representations and mapping remain explicit even when they look similar to domain models.

## Compliance

New and changed data sources follow this ADR. Intentional exceptions must be justified in the pull request or superseded by a later ADR.
