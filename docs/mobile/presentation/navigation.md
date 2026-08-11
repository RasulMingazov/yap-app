# Navigation

Navigation belongs to presentation and has one explicit owner. That owner may be a feature, host, or `app-root` composition component.

## Ownership

- The root owns application-level branches such as bootstrap and session state.
- A host owns navigation that spans several product features.
- Each feature owns its internal destinations and children.
- Use the narrowest owner that can legitimately compose all destinations at that level.
- Parents coordinate children through shared domain state or a small component contract.
- Children never depend on sibling internals, and the root does not absorb child state.

Introduce a navigation level only when it has real destinations to coordinate. Inactive destinations must not create components, view models, collectors, subscriptions, or data loads.

## Decompose

Use `ChildStack` or another appropriate Decompose model for mutually exclusive
destinations. Add a navigation model only when the feature has real destinations.

- Declare configurations in their own `<Owner>SlotConfig.kt` or `<Owner>StackConfig.kt` as a
  `@Serializable internal sealed interface` with one `@Serializable data object` or `data class`
  per destination.
- Keep child keys as `private const val` in the component's `companion object`, not at file level.
- A navigation owner may hold no state at all: derive whether a child is present from the slot
  itself instead of mirroring it into a flag. `AuthComponent` exposes only `login` and
  `selectProvider`.

Child structure follows [Child Components](child-components.md). Retention and restoration follow [Retention](retention.md).
