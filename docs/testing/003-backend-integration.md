# Backend Integration Tests

## Context

Database invariants are PostgreSQL behavior. An in-memory fake cannot prove that the production adapter preserves transactions, constraints, concurrency, or cascades.

## Decision

Use Testcontainers with PostgreSQL when the behavior depends on the production database:

- transaction atomicity;
- concurrent updates or token rotation;
- uniqueness and foreign-key constraints;
- reuse and idempotency enforced by persisted state;
- cascades and Flyway migrations.

Use faster focused tests for behavior owned elsewhere:

- service tests with thin repository stubs for orchestration and domain outcomes;
- route tests for HTTP parsing and serialization;
- adapter tests for identity, token, or protocol translation.

Do not reproduce a database algorithm in a fake and treat the result as PostgreSQL verification.

Example scenario:

```text
GIVEN one valid refresh token
WHEN two rotations start concurrently
THEN only one commits and the persisted session remains valid
```

Follow [Test Structure](001-structure.md) for naming and scenario shape.

## Corner cases

- Coordinate concurrency with a barrier or latch; timing based on delays is not deterministic.
- Isolate database state between tests. A rollback is insufficient for migrations or behavior using multiple connections.
- Test migrations from the previous supported schema when one exists; otherwise verify clean bootstrap from all current migrations.
- Pin the same PostgreSQL major in deployment and Testcontainers configuration;
  never use `latest`. The first change that introduces either configuration owns
  defining this shared version.
- If the container runtime is unavailable, report that the integration suite was not run. Do not silently substitute a fake or claim database verification.
