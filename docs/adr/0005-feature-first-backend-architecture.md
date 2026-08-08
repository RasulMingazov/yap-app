# ADR-0005: Feature-First Backend Architecture

- Status: Accepted
- Date: 2026-08-08
- Scope: `services/server/*` in `yap-app`

## Agent essentials

For ordinary backend work, read this section and only the detailed sections governing the existing boundary being changed. Read the full ADR when creating or restructuring a feature, route layer, service boundary, repository, or infrastructure adapter.

- Use one Gradle module per feature (e.g. `services/server/feature-auth`) with feature-first packages inside it: the feature's own API, scenarios, framework-free models, and any adapter not reused elsewhere. Cross-cutting infrastructure with its own reuse, build, or ownership boundary — configuration (`core-config`), persistence bootstrap (`core-database`), and credential/token issuance (`core-security`) — lives in its own module instead of a feature's package tree.
- Routes translate HTTP only, a cohesive feature service orchestrates scenarios, and `core-security`/feature-owned adapters translate infrastructure details behind narrow ports.
- Keep Ktor, serialization, JWT SDK, Exposed, and PostgreSQL types out of feature-independent models and failures.
- Default to `internal` within a module, wire dependencies manually, and add a new module only for a capability with a real independent reuse, build, or ownership boundary — not for every package inside an existing feature module.
- Keep atomic invariants inside the owning transaction and verify PostgreSQL-specific concurrency, constraints, and cascades against PostgreSQL through Testcontainers.

## Context

The backend needs boundaries that keep HTTP, credential, and database details out of business orchestration. `yap-app`'s server already expresses some of those boundaries as separate Gradle modules — `core-config`, `core-database`, `core-security` — rather than as packages inside a single application module, because those capabilities are meant to be reused by more than the one feature that currently consumes them (`feature-auth`). This ADR ratifies that existing split and defines the boundary a feature module itself should keep internally, rather than introducing a different structure. Mobile and server code share product language (`shared/contract/auth`), but their runtime responsibilities are different: the backend must not reproduce the mobile client's presentation or `data/local/remote` layering (see [ADR-0002: Feature Module Clean Architecture](0002-feature-module-clean-architecture.md)).

## Decision

`services/server` has two kinds of Gradle module:

1. **Cross-cutting infrastructure modules** own a capability with an independent reuse boundary and depend on no feature module:
   - `core-config` — environment loading and validation (`AppConfig`, `AuthConfig`, `DatabaseConfig`, `AppEnvironment`, `AppConfigLoader`);
   - `core-database` — connection-pool and migration bootstrap only (`DatabaseFactory`); it owns no entities, DAOs, or feature tables of its own;
   - `core-security` — token/credential issuance and verification (`TokenService`/`JwtTokenService`, `IssuedTokens`, `RefreshToken`, `SecurityChallenge`, `SessionIdentity`).
2. **Feature modules** own one complete vertical slice beneath their own package, e.g. `app.yap.server.feature.auth`: API routes, a cohesive feature service, framework-free models and failures, feature-owned persistence, and any adapter not (yet) reused by a second feature — such as Google/Apple identity-provider verification for `feature-auth`. `AuthConfig`'s own file comment already records this intent: identity-provider-specific settings belong to the feature that consumes them, not to shared config.

`services/server/app` is the composition module: process startup/shutdown, shared Ktor plugin wiring, readiness/liveness, and manual construction of the object graph from the `core-*` and `feature-*` modules it depends on.

```text
app (composition + process infra)
        |
        v
feature-auth: api -> service -> model
     |              |      ^
     v              v      |
   Ktor      identity/persistence adapters
                    |            |
                    v            v
             core-security   core-database
                    ^
                    |
                core-config
```

Dependencies point inward and downward: a feature module may depend on `core-*` modules; a `core-*` module must never depend on a feature module; `app` may depend on both to wire them together — matching the constitution's Module Boundary Isolation principle. Do not add a new module for a package that only one feature currently uses — keep it inside that feature's own module until a second feature genuinely needs to reuse it, the same threshold that already justified splitting out `core-config`, `core-database`, and `core-security`.

## Application ownership

`services/server/app` owns:

- process startup and shutdown;
- shared Ktor plugins and error mapping;
- readiness and liveness checks (backed by `core-database`'s `DatabaseFactory.isReady()`);
- construction of application-scoped dependencies by wiring together the `core-*` and `feature-*` modules it depends on.

Environment configuration loading and validation is not part of `app` here — it already lives in `core-config` as its own module (`AppConfigLoader`, `AppConfig`) — and database/connection-pool lifecycle already lives in `core-database` (`DatabaseFactory.init(config)`/`close()`), invoked by `app` at startup rather than implemented there.

Application wiring (`app`) may depend on concrete feature and core module implementations. Feature and core modules must not depend on `app`.

## Feature layout

Use `feature-auth` as the reference shape:

```text
feature-auth/
├── api/          Ktor routes and focused HTTP contracts
├── model/        framework-free values and failures
├── identity/     Google/Apple identity-provider verification (feature-owned)
├── persistence/  repository port, PostgreSQL adapter, feature-owned tables
└── AuthService   cohesive scenario orchestration
```

`AuthService` depends on `core-security`'s `TokenService` port for credential issuance and on its own `identity`/`persistence` packages for everything specific to the auth feature. Treat this as a menu of responsibilities: `feature-auth` has no source under any of these packages yet (`services/server/feature-auth/build.gradle.kts` currently only declares its module dependencies on `core-database`, `core-security`, and `shared:contract:auth`), so do not create empty packages ahead of the behavior that will own them.

## API boundary

- Keep request and response contracts close to their routes and group only tightly related endpoints.
- Validate transport shape at the API boundary and translate expected feature failures through shared status handling.
- Keep routes free of business orchestration.
- Keep credential responses credential-only; profile data belongs to a separate API concern.
- Do not expose provider exceptions, database rows, JWT claims, or framework types as API-independent feature models.

## Services and policies

- Use one cohesive service for a related scenario set instead of mirroring every endpoint with a use-case interface and implementation.
- Extract a policy object only when it owns an independent rule, state, or lifecycle.
- Preserve coroutine cancellation. Translate provider-specific failures at the identity boundary and persistence-specific failures at the persistence boundary.
- Default declarations to `internal`; widen visibility only for a real module or runtime boundary — the same rule that already justifies `core-security`'s `TokenService` being public across the module boundary while its implementation details stay internal.

## Persistence

- Repositories represent persisted domain aggregates owned by a feature; identity verifiers and token services are not repositories, which is why they live outside `persistence` (in `identity` and `core-security` respectively).
- An Exposed repository is already a persistence adapter. Add another data-source layer only for a second implementation or a real test seam.
- Keep transactions around invariants that must change atomically.
- Keep Flyway migrations forward-only. `core-database`'s `DatabaseFactory` scans `classpath:db/migration` across the whole runtime classpath, so each feature module owns its own migrations under its own `src/main/resources/db/migration` rather than centralizing them in `core-database`.
- Test PostgreSQL transaction, concurrency, constraint, and cascade behavior against PostgreSQL through Testcontainers rather than reproducing it in an in-memory fake.

## Testing

Test behavior at the boundary that owns it:

- routes for HTTP parsing, status, headers, authentication, limits, and serialized contracts;
- services for scenario orchestration and actionable outcomes using thin stubs;
- identity and `core-security` token adapters for translation and credential rules — `JwtTokenServiceTest` in [ADR-0001](0001-unit-test-conventions.md) is the reference shape a `core-security` test already follows;
- PostgreSQL repositories for transactions, concurrency, rotation, reuse, constraints, and cascades.

Do not put database algorithms into a fake repository and then use that fake as evidence that the PostgreSQL adapter has the same behavior. Detailed test shape and maintenance rules live in [ADR-0001: Unit Test Conventions](0001-unit-test-conventions.md).

## Consequences

Benefits:

- This architecture keeps feature behavior cohesive and framework details replaceable while avoiding module and interface ceremony inside a feature.
- Cross-cutting capabilities that already need reuse across features (config, persistence bootstrap, token issuance) are isolated in their own modules instead of ceremonially living inside one growing application module.

Costs:

- Package boundaries inside a feature module rely on disciplined visibility and review, because a single Gradle module cannot enforce every dependency direction on its own.
- Because `core-config`, `core-database`, and `core-security` already exist, a second feature gets to reuse them for free — but a package must earn that same promotion; the boundary requires judgment rather than a rule that fires automatically.

## Compliance and migration

New features follow this ADR immediately. Existing code is migrated when its area changes materially; unrelated code is not restructured solely to match the package tree.

Any intentional exception must be justified in the pull request description, per the constitution's Governance section, or superseded by a later ADR.
