# Feature Boundaries

A mobile feature is two Gradle modules. `api` is what other features may see; `impl` is everything else.

```text
feature-auth/
├── api/    NavKey, use-case contracts, entities in their signatures
└── impl/   domain implementations, repository ports, data, presentation, di
```

- `impl` depends on its own `api`. `api` depends on nothing but `core-*`.
- A feature depends on a sibling's `api` only, never on its `impl`.
- `app-root` depends on both: `api` to compose destinations, `impl` to load the Koin module.
- Two features that need each other's `impl` are one feature.

## What belongs in `api`

- The feature's `NavKey` hierarchy.
- Use-case contracts — the interface only, never `Default...UseCase`.
- Entities and value objects appearing in those signatures.
- The Koin module function.

Everything else is `impl` and stays `internal`: repository ports and implementations, use-case
implementations, data sources, mappers, view models, and composables.

## Layers inside `impl`

```text
                  di
             /    |    \
            v     v     v
presentation --> domain <-- data
```

- `presentation` depends on `domain`, never on `data`.
- `data` implements ports owned by `domain`.
- `domain` depends on neither.
- Common code stays platform-neutral; platform source sets provide narrow adapters.
- Do not create a layer or abstraction before it owns behavior or a required boundary.

## Package shape

Use this as a menu, not an empty template:

```text
feature-auth/api/src/commonMain/kotlin/app/yap/feature/auth/api/
├── AuthNavKey.kt
├── entity/
└── usecase/

feature-auth/impl/src/commonMain/kotlin/app/yap/feature/auth/
├── domain/
│   ├── repository/
│   └── usecase/
├── data/
│   ├── mapper/
│   ├── remote/entity/
│   └── repository/
├── presentation/
└── di/
```

Keep child presentation slices in focused packages such as `presentation/signin`.

## Implementation order

1. Define behavior, state, outcomes, and ownership.
2. Create `api` with the destinations and use-case contracts the feature promises.
3. Create `impl`, then define domain ports and implement data, mapping, and repositories.
4. Add view models, presentation mapping, and rendering.
5. Declare the feature's Koin module, including its `navigation<Key> { }` entries.
6. Add focused tests and verify Android and iOS compilation.

Layer rules: [Domain](002-domain.md), [Repositories](data/001-repositories.md), [Data Sources](data/002-data-sources.md), and [Dependency Injection](003-dependency-injection.md).

Presentation rules: [View Models](presentation/001-view-models.md) and [UI and Compose](presentation/002-ui-compose.md).
