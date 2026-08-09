# Models

`<Slice>Model` owns operations and durable in-memory presentation facts.

- Declare it directly below `Default...Component` in the same file; do not create a separate model file.
- Give it a nested `Factory` that owns every model dependency.
- Pass only `Model.Factory` to `Default...Component`; do not proxy use cases, repositories, or mappers through the component.
- Create it through `instanceKeeper.getOrCreate(modelFactory::create)`.
- Implement `InstanceKeeper.Instance` and call `clear()` from `onDestroy()`.
- Keep it `internal` and use `BaseModel` when `modelScope` or teardown is required.
- Nest immutable `DataState` in the model.
- Update state with `update { it.copy(...) }`; read `.value` only for a required synchronous snapshot.
- Update and observe presentation `Value` on the main thread.
- Set busy flags before launching guarded work.
- Clear stale errors on retry without erasing unrelated state.
- Name use-case dependencies with the `UseCase` suffix.
- Depend on use cases only; never inject repositories into a model.
- Route non-trivial events to named `on...` functions.
- Handle typed domain outcomes exhaustively.
- Emit one-shot presentation output through the owning component's `News`; do not store snackbar
  triggers in `DataState` or `UiState`.
- Close model-owned channels in `onCleared`; `clear()` invokes it before cancelling `modelScope`.

A model owns loading, retry, and screen errors. Compose owns colors, dimensions, focus, scrolling, and animation progress.

## Factory

- Inject dependencies into `Model.Factory` explicitly from the feature container.
- `create()` constructs a fresh model and contains no lookup or service-locator behavior.
- Do not cache a model in the factory; `InstanceKeeper` owns reuse.
- Tests also create models through the same factory; do not instantiate them directly.

## Verification

Test event-to-state and event-to-news behavior, typed outcomes, duplicate-action guards, cancellation, and cleanup. Do not test a component only to prove thin delegation. Follow [Test Structure](../../testing/001-structure.md).
