# Navigation

Navigation belongs to presentation and has one explicit owner. That owner may be a feature, host, or `app-root` composition component.

## Ownership

- The root owns application-level branches such as bootstrap and session state.
- A host owns navigation that spans several product features.
- Each feature owns its internal destinations and children.
- Use the narrowest owner that can legitimately compose all destinations at that level.
- Parents coordinate children through shared domain state or a small component contract.
- Children never depend on sibling internals, and the root does not absorb child state.

Introduce a navigation level only when it has real destinations to coordinate. Inactive destinations must not create components, models, collectors, subscriptions, or data loads.

## Decompose

Use `ChildStack` or another appropriate Decompose model for mutually exclusive
destinations. Add a navigation model only when the feature has real destinations.

Child structure follows [Child Components](child-components.md). Retention and restoration follow [Retention](retention.md).
