package app.yap.feature.auth.presentation.selectprovider

import app.yap.core.common.coroutines.CoroutineDispatchers
import app.yap.core.common.presentation.BaseModel
import app.yap.feature.auth.domain.entity.LoginProvider
import app.yap.feature.auth.domain.usecase.ObserveLoginProvidersUseCase
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.operator.map
import com.arkivanov.decompose.value.update
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.instancekeeper.InstanceKeeper
import com.arkivanov.essenty.instancekeeper.getOrCreate
import kotlinx.coroutines.launch

internal class DefaultSelectProviderComponent(
    componentContext: ComponentContext,
    modelFactory: SelectProviderModel.Factory,
    private val output: (SelectProviderComponent.Output) -> Unit,
) : SelectProviderComponent, ComponentContext by componentContext {

    private val model = instanceKeeper.getOrCreate(modelFactory::create)

    override val uiState: Value<SelectProviderComponent.UiState> =
        model.dataState.map(SelectProviderModel.DataState::toUiState)

    init {
        backHandler.register(BackCallback { output(SelectProviderComponent.Output.Dismissed) })
    }

    override fun dispatch(event: SelectProviderComponent.Event) {
        when (event) {
            is SelectProviderComponent.Event.DismissRequested -> output(SelectProviderComponent.Output.Dismissed)
            is SelectProviderComponent.Event.ProviderClicked -> output(
                SelectProviderComponent.Output.ProviderSelected(providerId = event.providerId),
            )
        }
    }

    class Factory(
        private val modelFactory: SelectProviderModel.Factory,
    ) : SelectProviderComponent.Factory {

        override fun create(
            componentContext: ComponentContext,
            output: (SelectProviderComponent.Output) -> Unit,
        ): SelectProviderComponent = DefaultSelectProviderComponent(
            componentContext = componentContext,
            modelFactory = modelFactory,
            output = output,
        )
    }
}

/** Holds the configured provider list verbatim; ordering and filtering happen in the mapper. */
internal class SelectProviderModel(
    coroutineDispatchers: CoroutineDispatchers,
    private val observeLoginProvidersUseCase: ObserveLoginProvidersUseCase,
) : BaseModel(coroutineDispatchers), InstanceKeeper.Instance {

    private val mutableDataState = MutableValue(DataState(providers = emptyList()))

    val dataState: Value<DataState> = mutableDataState

    init {
        modelScope.launch {
            observeLoginProvidersUseCase().collect { providers ->
                mutableDataState.update { state -> state.copy(providers = providers) }
            }
        }
    }

    override fun onDestroy() = clear()

    data class DataState(
        val providers: List<LoginProvider>,
    )

    class Factory(
        private val coroutineDispatchers: CoroutineDispatchers,
        private val observeLoginProvidersUseCase: ObserveLoginProvidersUseCase,
    ) {

        fun create(): SelectProviderModel = SelectProviderModel(
            coroutineDispatchers = coroutineDispatchers,
            observeLoginProvidersUseCase = observeLoginProvidersUseCase,
        )
    }
}
