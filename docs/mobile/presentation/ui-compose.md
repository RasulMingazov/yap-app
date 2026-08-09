# UI and Compose

Presentation follows one direction:

```text
Domain → Model.DataState → UiStateMapper → Value<Component.UiState> → Compose
Domain/model outcome → Component.News → Compose one-shot effect
```

## UI state

- Keep `UiState` immutable and ready to render.
- Prefer focused item models with stable keys over broad domain aggregates.
- Add a mapper whenever the screen owns presentation decisions or needs a useful test seam.
- Put every repeatable value required to render the screen in `UiState`: text resource
  references, icon resource references or stable icon tokens, item order, visibility,
  enabled/loading state, availability, stable keys, and persistent inline error content.
- A mapper derives these values from domain/model state, injected configuration, platform
  capabilities, and presentation resources.
- Keep concrete Compose resource types out of domain and model state. Presentation `UiState`
  may contain presentation resource references or stable UI tokens.
- Nest `UiState` in its owning component contract, for example `ProfileComponent.UiState`.
- Keep repeatable facts in `UiState`; never add a consumable snackbar, navigation, or dialog flag
  solely to trigger a one-shot effect.
- Never put theme values, dimensions, focus, scrolling, or animation state in a mapper.

## News

- Represent one-shot presentation output as the owning component's nested `News`, for example
  `ProfileComponent.News.ShowSnackbar(message)`.
- Use `News` for snackbar presentation, navigation commands, and other output that must be handled
  once. A persistent inline error or banner that must survive resubscription remains in `UiState`.
- Resolve feature copy and resource selection before emitting `News`; a composable must not map a
  domain error or provider identifier to snackbar text.
- Explicit cancellation emits no snackbar news. A disabled provider or recoverable failure emits
  exactly one `ShowSnackbar` news item.
- Do not mirror a `News` item into `UiState` and do not require an `ErrorConsumed` event merely to
  prevent replay.

## Compose

Subscribe to the component's `Value` and collect its `Flow<News>` once at the screen boundary,
then pass focused state and callbacks down.

- Do not fetch, call use cases, interpret domain outcomes, or launch work during composition.
- Keep strings and icons declared in Compose resources, but select their resource references or
  stable tokens in the mapper and carry that selection through `UiState`.
- Handle each `News` item once and invoke the corresponding UI API, such as
  `SnackbarHostState.showSnackbar`; do not write it back into screen state.
- Do not choose copy, icons, item visibility, ordering, availability, or platform behavior with
  product-specific `when` branches in a composable. Render the supplied `UiState` and emit
  user intent.
- Keep only transient visual state such as focus, scrolling, gesture progress, and animation
  progress in Compose.
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

Treat mapper tests as the primary verification surface for presentation decisions.

- Assert the complete observable `UiState` contract: exact text resources, icon mapping, item
  order, visibility, availability, enabled/loading state, and persistent inline-error mapping.
- Assert one-shot output separately as the exact nested `Component.News` subtype and payload,
  including snackbar resource selection and the absence of news on cancellation.
- Cover every meaningful combination of injected configuration, platform capability, and
  model/domain state.
- Prefer a deterministic mapper assertion over a Compose UI test when the behavior is fully
  represented by `UiState`.
- Use Compose UI or screenshot tests for rendering, semantics, interaction wiring, accessibility,
  and light/dark visual states that a mapper test cannot prove.
