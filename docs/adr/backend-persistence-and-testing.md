# Backend Persistence and Integration Verification

- Status: Accepted
- Date: 2026-08-09
- Scope: Feature persistence adapters and PostgreSQL verification under `services/server/*`

## Context

Database invariants, transactions, constraints, concurrency, and cascades are PostgreSQL behavior. In-memory fakes cannot prove that a production adapter preserves those semantics.

General backend module ownership is defined in [Feature-First Backend Boundaries](backend-feature-boundaries.md).

## Decision

A feature owns repositories for its persisted aggregates, its tables, and its Flyway migrations. `core-database` owns only connection, transaction bootstrap, and migration execution.

- Identity verifiers and token services are not repositories; keep them in their owning identity or security boundary.
- An Exposed repository is already a persistence adapter. Add another data-source layer only for a second implementation or a real boundary.
- Keep invariants inside the transaction that owns their atomic change.
- Keep Flyway migrations forward-only.
- Store feature migrations under that feature's `src/main/resources/db/migration`; `DatabaseFactory` scans the runtime classpath.
- Translate persistence-specific failures at the persistence boundary.

## Verification

Use Testcontainers with PostgreSQL to verify behavior that depends on the real database:

- transaction atomicity;
- concurrent updates and token rotation;
- reuse and idempotency;
- uniqueness and foreign-key constraints;
- cascades and migration behavior.

Use service tests with thin repository stubs for orchestration and actionable outcomes, route tests for HTTP parsing and serialization, and focused adapter tests for identity or token translation.

Do not reproduce database algorithms inside a fake repository and treat those tests as evidence for PostgreSQL behavior. Follow the [unit-testing rules](../testing/README.md).

## Consequences

Database-specific guarantees are verified against the production engine. Integration tests cost more time and infrastructure than in-memory tests, so they remain focused on behavior only PostgreSQL can prove.

## Compliance

New and changed persistence behavior follows this ADR. Intentional exceptions must be justified in the pull request or superseded by a later ADR.
