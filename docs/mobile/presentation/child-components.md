# Child and Host Components

Start with one screen component. A child component is optional and must justify its own boundary.

Consider extracting a child when a section:

- has an independent lifecycle, navigation, or reusable interaction;
- remains cohesive when separated from the parent;
- already makes the parent difficult to read, test, or change safely.

Loading, error state, an operation, or a large visual block alone is not enough. Keep a simple screen and its cohesive states in one component and view model.

Prefer a separate child component for a behavior-rich bottom sheet or dialog. Use `ChildSlot` when its presence and lifecycle are component state. A simple sheet may remain part of the screen component or a local composable.

Keep the complete child slice in one package:

```text
login/
├── LoginComponent.kt         + DefaultLoginComponent.kt
├── LoginViewModel.kt
├── LoginDataState.kt         + LoginUiState.kt + LoginUiStateMapper.kt
├── LoginEvent.kt             + LoginNews.kt + LoginOutput.kt
└── LoginContent.kt
```

The full inventory and its optional files are in [Components](components.md).

- Child content receives ready-to-render state and callbacks only.
- Children never reach into their parent or siblings: a child reports through
  `output: (XOutput) -> Unit` and the parent decides what happens.
- A child that owns a back gesture registers it itself, for example
  `backHandler.register(BackCallback { output(XOutput.Dismissed) })`.
- The parent may order children and combine only genuine screen-level facts.
- Static or simple fragments without an independent boundary remain local composables.

## Host components

Add a host component when one lifecycle owner must coordinate several children, navigation models, or overlays.

- The host owns composition and routing, not copies of child state or business rules.
- A host has no view model, `DataState`, `UiState`, or `Event`. Its files are the contract, the
  default implementation, and its navigation config; the screen-slice file set does not apply to it.
- Derive facts such as "the sheet is open" from the slot itself, never from a mirrored flag.
- Children communicate through their contracts and `Output`; the host decides what each one means.
- Do not add an empty host for hypothetical future screens.
