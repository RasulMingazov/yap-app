# ADR-0002: Feature Module Clean Architecture

- Status: Accepted
- Date: 2026-08-08
- Scope: Kotlin Multiplatform product feature modules in `yap-app` (`apps/mobile/*`)

## Agent essentials

For ordinary feature work, read this section and only the detailed sections governing the boundaries being changed. Read the full ADR when creating or restructuring a feature module or changing layer or module ownership.

- One product feature owns one vertical Gradle module (e.g. `apps/mobile/feature-auth`) containing only the presentation, domain, data, and DI responsibilities it actually needs.
- Dependencies point `presentation -> domain <- data`; DI wires concrete implementations, and Compose never calls domain or data dependencies directly.
- Expose only the root component contract, required presentation models, public content, and a narrow factory or platform facade. Keep implementations internal.
- Host modules pass application or platform inputs into feature factories; they do not construct feature repositories, storage, data sources, or provider adapters. Today `app-root` is the only host module.
- Keep common code platform-neutral and implement narrow platform contracts in `androidMain`/`iosMain` source sets — `core-network`'s `PlatformHttpClient.android.kt`/`PlatformHttpClient.ios.kt` split is the existing reference shape.
- Do not create empty layers, ceremonial interfaces, pass-through use cases, or speculative abstractions.

## Context

Product features need a consistent structure that keeps business behavior independent from UI, transport, persistence, and platform SDKs. The structure must also fit the current and intended stack: Compose Multiplatform, coroutines, manual dependency injection, Android, iOS, and Decompose for navigation and retained lifecycle.

Decompose is not yet declared in `gradle/libs.versions.toml`. `feature-auth`, the only scaffolded feature module today, has no presentation, domain, or data source of its own — it currently only declares dependencies on `core-design`, `core-network`, and `shared:contract:auth` (`apps/mobile/feature-auth/build.gradle.kts`). This ADR defines the target architecture for when that behavior is built. Add the Decompose dependency and its navigation wiring when a feature first needs real navigation or a retained lifecycle beyond a single root component — not speculatively, per the constitution's Simplicity & YAGNI principle.

Without an explicit boundary, feature work tends to leak DTOs into presentation, move business decisions into composables, expose implementations across Gradle modules, or add layers that only forward calls. This ADR defines the default architecture for a new feature and the target structure when an existing feature is changed substantially.

## Decision

Each product feature is a vertical Gradle module that owns its presentation, domain, data implementations, and dependency wiring.

```text
                  di
             /    |    \
            v     v     v
presentation --> domain <-- data
      |
      v
   Compose
```

The dependency rules are:

- `presentation` depends on `domain`, never on `data`.
- `data` depends on `domain` to implement repository contracts.
- `domain` depends on neither `presentation` nor `data`.
- `di` may see all feature layers, but only constructs and connects them.
- Compose renders presentation state and emits events; it does not invoke domain or data dependencies directly.
- Platform source sets implement narrow contracts declared in common code.

Layers are added only when they own a real responsibility. A feature with no remote, local, identity, or platform integration omits those data packages instead of creating placeholders.

## Module boundary

A feature normally exposes only:

- its root `FeatureComponent` contract;
- presentation models required by that contract;
- its public `FeatureContent` composable;
- a narrow `createFeatureComponentFactory(...)` or platform facade.

Default components, models, mappers, use-case implementations, repository contracts used only inside the feature, data sources, repositories, DTOs, and containers remain `internal` or `private`.

Host modules pass platform or application-scoped inputs into the feature factory. They do not construct the feature's repositories, data sources, storage implementations, or provider adapters. Today `app-root` is the only host module, and it depends only on `feature-auth` and `core-design` (`apps/mobile/app-root/build.gradle.kts`) — not on any of `feature-auth`'s internal data or domain types.

## Package layout

Use `feature-auth` as the reference shape. This is a menu of owned responsibilities, not a requirement to create empty directories:

```text
feature-auth/
└── src/
    ├── commonMain/kotlin/app/yap/feature/auth/
    │   ├── presentation/
    │   │   ├── AuthComponent.kt
    │   │   ├── DefaultAuthComponent.kt
    │   │   ├── AuthModel.kt
    │   │   ├── AuthUiStateMapper.kt
    │   │   └── AuthContent.kt
    │   ├── domain/
    │   │   ├── entity/
    │   │   ├── repository/
    │   │   └── usecase/
    │   ├── data/
    │   │   ├── exception/
    │   │   ├── identity/
    │   │   ├── local/entity/
    │   │   ├── mapper/
    │   │   ├── platform/entity/
    │   │   ├── remote/entity/
    │   │   └── repository/
    │   └── di/
    ├── androidMain/kotlin/app/yap/feature/auth/
    ├── iosMain/kotlin/app/yap/feature/auth/
    └── commonTest/kotlin/app/yap/feature/auth/
```

Child presentation units live under focused packages such as `presentation/signin` or `presentation/challenge`. The root component composes them without absorbing their internal state.

## Presentation

Detailed component, model, mapper, Compose, navigation, and lifecycle rules are defined in [ADR-0004: Presentation Architecture](0004-presentation-architecture.md).

Presentation follows this pipeline:

```text
domain result or flow
        ↓
Model.DataState
        ↓
UiStateMapper
        ↓
Component.UiState
        ↓
Compose
```

- `Component` is the public presentation contract: observable `UiState`, input `Event`, optional one-shot `News`, child components, and `Factory`.
- `DefaultComponent` adapts Decompose lifecycle, retains the model, exposes its state/news, and forwards events. It contains no business decisions.
- `Model` owns operations and `DataState`. It depends on use-case interfaces for actor intents, not on data implementations. `core-common`'s `BaseModel` (`app.yap.core.common.presentation.BaseModel`) already provides the `modelScope`, `clear()`/`onCleared()`, and `mapState` machinery every feature model builds on.
- `UiStateMapper` derives presentation-only values, resource choices, stable item keys, and action availability when that mapping contains meaningful decisions.
- `UiState` contains ready-to-render presentation values. Domain entities do not cross into Compose when a focused UI model or primitive is sufficient.
- `News` is reserved for effects consumed once. Repeatable visual facts remain in `UiState`.
- Compose resolves resources, owns transient visual state, renders `UiState`, and dispatches user intent.

Use Decompose navigation models for mutually exclusive children so inactive routes do not eagerly initialize models or subscriptions. Use `InstanceKeeper` for live retained objects and `StateKeeper` only for small serializable state that must survive process death; durable data returns from repository or storage.

## Domain

The domain contains only `entity`, `repository`, and `usecase` packages.

- Entities and value objects are immutable, expressed in product language, and contain genuine business invariants.
- Domain types have no Compose, Decompose, Ktor, serialization, database, resource, or platform SDK dependencies.
- Repository contracts describe business operations using domain types and read-only flows. They expose no DTOs, status codes, credentials, storage models, or mutable flows.
- Use cases represent actor intent, business policy, orchestration, or a deliberate presentation test boundary. A use case is not required merely to mirror every repository method.
- Expected results distinguish only outcomes that change caller behavior.

## Data

Detailed repository ownership, method naming, cache, refresh, and mutation semantics are defined in [ADR-0003: Data Layer and Repository Conventions](0003-data-layer-and-repository-conventions.md).

- Remote models end in `Request` or `Response`; persisted serialized models end in `Db`.
- Remote and local data sources operate only on primitives and their own representations, never on domain entities.
- Pure conversions live in `data.mapper`, grouped by the domain entity they produce.
- Repository implementations are the boundary where data representations become domain entities.
- Repositories own caching, synchronization, refresh, retry, fallback, persistence, and coordination policies.
- HTTP, JSON, database, secure-storage, and SDK details stay in their data source or platform adapter.
- Coroutine `CancellationException` is always rethrown. Other infrastructure failures are translated into the smallest caller-actionable domain result.

Android and iOS implementations live in their corresponding source sets under the same logical package as the common contract.

## Dependency injection and lifetime

Each feature uses an internal `DefaultFeatureContainer` to construct concrete implementations and bind them to contracts.

- Application-scoped infrastructure may include authentication session state and shared platform inputs.
- Authenticated/user-scoped feature containers are created with the authenticated component branch and released with it. This matters starting with the first feature added after `feature-auth` that requires a signed-in session.
- Component factories may be longer-lived only when each invocation creates fresh dependencies for the new owning scope.
- A DI framework is not introduced without a separate architecture decision.

## Testing

Detailed unit-test naming, verification, complexity, and test-double rules are defined in
[ADR-0001: Unit Test Conventions](0001-unit-test-conventions.md).

Test behavior at the boundary that owns it:

- entities and pure business rules with direct unit tests;
- use cases when they contain policy or orchestration;
- repository implementations with fake remote/local/platform contracts;
- models through event-to-state/news behavior;
- UI-state mappers through domain/data-state-to-presentation mapping;
- navigation and restoration when initial destination or process recovery can regress.

Cover meaningful success, actionable failure, retry, cancellation, caching/refresh, and duplicate-action protection. Do not add tests that only prove a one-line delegate calls another interface.

For a broad feature change, run focused common/Android tests, compile an iOS target, and assemble the Android app. UI changes also require visual verification when a device or simulator is available.

## New feature sequence

Implement a new feature in this order:

1. Define product behavior, invariants, observable state, actionable outcomes, and ownership.
2. Create the feature module and its minimal dependencies.
3. Define domain entities/results, repository ports, and necessary use cases.
4. Define external representations and data-source contracts.
5. Implement mappers and repository orchestration.
6. Define the component contract, model state/events, mapper, and child ownership.
7. Implement Compose rendering and interactions.
8. Wire the internal container and expose one narrow feature factory.
9. Integrate the feature into its owning navigator. Today that is `app-root` directly; introduce a dedicated authenticated navigator module only once a second authenticated feature exists, per the constitution's Simplicity & YAGNI principle.
10. Add focused tests and run Android/iOS verification.

## Consequences

Benefits:

- Business rules remain testable and platform-independent.
- Transport, persistence, and SDK changes stay behind data boundaries.
- Feature modules expose a small stable surface.
- Android and iOS share behavior while retaining platform-specific adapters.

Costs:

- Features with real presentation and data behavior require explicit mapping and DI wiring.
- The architecture relies on disciplined visibility and dependency direction because Kotlin packages do not enforce every layer boundary.

The cost is accepted. Ceremonial interfaces, empty packages, speculative use cases, and pass-through abstractions are not required by this decision.

## Compliance and migration

New features follow this ADR from their first implementation. Existing features are migrated opportunistically when their affected area changes; unrelated code is not rewritten solely to match the package tree.

Any intentional exception must be justified in the pull request description, per the constitution's Governance section, or superseded by a later ADR.
