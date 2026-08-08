# ADR-0004: Presentation Architecture

- Status: Accepted
- Date: 2026-08-08
- Scope: Presentation code in `yap-app` mobile feature modules (`apps/mobile/*`)

## Agent essentials

For ordinary presentation work, read this section and only the detailed sections governing the changed component, model, mapper, navigation, lifecycle, or Compose code. Read the full ADR when establishing or restructuring presentation architecture.

- Keep the pipeline `domain -> Model.DataState -> UiStateMapper -> Component.UiState -> Compose` and do not merge responsibilities for convenience.
- Components are lifecycle-aware contracts, default components are thin Decompose adapters, models own operations and durable in-memory facts, and Compose only renders immutable state and emits intent.
- Add a mapper only when it owns meaningful presentation decisions or a useful test seam. Keep transient visual state in Compose.
- Use `UiState` for repeatable rendered facts and `News` or `Effect` only for one-shot output.
- Keep mutually exclusive navigation lazy, retain live objects with `InstanceKeeper`, use `StateKeeper` only for small process-restorable state, and make restoration idempotent.
- Test behavior at the owning model, mapper, navigation, lifecycle, or Compose boundary; do not test thin delegation alone.

## Context

Feature presentation uses Compose Multiplatform and, once a feature needs real navigation or a retained lifecycle, Decompose — see [ADR-0002: Feature Module Clean Architecture](0002-feature-module-clean-architecture.md) for that dependency's current status (not yet declared in `gradle/libs.versions.toml`).

Without an explicit ownership model, business decisions drift into composables, lifecycle adapters accumulate state, domain entities leak into UI contracts, and inactive navigation branches start work eagerly. This ADR defines the boundary between components, retained models, UI-state mappers, composables, and child navigation. It is the source of truth for presentation changes in `yap-app` regardless of which coding agent or IDE performs them.

`feature-auth`, the only scaffolded feature so far, has no presentation code yet (`apps/mobile/feature-auth/build.gradle.kts`). The shared building block it and future features build on already exists in `core-common`: `BaseModel` (`app.yap.core.common.presentation.BaseModel`) provides a `SupervisorJob`-backed `modelScope`, `clear()`/`onCleared()` teardown, and a `StateFlow`-preserving `mapState` helper.

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

Responsibilities must remain distinct:

- `Component` is the public, lifecycle-aware presentation contract.
- `DefaultComponent` adapts Decompose and delegates behavior.
- `Model` owns operations and durable in-memory presentation facts.
- `UiStateMapper` derives ready-to-render presentation state when mapping contains meaningful decisions.
- Compose renders state, owns transient visual behavior, and emits user intent.

Do not add a component, model, or mapper merely to satisfy a package template. Add it when the responsibility exists.

## Component contract

A component may expose:

- `StateFlow<UiState>` for repeatable rendered state;
- `Flow<News>` for output consumed once;
- `Event` as user or parent intent;
- child component contracts;
- a narrow `Factory` used by the owning navigator or feature entry point.

Keep default components, models, and mappers `internal`. Only the minimum contract required across a Gradle-module boundary is public.

Name events after intent, such as `SendClicked`, `TextChanged`, or `ErrorDismissed`. Do not expose domain entities through public UI state or events when a primitive or focused presentation model expresses the same fact. A small domain enum that presentation only names, never interprets, is not worth a parallel presentation copy: publish the domain type instead of a duplicate that has to be mapped back and forth.

Nest `UiState` and `Event` inside their owning `Component`, and nest any state `UiState` owns, such as an error or snackbar payload, inside `UiState` in turn. Import the nested type and reference it by its bare name (`UiState`, `UiState.ErrorSnackbar`, `Event.ProviderClicked`) at the point of use — mapper signatures, model properties, `dispatch` signatures and `when` branches, composable parameters, test assertions — instead of qualifying it with the owning component name (avoid `SignInComponent.Event.ErrorDismissed`).

## Default component

`Default...Component` delegates `ComponentContext`, retains the model through `InstanceKeeper` when needed, exposes state and news, and forwards events. It contains no business policy and does not duplicate model or mapper decisions.

A default component creates its own model from the collaborators its factory holds. Do not accept a ready-made model as a constructor parameter; the component owns its model's lifetime.

Reference children and siblings through their `Component` interface, never through a `Default...Component` type. A parent that needs to drive a child does so with an `Event` on that interface, not an extra method on the implementation.

Use stable, component-local unique keys for retained models and child contexts. Factories construct fresh dependencies for the lifetime owned by each new component.

## Model

Presentation state holders are named `Model`. Use `core-common`'s `BaseModel` when retained lifetime, `modelScope`, or `mapState` is needed — it already implements the scope, teardown, and mapping machinery; feature models do not reimplement it.

- Nest immutable `DataState` inside its owning model. Import the nested type and reference it by its bare name (`DataState`) at the point of use — mapper signatures, model properties, test assertions — instead of qualifying it with the owning model name (avoid `SignInModel.DataState`).
- Change it with `dataState.update { it.copy(...) }`; read `.value` only for a required synchronous snapshot.
- Set busy flags synchronously before launching guarded work so duplicate events cannot pass the guard.
- Clear stale errors when retrying and preserve unrelated facts through `copy`.
- Suffix use-case dependencies with `UseCase` and route non-trivial events to named `on...` functions.
- Handle typed domain outcomes exhaustively; infrastructure-error translation belongs below presentation.
- Close model-owned channels in `onCleared` — `BaseModel.onCleared()` is the hook `clear()` already calls before cancelling `modelScope`.

The model owns loading, retry, and error facts needed by the screen. It does not own theme values, dimensions, focus, scroll position, or animation progress.

## UI state, mapping, and news

`UiState` contains immutable, ready-to-render presentation values. Use focused item models with stable keys instead of passing broad domain aggregates into Compose.

Annotate a `UiState` that holds a collection with `@Immutable`. The Compose compiler treats `List`, `Set`, and `Map` as unstable, which makes the whole state unstable and stops composables that take it from skipping recomposition. Only annotate state that is genuinely immutable: build its collections once in the mapper and never mutate them afterwards.

Introduce a `UiStateMapper` interface and internal `Default...UiStateMapper` only when mapping contains presentation decisions or provides a useful test seam. The mapper may derive action availability, item presentation types, stable keys, and resource choices. It must not own theme colors, dimensions, measurements, focus, scrolling, or animation state.

Use `News` or `Effect` only for actions consumed once, such as navigation requests or scrolling commands. Repeatable visual facts remain in `UiState`. When delivery must be one-shot, use a buffered channel exposed as a flow and close the channel with its model.

## Compose renderer

Composable content accepts explicit immutable UI state and callbacks. It must not fetch data, call repositories or use cases, interpret business outcomes, or coordinate feature lifetimes.

- Keep user-visible strings in Compose resources.
- Resolve presentation-selected resources in Compose.
- Keep colors, dimensions, measurements, focus, scroll state, animation progress, and other transient visual state in Compose.
- Reuse `core-design` for deliberate feature-agnostic primitives with stable APIs — `YapTheme` and the app's type scale already live there (`apps/mobile/core-design/src/commonMain/kotlin/app/yap/core/design/theme/`); keep feature-specific variants and resources in the feature.
- Give lazy-list items stable keys.
- Key effects with the values that define their lifetime and ensure long-lived effects observe current callbacks.
- Follow supplied design values and verify both light and dark themes when styling changes.

Extract a composable when it is a meaningful visual unit, owns a coherent interaction, or materially clarifies its parent. Do not create generic helpers for one-off expressions.

## Children and navigation

Use `ChildStack` or another appropriate Decompose navigation model for mutually exclusive destinations. Inactive destinations must not eagerly create components, models, collectors, or data loads.

The owning parent coordinates children through shared domain state or a small explicit contract. A child does not depend on sibling internals. A root component composes children and routes events without absorbing their state or implementation details.

Navigation ownership is hierarchical, though `yap-app` has not yet grown enough features to need every level:

- a root navigation component selects bootstrap, unauthenticated, or authenticated state — this responsibility currently lives directly in `app-root` (`apps/mobile/app-root`), which composes `feature-auth`;
- an authenticated navigator owns authenticated product navigation once a second authenticated feature exists — see [ADR-0002](0002-feature-module-clean-architecture.md) and [ADR-0003](0003-data-layer-and-repository-conventions.md) for when to introduce that module;
- each product feature owns its nested navigation and children.

## Splitting one screen into presentation components

Split a screen by independently owned state and interaction, not merely by visual size.

- Keep the root `FeatureComponent` and `FeatureContent` responsible for screen-level composition, shared scaffold, navigation hand-off, and coordination between children.
- Give a section its own `Component`, `DefaultComponent`, `Model`, `UiStateMapper`, and `Content` when it owns an independent state stream, loading/error lifecycle, operation, news, or reusable interaction boundary.
- Place the complete child slice in one focused package, for example `presentation/signin` or `presentation/challenge`. Do not leave child rendering in a monolithic root content file after extracting its component and model.
- Let each child `Content` accept only its own ready-to-render state and callbacks. It must not reach into the root component or a sibling component.
- The root content may choose section order and combine child facts only for genuine screen-level states. It must not duplicate child mapping or absorb child-specific rendering.
- Keep purely shared screen visuals, such as loading/empty/error surfaces or formatting used by multiple children, in small root presentation files. Move a primitive to `core-design` only when it is feature-agnostic and intentionally reused across features.
- Do not create a child component for a static visual fragment with no independent state, lifecycle, operation, or interaction ownership; extract only a local composable in that case.

This produces slices such as:

```text
presentation/
├── AuthComponent.kt
├── AuthContent.kt
├── AuthStateContent.kt
├── signin/
│   ├── SignInComponent.kt
│   ├── DefaultSignInComponent.kt
│   ├── SignInUiStateMapper.kt
│   └── SignInContent.kt
└── challenge/
    └── ...
```

## Retention and restoration

- Use `InstanceKeeper` for live objects retained across configuration changes.
- Use `StateKeeper` only for small serializable state that must survive process death.
- Restore durable data from repository or storage instead of serializing large histories into presentation state.
- Derive an already-known initial destination synchronously so restoration does not flash a bootstrap or empty child.
- Make restoration idempotent and prevent duplicate requests, collectors, navigation events, dialogs, snackbars, and loading operations after recreation.

## Testing and verification

Detailed unit-test naming, verification, and test-double conventions are defined in [ADR-0001: Unit Test Conventions](0001-unit-test-conventions.md).

Test behavior at the boundary that owns it:

- model event-to-state and event-to-news behavior;
- mapper branches, derived flags, item types, resources, and stable keys;
- duplicate-action protection and cancellation where applicable;
- navigation initial state, back behavior, and process restoration when they can regress;
- Compose loading, error, empty, content, small-screen, accessibility, and interaction states.

Do not add component tests that only prove a thin adapter delegates to its model. Destroy active retained models in tests.

Run focused presentation tests, compile Android and an iOS target, and visually verify UI changes on an emulator or device when available. Report lifecycle and visual scenarios that were not exercised.

## Consequences

Benefits:

- Business behavior stays testable outside Compose and Decompose adapters.
- UI contracts are stable and ready to render.
- Navigation and retained lifetimes have explicit owners.
- The same rules apply regardless of which coding agent or IDE performs the change.

Costs:

- Meaningful presentation decisions may require explicit models and mapping.
- Lifecycle and one-shot delivery require focused tests beyond pure UI rendering.

These costs are accepted. Pass-through abstractions and ceremonial layers are not required.

## Compliance and migration

New presentation code follows this ADR immediately. `yap-app` has no presentation code yet, so this ADR governs from the first screen built; existing screens are migrated when their presentation area changes materially, and unrelated screens are not rewritten solely for structural consistency.

Any intentional exception must be justified in the pull request description, per the constitution's Governance section, or superseded by a later ADR.
