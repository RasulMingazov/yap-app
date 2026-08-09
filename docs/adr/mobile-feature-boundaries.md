# Mobile Feature Module Boundaries

- Status: Accepted
- Date: 2026-08-08
- Scope: Kotlin Multiplatform product features under `apps/mobile/*`

## Context

Product features need a consistent module and layer boundary so business behavior remains independent from UI, transport, persistence, and platform SDKs. The boundary must stay proportional to the feature instead of requiring empty layers or ceremonial abstractions.

Detailed decisions live in focused ADRs:

- [Repository Contracts and Cache Semantics](repository-contracts-and-cache.md);
- [Presentation Components and Models](presentation-components-and-models.md);
- [Manual Dependency Injection Containers](manual-dependency-injection.md);
- [Mobile Data Source Boundaries](mobile-data-sources.md);
- [Mobile Persistence Composition](mobile-persistence.md);
- [UI State and Compose Rendering](ui-state-and-compose.md);
- [Navigation, Child Components, and Retention](navigation-and-retention.md);
- [Mobile Domain Boundaries](mobile-domain-boundaries.md).

## Decision

One product feature owns one vertical Gradle module containing only the presentation, domain, data, and DI responsibilities it actually needs.

```text
                  di
             /    |    \
            v     v     v
presentation --> domain <-- data
      |
      v
   Compose
```

- `presentation` depends on `domain`, never on `data`.
- `data` implements contracts owned by `domain`.
- `domain` depends on neither `presentation` nor `data`.
- `di` may see all feature layers but only constructs and connects them.
- Compose renders presentation state and emits intent; it does not call domain or data dependencies directly.
- Common code remains platform-neutral. Narrow common contracts receive Android and iOS implementations from their platform source sets.

Do not create a layer, interface, mapper, use case, or package until it owns real behavior or a required boundary.

## Public module surface

A feature exposes only what another Gradle module needs:

- its root `FeatureComponent` contract;
- presentation models required by that contract;
- its public `FeatureContent` composable;
- a narrow `createFeatureContainer(...)`, component factory, or platform facade.

Default components, models, mappers, use cases, repositories, data sources, DTOs, and default containers remain `internal` or `private`. A container contract is public only when it is returned across a module boundary.

Host modules pass application or platform inputs into feature factories. They do not construct feature repositories, storage, data sources, or provider adapters. Product features must not depend on sibling feature implementations; only entry-point or composition modules may combine features.

## Package shape

Use this as a menu, not a required empty tree:

```text
feature-auth/src/commonMain/kotlin/app/yap/feature/auth/
├── presentation/
├── domain/
│   ├── entity/
│   ├── repository/
│   └── usecase/
├── data/
│   ├── exception/
│   ├── identity/
│   ├── local/entity/
│   ├── mapper/
│   ├── platform/entity/
│   ├── remote/entity/
│   └── repository/
└── di/
```

Child presentation slices live in focused packages such as `presentation/signin` or `presentation/challenge`.

## Implementation sequence

1. Define behavior, invariants, observable state, outcomes, and ownership.
2. Create the feature module with minimal dependencies.
3. Define required domain types and ports.
4. Implement external representations, adapters, mapping, and repository orchestration.
5. Define presentation contracts, models, mapping, and rendering.
6. Wire the manual container and expose one narrow entry point.
7. Integrate the feature into its owning navigator.
8. Add focused tests and verify Android and iOS compilation.

Introduce a dedicated authenticated navigator only when a second authenticated feature needs it.

## Consequences

Features remain independently buildable and implementation details stay replaceable. Real features require explicit mapping and wiring, while unused architectural ceremony is intentionally absent.

## Compliance

New features follow this ADR. Existing code migrates when its affected area changes; unrelated code is not rewritten solely to match the package tree.

Intentional exceptions must be justified in the pull request or superseded by a later ADR.
