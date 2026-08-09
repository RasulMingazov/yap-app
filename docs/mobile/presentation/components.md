# Components

## Contract

`Component` is the public lifecycle-aware boundary between shared logic and UI.

- Expose state as read-only `Value<UiState>`, never `StateFlow` or `MutableValue`.
- Assign each `Value` once; do not recreate it from a property getter.
- Expose one-shot `Flow<News>`, `Event`, child contracts, and `Factory` only when needed.
- Name events after intent: `SendClicked`, `TextChanged`, `ErrorDismissed`.
- Nest `UiState`, `News`, `Event`, and their payloads in the component that owns them. Refer to
  them as `ProfileComponent.UiState`, `ProfileComponent.News`, and `ProfileComponent.Event`; do
  not create top-level `ProfileUiState`, `ProfileNews`, or `ProfileEvent` types.
- Keep the public surface minimal and free from broad domain aggregates.

## Default implementation

Every component has an `internal Default...Component`.

- Delegate `ComponentContext`, own the model, expose state, and forward events.
- Keep business rules in the model and mapping in the mapper.
- Accept `Model.Factory` and create the model through `instanceKeeper.getOrCreate(modelFactory::create)`.
- Do not proxy model dependencies through the component constructor.
- Reference children through their interfaces and drive them through events.
- A component factory creates a new component; `Model.Factory` creates a fresh model only when `InstanceKeeper` has none.

```kotlin
interface ProfileComponent {

    val uiState: Value<UiState>
    val news: Flow<News>

    fun onEvent(event: Event)

    data class UiState(
        val error: Error?,
        val isLoading: Boolean,
    ) {

        data class Error(
            val message: StringResource,
        )
    }

    sealed interface News {

        data class ShowSnackbar(
            val message: StringResource,
        ) : News
    }

    sealed interface Event {

        data object RetryClicked : Event
    }
}

internal class DefaultProfileComponent(
    componentContext: ComponentContext,
    modelFactory: ProfileModel.Factory,
) : ProfileComponent, ComponentContext by componentContext {

    private val model = instanceKeeper.getOrCreate(modelFactory::create)
}

internal class ProfileModel(
    private val observeProfileUseCase: ObserveProfileUseCase,
) :
    BaseModel(/* ... */),
    InstanceKeeper.Instance {

    override fun onDestroy() = clear()

    class Factory(
        private val observeProfileUseCase: ObserveProfileUseCase,
    ) {

        fun create(): ProfileModel = ProfileModel(
            observeProfileUseCase = observeProfileUseCase,
        )
    }
}
```

Declare the model below the default component in the same file. Detailed model rules are in [Models](models.md).

`Value` is the component state contract from the first implemented feature. Internal
model mechanics may change without changing that boundary; `StateFlow` never leaks
through `Component`.
