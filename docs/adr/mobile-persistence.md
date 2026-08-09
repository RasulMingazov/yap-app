# Mobile Persistence Composition

- Status: Accepted
- Date: 2026-08-09
- Scope: Room bootstrap, database ownership, and authenticated persistence under `apps/mobile/*`

## Context

The app should eventually share one physical database across authenticated features, but no mobile database module or authenticated-shell module exists yet. Room also requires its concrete `@Database` class to see every registered entity and DAO at compile time, while core modules cannot depend on features.

## Decision

Introduce `apps/mobile/core-database` only when a feature first needs local persistence. It owns generic platform driver and dispatcher bootstrap, such as `createRoomDatabase<T>(databasePath)`, but owns no feature entity, DAO, or concrete `@Database` class.

The concrete database belongs to the composition module that legitimately depends on every feature whose entities it registers.

- Until a second authenticated feature needs shared persistence, `feature-auth` may own a focused database directly.
- Introduce an authenticated composition module only when real cross-feature persistence or navigation requires it.
- The application host passes only `databasePath: String`; it does not construct DAOs, repositories, or storage implementations.
- Feature repositories receive focused DAOs through manual DI and do not own database bootstrap.
- Database-backed, user-owned containers and repositories are recreated with the authenticated branch and released on logout or account replacement.

This mirrors the server's generic `DatabaseFactory`: core infrastructure owns connection lifecycle, while feature-aware composition owns schemas and adapters.

## Consequences

Core modules remain feature-independent and Room's compile-time visibility requirement has a legitimate owner. A single-feature phase may temporarily use a focused feature database instead of speculative shared infrastructure.

## Compliance

Do not create shared mobile database or authenticated-shell modules before the ownership boundary exists. Intentional exceptions require a new or superseding ADR.
