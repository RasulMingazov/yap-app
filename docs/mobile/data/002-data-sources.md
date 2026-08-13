# Data Sources

Data sources remain specific to one external protocol so infrastructure representations never leak into domain contracts.

## Representations

- Shared wire models end in `Dto`; follow [Shared Contracts](../../shared/README.md).
- Persisted serialized models end in `Db` for root entities or `Local`.
- Identity and platform adapters use focused data-layer tokens or platform models.
- No data source accepts or returns a domain entity.
- Keep HTTP, JSON, database, secure-storage, and platform SDK behavior beside its source.
- Do not add a forwarding `RemoteDataSource` or `LocalDataSource` with no behavior.

## Mapping

- Convert data representations to domain only at repository boundaries.
- Put pure extensions in `data.mapper`, grouped by the domain model they produce.
- Use `toDomain()` and `toDb()`.
- Do not map inline in representations, sources, repositories, use cases, view models, or composables.
- Apply serialization annotations only to transport or persisted representations.

## Errors

- Always rethrow `CancellationException`; `runSuspendCatching` from `core-common` already does.
- Use typed domain results only for alternatives callers handle differently.
- Never expose protocol, database, provider, or internal data errors through repository contracts.
- A failed request must not erase the last successful cache unless invalidation is the domain result.

## Authenticated networking

The application owns one Ktor client, declared by `coreNetworkModule(baseUrl)`. Feature modules resolve `NetworkClient` with `get()`; they neither build nor close it.

- Mark authenticated requests with `authenticated()` instead of adding bearer headers manually.
- The shared modifier attaches the token and retries once after `401 Unauthorized` following a refresh.
- Repositories and sources do not implement token refresh or `401` retry.
- `AccessTokenProvider` serializes refresh, reuses a newer token, persists the new session, and publishes signed-out state when recovery fails.
- Translate only the final response after shared retry completes.

## Verification

Test protocol behavior and serialization at the source boundary. Test non-trivial mapping, defaults, invalid input, and parsing separately. Follow [Test Structure](../../testing/001-structure.md).

Repository behavior follows [Repositories](001-repositories.md).
