package app.yap.feature.auth.presentation.selectprovider

import app.yap.core.common.coroutines.CoroutineDispatchers
import app.yap.core.decompose.presentation.BaseViewModel
import app.yap.feature.auth.domain.usecase.ObserveLoginProvidersUseCase
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.operator.map
import com.arkivanov.decompose.value.update
import kotlinx.coroutines.launch

/** Holds the configured provider list verbatim; ordering and filtering happen in the mapper. */
internal class SelectProviderViewModel(
    private val observeLoginProvidersUseCase: ObserveLoginProvidersUseCase,
    private val output: (SelectProviderOutput) -> Unit,
    coroutineDispatchers: CoroutineDispatchers,
) : BaseViewModel(coroutineDispatchers) {

    private val dataState = MutableValue(SelectProviderDataState())
    val uiState: Value<SelectProviderUiState> = dataState.map { it.toUiState() }

    init {
        viewModelScope.launch {
            observeLoginProvidersUseCase().collect { providers ->
                dataState.update { state -> state.copy(providers = providers) }
            }
        }
    }

    fun dispatch(event: SelectProviderEvent) {
        when (event) {
            is SelectProviderEvent.DismissRequested -> output(SelectProviderOutput.Dismissed)
            is SelectProviderEvent.ProviderClicked -> output(
                SelectProviderOutput.ProviderSelected(
                    providerId = event.providerId
                ),
            )
        }
    }

    class Factory(
        private val coroutineDispatchers: CoroutineDispatchers,
        private val observeLoginProvidersUseCase: ObserveLoginProvidersUseCase,
    ) {

        operator fun invoke(output: (SelectProviderOutput) -> Unit): SelectProviderViewModel =
            SelectProviderViewModel(
                coroutineDispatchers = coroutineDispatchers,
                observeLoginProvidersUseCase = observeLoginProvidersUseCase,
                output = output,
            )
    }
}
