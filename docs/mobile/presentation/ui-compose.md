# UI and Compose

Presentation follows one direction:

```text
Domain → Model.DataState → UiStateMapper → Value<UiState> → Compose
```

## UI state

- Keep `UiState` immutable and ready to render.
- Prefer focused item models with stable keys over broad domain aggregates.
- Add a mapper only when it owns presentation decisions or provides a useful test seam.
- A mapper may derive availability, item types, stable keys, and resource choices.
- Keep repeatable facts in `UiState`; use `News` only for one-shot output.
- Never put theme values, dimensions, focus, scrolling, or animation state in a mapper.

## Compose

Subscribe to the component's `Value` once at the screen boundary, then pass focused state and callbacks down.

- Do not fetch, call use cases, interpret domain outcomes, or launch work during composition.
- Keep strings in Compose resources and transient visual state in Compose.
- Keep collections immutable and give lazy items stable keys.
- Key effects by their lifetime and make long-lived effects observe current callbacks.
- Extract meaningful visual units, not generic helpers for one-off expressions.

## Recomposition

- Pass each child only the stable state it needs.
- Do not rebuild unchanged collections during mapping.
- Do not add `remember`, `derivedStateOf`, stability annotations, or custom equality speculatively.
- Prefer correct, simple data flow; optimize measured or clearly hot paths.

## Core design

Move a feature-agnostic composable such as a shared snackbar to `core-design` only when it has a stable API and a real consumer. Its queue, messages, actions, models, and feature resources remain in the feature.

## Verification

Test meaningful mapper branches and verify loading, error, empty, content, accessibility, interaction, light, and dark states when they can regress.
