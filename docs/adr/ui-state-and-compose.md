# UI State and Compose Rendering

- Status: Accepted
- Date: 2026-08-09
- Scope: UI-state mapping and Compose rendering under `apps/mobile/*`

## Context

Compose should receive ready-to-render values without interpreting business outcomes. Repeatable visual facts, one-shot output, and transient visual state have different lifetimes and need explicit ownership.

Component and model responsibilities are defined in [Presentation Components and Models](presentation-components-and-models.md).

## Decision

`UiState` contains immutable ready-to-render values. Use focused item models with stable keys instead of broad domain aggregates.

- Annotate a collection-holding `UiState` with `@Immutable` only when its collections are built once and never mutated.
- Add a `UiStateMapper` interface and `internal Default...UiStateMapper` only when mapping owns presentation decisions or provides a useful test seam.
- A mapper may derive action availability, item types, stable keys, and resource choices.
- A mapper must not own theme colors, dimensions, measurements, focus, scrolling, or animation state.
- Use `News` or `Effect` only for output consumed once, such as navigation or a scrolling command.
- Keep repeatable visual facts in `UiState`.
- When delivery must be one-shot, use a buffered channel exposed as a flow and close it with its model.

## Compose boundary

Composable content accepts explicit immutable UI state and callbacks. It does not fetch, call repositories or use cases, interpret domain outcomes, or coordinate feature lifetimes.

- Keep user-visible strings in Compose resources.
- Resolve presentation-selected resources in Compose.
- Keep colors, dimensions, measurements, focus, scroll, animation progress, and other transient visual state in Compose.
- Reuse `core-design` only for deliberate feature-agnostic primitives with stable APIs; keep feature-specific resources and variants in the feature.
- Give lazy-list items stable keys.
- Key effects with the values defining their lifetime and ensure long-lived effects observe current callbacks.
- Follow supplied design values and verify both light and dark themes for styling changes.

Extract a composable when it is a meaningful visual unit, owns a coherent interaction, or materially clarifies its parent. Do not create generic helpers for one-off expressions.

## Verification

Test meaningful mapper branches, flags, item types, resources, and keys. Verify Compose loading, error, empty, content, small-screen, accessibility, and interaction states when they can regress. Run focused tests, compile Android and iOS, and visually verify UI changes on a device or emulator when available.

## Consequences

Compose remains a renderer and UI contracts are stable and ready to display. Explicit mapping adds types only where presentation decisions justify them.

## Compliance

New and changed UI state and composables follow this ADR. Intentional exceptions must be justified in the pull request or superseded by a later ADR.
