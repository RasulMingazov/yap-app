# View Models

`<Slice>ViewModel` owns operations and durable in-memory presentation facts.

A view model belongs to a **screen slice**. A host or orchestrator component — one that only
composes children and owns navigation — has no view model, no `DataState`, and no `UiState`;
adding an empty one to satisfy the template is wrong. `AuthComponent` is the reference: children
and slots only. See [Child Components](child-components.md).

- Declare it `internal` in its own file `<Slice>ViewModel.kt` and extend `BaseViewModel`
  (`core-decompose`).
- Do not declare `InstanceKeeper.Instance` and do not override `onDestroy()`: `BaseViewModel` is
  the retained instance and its `onDestroy()` is `final`. Release resources in `onCleared()`, for
  example `newsChannel.close()`.
- Hold state as `private val dataState = MutableValue(<Slice>DataState())`. Every `DataState` field
  has a default, so the initial state needs no arguments.
- Derive `val uiState: Value<XUiState> = dataState.map { it.toUiState() }`.
- Update with `update { it.copy(...) }`; read `.value` only for a required synchronous snapshot.
- Start observation from `init` on `viewModelScope`; update and observe `Value` on the main thread.
- Route events through `dispatch` with an exhaustive `when` over named `on...` functions.
- Set busy flags before launching guarded work and guard duplicate actions on the same flag.
- Handle typed domain outcomes exhaustively; clear stale errors on retry without erasing unrelated
  state.
- Emit one-shot presentation output through `News`; do not store snackbar triggers in `DataState`
  or `UiState`. Report to the parent through `Output`.
- Depend on use cases only, named with the `UseCase` suffix; never inject repositories.

A view model owns loading, retry, and screen errors. Compose owns colors, dimensions, focus,
scrolling, and animation progress.

```kotlin
internal class LoginViewModel(
    private val logInUseCase: LogInUseCase,
    private val output: (LoginOutput) -> Unit,
    coroutineDispatchers: CoroutineDispatchers,
) : BaseViewModel(coroutineDispatchers) {

    private val dataState = MutableValue(LoginDataState())
    val uiState: Value<LoginUiState> = dataState.map { it.toUiState() }

    private val newsChannel = Channel<LoginNews>(Channel.BUFFERED)
    val news: Flow<LoginNews> = newsChannel.receiveAsFlow()

    fun dispatch(event: LoginEvent) {
        when (event) {
            is LoginEvent.LoginClicked -> onLoginClicked()
            is LoginEvent.ProviderSelected -> onProviderSelected(providerId = event.providerId)
        }
    }

    override fun onCleared() {
        newsChannel.close()
    }

    class Factory(
        private val coroutineDispatchers: CoroutineDispatchers,
        private val logInUseCase: LogInUseCase,
    ) {

        operator fun invoke(output: (LoginOutput) -> Unit): LoginViewModel = LoginViewModel(/* ... */)
    }
}
```

## Factory

- Give every view model a nested `Factory` with `operator fun invoke(output: (XOutput) -> Unit)`.
- The factory owns every view-model dependency, injected explicitly from the feature container.
- `invoke` constructs a fresh view model and contains no lookup or service-locator behavior.
- Do not cache the view model in the factory; `InstanceKeeper` owns reuse.
- Tests create view models through the same factory; do not instantiate them directly.

## Verification

Test event-to-state and event-to-news behavior, typed outcomes, duplicate-action guards,
cancellation, and cleanup. Do not test a component only to prove thin delegation. Follow
[Test Structure](../../testing/001-structure.md).
