# Presentation Components and Models

- Status: Accepted
- Date: 2026-08-08
- Scope: Component and model responsibilities in mobile features under `apps/mobile/*`

## Context

Business decisions drift into composables and lifecycle adapters when presentation responsibilities are not explicit. Components, retained models, UI mapping, and rendering need separate owners.

UI state and Compose are defined in [UI State and Compose Rendering](ui-state-and-compose.md). Navigation, child ownership, and restoration are defined in [Navigation, Child Components, and Retention](navigation-and-retention.md).

## Decision

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

- `Component` is the public lifecycle-aware contract.
- `DefaultComponent` is a thin Decompose adapter.
- `Model` owns operations and durable in-memory presentation facts.
- `UiStateMapper` derives ready-to-render state when meaningful mapping exists.
- Compose renders immutable state, owns transient visual behavior, and emits intent.

Do not add any of these types solely to satisfy a package template.

## Component contract

A component may expose `StateFlow<UiState>`, one-shot `Flow<News>`, user or parent `Event`, child component contracts, and a narrow `Factory`.

- Keep default components, models, and mappers `internal`.
- Publish only the minimum contract required across a Gradle-module boundary.
- Name events after intent, such as `SendClicked`, `TextChanged`, or `ErrorDismissed`.
- Avoid broad domain aggregates in UI contracts when a primitive or focused presentation type is sufficient.
- A small domain enum that presentation only names may cross the boundary instead of being copied into a parallel UI enum.
- Nest `UiState` and `Event` in their component. Nest payloads owned by `UiState` inside it.
- Import nested types and use their short names at call sites rather than repeatedly qualifying them with the component name.

## Default component

`Default...Component` delegates `ComponentContext`, retains its model through `InstanceKeeper` when needed, exposes state and news, and forwards events. It contains no business policy or duplicate mapping.

- A component creates its model from collaborators held by its factory; it does not accept a ready-made model.
- Reference children and siblings through their component interfaces, never default implementations.
- Drive a child through its `Event` contract instead of implementation-only methods.
- Use stable, component-local unique keys for retained models and child contexts.
- Factories create fresh dependencies for each new component-owned lifetime.

## Model

Presentation state holders are named `Model`. Use `core-common`'s `BaseModel` when retention, `modelScope`, teardown, or `mapState` is required.

- Nest immutable `DataState` inside its model and import it by its short name at use sites.
- Update state with `dataState.update { it.copy(...) }`; read `.value` only for a required synchronous snapshot.
- Set busy flags synchronously before launching guarded work.
- Clear stale errors on retry and preserve unrelated facts through `copy`.
- Suffix use-case dependencies with `UseCase`.
- Route non-trivial events to named `on...` functions.
- Handle typed domain outcomes exhaustively; infrastructure translation belongs below presentation.
- Close model-owned channels in `onCleared`; `BaseModel.clear()` invokes it before cancelling `modelScope`.

Models own loading, retry, and error facts needed by a screen. They do not own theme values, dimensions, focus, scroll position, or animation progress.

## Verification

Test model event-to-state and event-to-news behavior, typed outcome handling, duplicate-action guards, and cancellation. Do not test a component only to prove thin delegation. Destroy retained models in tests. Follow the [test-structure rules](../testing/001-test-structure.md).

## Consequences

Business behavior stays testable outside Compose and lifecycle adaptation stays thin. Meaningful presentation behavior may require explicit model state and mapping.

## Compliance

New presentation code follows this ADR. Existing code migrates when its affected area changes. Intentional exceptions must be justified in the pull request or superseded by a later ADR.
