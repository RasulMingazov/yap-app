# Components

`Component` is the public lifecycle-aware boundary between shared logic and UI.

- Expose state as read-only `Value<UiState>`, never `StateFlow` or `MutableValue`.
- Assign each `Value` once; do not recreate it from a property getter.
- Expose `Flow<News>`, `Output`, child contracts, and `Factory` only when needed.
- Name events after intent: `SendClicked`, `TextChanged`, `ErrorDismissed`.
- Keep the public surface minimal and free from broad domain aggregates.

## Slice layout

One top-level declaration per file, named `<Slice><Role>.kt`. Do not nest `UiState`, `Event`,
`News`, or `Output` inside the component; nest only sub-shapes of a state, such as
`LoginUiState.Button`.

```text
login/
├── LoginComponent.kt         contract              required
├── DefaultLoginComponent.kt  wiring only           required
├── LoginViewModel.kt         behavior              required
├── LoginDataState.kt         domain-shaped state   required
├── LoginUiState.kt           render-ready state    required
├── LoginUiStateMapper.kt     DataState -> UiState  required
├── LoginEvent.kt             UI intent             required
├── LoginNews.kt              one-shot effects      only when the screen emits them
├── LoginNewsMapper.kt        domain reason -> News only when copy selection is non-trivial
└── LoginOutput.kt            messages to parent    only when the parent must react
```

This file set describes a **screen slice**. A host or orchestrator component owns none of it:
`AuthComponent` is just a contract, a default implementation, and `AuthSlotConfig.kt`, with no view
model, `DataState`, `UiState`, or `Event`. Navigation configurations live in `<Owner>SlotConfig.kt`
(see [Navigation](navigation.md) and [Child Components](child-components.md)).

## Canonical slice

```kotlin
interface LoginComponent {

    val news: Flow<LoginNews>
    val uiState: Value<LoginUiState>

    fun dispatch(event: LoginEvent)

    interface Factory {

        operator fun invoke(
            componentContext: ComponentContext,
            output: (LoginOutput) -> Unit,
        ): LoginComponent
    }
}

sealed interface LoginEvent {

    data object LoginClicked : LoginEvent

    data class ProviderSelected(val providerId: LoginProviderId) : LoginEvent
}

sealed interface LoginNews {

    data class ShowSnackbar(val formatArgs: List<String>, val message: StringResource) : LoginNews
}

sealed interface LoginOutput {

    data object OpenProviderSelection : LoginOutput
}

internal data class LoginDataState(
    val isLoading: Boolean = false,
    val providers: List<LoginProvider> = emptyList(),
)

internal class DefaultLoginComponent(
    private val output: (LoginOutput) -> Unit,
    componentContext: ComponentContext,
    viewModelFactory: LoginViewModel.Factory,
) : LoginComponent, ComponentContext by componentContext {

    private val model = instanceKeeper.getOrCreate { viewModelFactory.invoke(output) }

    override val news: Flow<LoginNews> = model.news
    override val uiState: Value<LoginUiState> = model.uiState

    override fun dispatch(event: LoginEvent) = model.dispatch(event)

    class Factory(
        private val viewModelFactory: LoginViewModel.Factory,
    ) : LoginComponent.Factory {

        override fun invoke(
            componentContext: ComponentContext,
            output: (LoginOutput) -> Unit,
        ): LoginComponent = DefaultLoginComponent(/* ... */)
    }
}
```

`UiState`, `UiStateMapper`, and `ViewModel` are in [View models](view-models.md) and
[UI and Compose](ui-compose.md).

## Default implementation

`Default...Component` is wiring and nothing else.

- Delegate `ComponentContext`, create the view model with
  `instanceKeeper.getOrCreate { viewModelFactory.invoke(output) }`, re-expose its `uiState` and
  `news`, and forward `dispatch` as a one-line expression body.
- Keep business rules in the view model and mapping in the mapper.
- Do not proxy view-model dependencies through the component constructor.
- Reference children through their interfaces and drive them through events.
- Register `backHandler` here when the screen owns a back gesture.

## Output

A child never navigates and never reaches for its parent. It reports through
`output: (XOutput) -> Unit`, threaded identically at every hop:
`Component.Factory` → `Default...Component` → `ViewModel.Factory` → `ViewModel`.

- The parent decides what an `Output` means and sequences the consequences. In
  `DefaultAuthComponent`, `SelectProviderOutput.ProviderSelected` dismisses the slot first, then
  dispatches `LoginEvent.ProviderSelected`.
- Use `Output` for messages the parent must act on and `News` for effects the screen's own UI
  performs.

## Visibility

Public: `Component`, `UiState`, `Event`, `News`, `Output`.
Internal: `Default...Component`, `ViewModel`, `DataState`, `UiStateMapper`, `SlotConfig`.

`Value` is the component state contract. Internal view-model mechanics may change without changing
that boundary; `StateFlow` never leaks through `Component`.
