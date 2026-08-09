# Navigation, Child Components, and Retention

- Status: Accepted
- Date: 2026-08-09
- Scope: Decompose navigation and presentation lifetime under `apps/mobile/*`

## Context

Inactive destinations can start work eagerly, child components can become coupled through implementation details, and restoration can duplicate requests or effects. Navigation and retained lifetime therefore need one explicit owner.

## Decision

Use `ChildStack` or another appropriate Decompose model for mutually exclusive destinations. Inactive destinations must not create components, models, collectors, subscriptions, or data loads.

Decompose is not yet declared in `gradle/libs.versions.toml`. Add it only when the first feature needs real navigation or retained lifecycle; do not add it to the scaffold speculatively.

Navigation ownership is hierarchical:

- the root selects bootstrap, unauthenticated, or authenticated state;
- an authenticated navigator owns product navigation once a second authenticated feature requires that composition level;
- each product feature owns its own nested navigation and children.

Today `apps/mobile/app-root` directly owns the root state and composes `feature-auth`. Add an authenticated navigator only when a second authenticated feature creates that ownership boundary.

The parent coordinates children through shared domain state or a small explicit component contract. Children never depend on sibling internals. The root routes events without absorbing child state or implementation details.

## Splitting a screen

Split by independent state and interaction ownership, not visual size.

- Keep `FeatureComponent` and `FeatureContent` responsible for screen-level composition, shared scaffold, navigation hand-off, and child coordination.
- Give a section its own component, default component, model, mapper, and content when it owns an independent state stream, loading or error lifecycle, operation, news, or reusable interaction boundary.
- Keep the complete child slice in one focused package such as `presentation/signin`.
- Child content accepts only its ready-to-render state and callbacks; it does not reach into root or siblings.
- Root content may order children and combine facts only for genuine screen-level states.
- Keep shared screen visuals in small root presentation files. Promote a primitive to `core-design` only when it is feature-agnostic and intentionally reused.
- For a static fragment with no independent state or lifecycle, extract only a local composable.

```text
presentation/
├── AuthComponent.kt
├── AuthContent.kt
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
- Restore durable data from repositories or storage instead of serializing large histories into presentation state.
- Derive an already-known initial destination synchronously so restoration does not flash a bootstrap or empty child.
- Make restoration idempotent and prevent duplicate requests, collectors, navigation events, dialogs, snackbars, and loading work.
- Use stable unique keys for retained instances and child contexts.
- Destroy active retained models in tests.

Container creation for cold branches and lifetime ownership follows [Manual Dependency Injection Containers](manual-dependency-injection.md).

## Verification

Test initial destinations, back behavior, lazy child creation, configuration retention, and process restoration when they can regress. Report lifecycle scenarios not executed.

## Consequences

Inactive branches stay cold and lifetime ownership is explicit. Navigation and restoration require focused tests beyond pure rendering.

## Compliance

New navigation follows this ADR. Introduce new navigation levels only when current product structure requires them. Intentional exceptions must be justified in the pull request or superseded by a later ADR.
