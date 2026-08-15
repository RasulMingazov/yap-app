package app.yap.feature.auth.presentation.selectprovider

import androidx.lifecycle.viewModelScope
import app.yap.core.common.navigation.Navigator
import app.yap.core.common.presentation.BaseViewModel
import app.yap.feature.auth.api.entity.AuthProvider
import app.yap.feature.auth.api.usecase.ObserveAuthProvidersUseCase
import app.yap.feature.auth.presentation.common.AuthProviderUi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class SelectAuthProviderViewModel(
    private val navigator: Navigator,
    observeAuthProvidersUseCase: ObserveAuthProvidersUseCase,
    uiStateMapper: SelectAuthProviderUiStateMapper,
) : BaseViewModel() {

    private val dataState = MutableStateFlow(DataState())

    val uiState: StateFlow<UiState> = dataState.mapState(uiStateMapper::invoke)

    init {
        viewModelScope.launch {
            observeAuthProvidersUseCase().collect { providers ->
                dataState.update { state -> state.copy(providers = providers) }
            }
        }
    }

    fun onEvent(event: Event) = when (event) {
        is Event.ProviderChosen -> navigator.back()
    }

    data class DataState(val providers: List<AuthProvider> = emptyList())

    data class UiState(val providers: List<Provider>) {

        data class Provider(
            val provider: AuthProvider,
            val ui: AuthProviderUi,
        )
    }

    sealed interface Event {

        data object ProviderChosen : Event
    }
}
