# Retention and Restoration

- Create every view model through `instanceKeeper.getOrCreate { viewModelFactory.invoke(output) }`; do not call its constructor directly from a component.
- `BaseViewModel` is already the `InstanceKeeper.Instance`; do not declare it again and release resources in `onCleared()`.
- Serialize navigation state: make configurations `@Serializable sealed interface` and pass a real `serializer` to `childSlot` and `childStack`, so a restored process reopens what it was showing.
- Pass `serializer = null` only for state that is genuinely disposable, and say why in a KDoc.
- Use `StateKeeper` only for small serializable state that must survive process death.
- Restore durable data from repositories instead of serializing large histories.
- Derive a known initial destination synchronously to avoid flashing an empty child.
- Make restoration idempotent: do not duplicate work or replay navigation, dialogs, snackbars, or other news.
- Use stable keys unique within the parent component.
- Destroy active retained view models in tests.

Inactive navigation branches must not create components, view models, subscriptions, or data loads. Container creation follows [Dependency Injection](../dependency-injection.md).

## Corner cases

- A factory may outlive its child only when each invocation creates fresh screen-scoped state.
- A retained view model must not capture an Activity, UIViewController, or another host-scoped object.
- A rollback to durable state must not restore a consumed one-shot event.
- Multiple retained children of the same type still need distinct keys.

## Verification

Test initial destinations, back behavior, lazy child creation, configuration retention, and process restoration when they can regress. Report lifecycle scenarios not executed.
