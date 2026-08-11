package app.yap.feature.auth.presentation.login

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.instancekeeper.getOrCreate
import kotlinx.coroutines.flow.Flow

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
        ): LoginComponent = DefaultLoginComponent(
            componentContext = componentContext,
            output = output,
            viewModelFactory = viewModelFactory,
        )
    }
}
