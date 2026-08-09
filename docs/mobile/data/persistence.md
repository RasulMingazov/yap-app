# Persistence

Use one physical Room database per logical persistence zone. Features do not own separate databases.

## Ownership

- `core-database` owns reusable platform bootstrap such as drivers, paths, builders, and dispatchers.
- A feature owns its Room entities, DAO contract, mapping, and repository implementation.
- The zone composition owns the concrete `@Database`, registered schemas, and database lifecycle.
- Core infrastructure never depends on feature schemas.
- Sharing a database does not allow features to depend on sibling repositories or implementations.

Room entities and DAO contracts are public because the zone database must register
and provide them across a Gradle-module boundary. Treat them as a narrow persistence
integration API: only the zone composition and the owning feature may use them.
Mappers, repository implementations, sources, and other data representations remain
`internal` or `private`.

## Zones

- The authenticated composition owns one shared database for authenticated features.
- The unauthenticated zone has no database unless it gains persisted data with its own lifetime.
- Create a separate physical database only when data has a genuinely different security, cleanup, or lifetime boundary.
- Do not create a database per feature.

## Composition

- The application host provides platform inputs such as `databasePath`; it does not construct DAOs or repositories.
- The zone database exposes focused DAOs to feature containers through [Dependency Injection](../dependency-injection.md).
- Features neither create nor close the shared database.

## Lifetime

- Keep one active database instance per zone and path.
- Recreate or clear authenticated persistence when the active identity changes according to the product's data-retention policy.
- The zone that creates the database also closes it.
