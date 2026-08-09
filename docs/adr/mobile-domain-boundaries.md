# Mobile Domain Boundaries

- Status: Accepted
- Date: 2026-08-09
- Scope: Domain models, ports, results, and use cases under `apps/mobile/*`

## Context

The domain layer expresses product language and policy. Framework types, transport details, and pass-through abstractions make that language unstable and couple behavior to infrastructure.

## Decision

Limit domain packages to `entity`, `repository`, and `usecase`, and create only those that own real behavior or a required boundary.

- Entities and value objects are immutable, use product language, and enforce genuine business invariants.
- Domain code has no Compose, Decompose, Ktor, serialization, database, resource, or platform SDK dependencies.
- Repository ports describe business operations with domain types and read-only flows. They expose no DTOs, status codes, credentials, storage models, or mutable flows.
- Expected alternatives use typed domain results only when they change caller behavior.
- Use cases represent actor intent, business policy, orchestration, or a deliberate presentation test boundary.
- Do not create a use case merely to mirror every repository method.
- Keep default use-case implementations `internal` unless a module boundary requires otherwise.

Repository naming and cache behavior follow [Repository Contracts and Cache Semantics](repository-contracts-and-cache.md). Infrastructure translation follows [Mobile Data Source Boundaries](mobile-data-sources.md).

## Consequences

Product behavior remains platform-neutral and testable. Some simple repository operations may be consumed without a pass-through use case when no policy or test boundary justifies one.

## Compliance

New and changed domain code follows this ADR. Intentional exceptions must be justified in the pull request or superseded by a later ADR.
