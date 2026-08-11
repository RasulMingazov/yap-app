package app.yap.feature.auth.presentation.selectprovider

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.instancekeeper.getOrCreate

internal class DefaultSelectProviderComponent(
    private val output: (SelectProviderOutput) -> Unit,
    componentContext: ComponentContext,
    viewModelFactory: SelectProviderViewModel.Factory,
) : SelectProviderComponent, ComponentContext by componentContext {

    private val model = instanceKeeper.getOrCreate { viewModelFactory.invoke(output) }

    override val uiState: Value<SelectProviderUiState> = model.uiState

    init {
        backHandler.register(BackCallback { output(SelectProviderOutput.Dismissed) })
    }

    override fun dispatch(event: SelectProviderEvent) = model.dispatch(event)

    class Factory(
        private val viewModelFactory: SelectProviderViewModel.Factory,
    ) : SelectProviderComponent.Factory {

        override fun invoke(
            componentContext: ComponentContext,
            output: (SelectProviderOutput) -> Unit,
        ): SelectProviderComponent = DefaultSelectProviderComponent(
            componentContext = componentContext,
            output = output,
            viewModelFactory = viewModelFactory,
        )
    }
}
