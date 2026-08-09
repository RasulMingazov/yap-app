# Feature-First Backend Boundaries

- Status: Accepted
- Date: 2026-08-08
- Scope: Module and package ownership under `services/server/*`

## Context

The backend needs boundaries that keep HTTP, credential, and database details out of business orchestration. Existing `core-config`, `core-database`, and `core-security` modules already represent independently reusable infrastructure, while product scenarios belong to vertical feature modules.

Backend persistence and integration verification are defined separately in [Backend Persistence and Integration Verification](backend-persistence-and-testing.md). Mobile features follow [Mobile Feature Module Boundaries](mobile-feature-boundaries.md) instead of copying this server layout.

## Decision

Use two kinds of server module:

1. Cross-cutting `core-*` infrastructure owns a capability with an independent reuse, build, or lifecycle boundary and depends on no feature.
2. A `feature-*` module owns one vertical product slice: API routes, a cohesive service, framework-free models and failures, feature persistence, and adapters not reused elsewhere.

Current core ownership is:

- `core-config`: environment loading and validation;
- `core-database`: connection-pool and migration bootstrap, with no feature tables or repositories;
- `core-security`: token and credential issuance and verification.

`services/server/app` is the composition module. It owns process startup and shutdown, shared Ktor plugins and error mapping, readiness and liveness, and manual application graph construction.

```text
app (composition + process infrastructure)
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

A feature may depend on `core-*`; a core module never depends on a feature; `app` may depend on both. Do not create a module for a package used by only one feature. Promote it only when independent reuse, build, lifecycle, or ownership justifies the boundary.

## Application ownership

`app` invokes configuration and database lifecycle owned by their core modules; it does not reimplement them. It may depend on concrete feature and core implementations for wiring. Feature and core modules must not depend on `app`.

## Feature shape

Use this as a menu rather than an empty package template:

```text
feature-auth/
├── api/          Ktor routes and focused HTTP contracts
├── model/        framework-free values and failures
├── identity/     feature-owned provider verification
├── persistence/  repository port, adapter, tables, migrations
└── AuthService   cohesive scenario orchestration
```

- Routes validate and translate HTTP only; they contain no business orchestration.
- Keep request and response contracts near their routes and group only related endpoints.
- Translate expected feature failures through shared status handling.
- Keep credential responses credential-only; profile data is a separate API concern.
- Do not expose provider exceptions, database rows, JWT claims, or framework types as feature models.
- Keep provider-specific configuration with its owning feature; `core-config` owns generic loading and validation, not product-provider ownership.
- Use one cohesive service for a related scenario set instead of mirroring each endpoint with a use-case interface and implementation.
- Extract a policy only when it owns an independent rule, state, or lifecycle.
- Preserve coroutine cancellation and translate provider-specific failures at the adapter boundary.
- Default declarations to `internal`; widen visibility only for a real module or runtime boundary.
- Wire dependencies manually.

## Consequences

Feature behavior stays cohesive while genuinely reusable infrastructure remains independent. Package boundaries inside a feature rely on disciplined visibility, and promotion to a core module requires judgment.

## Compliance

New backend features follow this ADR. Existing code migrates when its area changes. Intentional exceptions must be justified in the pull request or superseded by a later ADR.
