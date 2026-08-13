# View Models

`<Slice>ViewModel` is the boundary between shared logic and Compose. It owns screen state, operations, navigation, and one-shot output.

```kotlin
internal class ProfileViewModel(
    private val navigator: Navigator,
    private val observeProfileUseCase: ObserveProfileUseCase,
) : BaseViewModel() {

    private val dataState = MutableStateFlow(DataState())
    private val newsChannel = Channel<News>(Channel.BUFFERED)

    val uiState: StateFlow<UiState> = dataState.mapState(ProfileUiStateMapper::map)
    val news: Flow<News> = newsChannel.receiveAsFlow()

    fun onEvent(event: Event) = when (event) {
        is Event.RetryClicked -> onRetryClicked()
        is Event.SettingsClicked -> navigator.navigate(SettingsNavKey.Overview)
    }

    override fun onCleared() {
        super.onCleared()
        newsChannel.close()
    }

    data class DataState(val isLoading: Boolean = false, val user: User? = null)

    data class UiState(val error: Error?, val isLoading: Boolean) {

        data class Error(val message: StringResource)
    }

    sealed interface News {

        data class ShowSnackbar(val message: StringResource) : News
    }

    sealed interface Event {

        data object RetryClicked : Event

        data object SettingsClicked : Event
    }
}
```

## Rules

- Nest `UiState`, `News`, `Event`, and their payloads in the owning view model; never create top-level `ProfileUiState` types. Name events after intent: `SendClicked`, `ErrorDismissed`.
- Extend `BaseViewModel`, keep it `internal`, and launch work in `viewModelScope`.
- Nest immutable `DataState` for internal facts and map it to `UiState` with `BaseViewModel.mapState`.
- Set busy flags before launching guarded work and clear stale errors on retry without erasing unrelated state.
- Depend on use cases and `Navigator` only; never inject a repository or data source.
- Navigate through `Navigator` with keys from the target feature's `api`. `News` carries snackbars and dialogs, never navigation.
- Do not switch dispatchers. `viewModelScope` is enough and `runViewModelTest` makes it deterministic; inject `CoroutineDispatchers` only where a blocking API or heavy computation requires it.
- Route non-trivial events to named `on...` functions and handle typed domain outcomes exhaustively.
- Release owned resources in `onCleared()` and never retain an Activity or `UIViewController`.
- Restore durable data from repositories instead of serializing histories, and make restoration idempotent: do not replay navigation, dialogs, or snackbars.

A view model owns loading, retry, screen errors, and navigation. Compose owns colors, dimensions, focus, scrolling, and animation.

Start with one view model per screen. Extract a second one only when a section has an independent lifecycle, its own destination, or a reusable interaction — loading, error state, a single operation, or a large visual block is not enough.

## Verification

Test event-to-state, event-to-news, and event-to-navigation behavior, typed outcomes, duplicate-action guards, cancellation, and cleanup. Drive tests through `runViewModelTest` from `core-test`. Follow [Test Structure](../../testing/001-structure.md).
