# UI and Compose

Compose API usage, adaptive layout, and theming follow the official Compose skills. These rules
cover only what this project decides: what belongs in state versus in a composable.

```text
Domain → ViewModel.DataState → UiStateMapper → StateFlow<ViewModel.UiState> → Compose
Domain/view-model outcome → ViewModel.News → Compose one-shot effect
```

## UI state

- Put every repeatable value required to render the screen in `UiState`: resource references or
  stable tokens, item order, visibility, availability, enabled/loading state, stable keys, and
  persistent inline errors.
- A mapper derives those from domain state, injected configuration, platform capabilities, and
  presentation resources. It never touches theme values, dimensions, focus, scrolling, or animation.
- Keep Compose resource types out of domain and `DataState`; `UiState` may carry resource references.
- Never add a consumable snackbar, navigation, or dialog flag to `UiState`.

## News

- `News` is one-shot output: snackbars and dialogs. Navigation goes through `Navigator`.
- A persistent inline error or banner that must survive resubscription stays in `UiState`.
- Resolve copy and resource selection before emitting; a composable must not map a domain error to
  snackbar text.
- Explicit cancellation emits no news. A recoverable failure emits exactly one `ShowSnackbar`.
- Do not mirror `News` into `UiState` or add an `ErrorConsumed` event to prevent replay.

## Compose

Collect `UiState` with `collectAsStateWithLifecycle()` and `News` once at the screen boundary. Pass
focused state and callbacks down; a child receives state and lambdas, never the view model.

- Do not fetch, call use cases, interpret domain outcomes, or launch work during composition.
- Do not choose copy, icons, visibility, ordering, or availability with product-specific `when`
  branches. Render `UiState` and emit intent.
- Keep only transient visual state — focus, scrolling, gesture and animation progress — in Compose.
- Move a composable to `core-design` only when it has a stable API and a real consumer; its queue,
  messages, actions, and feature resources stay in the feature.

## Verification

Mapper tests are the primary verification surface: assert the complete observable `UiState` and,
separately, the exact `News` subtype and payload, across every meaningful combination of injected
configuration, platform capability, and domain state. Use Compose UI or screenshot tests only for
rendering, semantics, accessibility, and light/dark states a mapper test cannot prove.
