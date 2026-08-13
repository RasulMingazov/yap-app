# Persistence

Each feature owns its persisted data while shared database infrastructure remains feature-independent.

- A feature owns repositories for its persisted aggregates, tables, and Flyway migrations.
- `core-database` owns connection, transaction bootstrap, and migration execution only.
- Identity verifiers and token services are not repositories.
- An Exposed repository is already a persistence adapter; add another source layer only for a real second boundary.
- Keep invariants inside the transaction that owns their atomic change.
- Keep Flyway migrations forward-only under the feature's `src/main/resources/db/migration`.
- Translate persistence-specific failures at the persistence boundary.

PostgreSQL-specific behavior follows [Backend Integration](../testing/003-backend-integration.md).
