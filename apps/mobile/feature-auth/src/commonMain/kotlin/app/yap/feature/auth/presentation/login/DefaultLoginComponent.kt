package app.yap.feature.auth.presentation.login

import app.yap.core.common.coroutines.CoroutineDispatchers
import app.yap.core.common.presentation.BaseModel
import app.yap.feature.auth.domain.entity.LoginFailure
import app.yap.feature.auth.domain.entity.LoginOutcome
import app.yap.feature.auth.domain.entity.LoginProvider
import app.yap.feature.auth.domain.entity.LoginProviderId
import app.yap.feature.auth.domain.usecase.LogInUseCase
import app.yap.feature.auth.domain.usecase.ObserveLoginProvidersUseCase
import app.yap.feature.auth.generated.resources.Res
import app.yap.feature.auth.generated.resources.login_error_connectivity
import app.yap.feature.auth.generated.resources.login_error_provider
import app.yap.feature.auth.generated.resources.login_provider_coming_soon
import app.yap.feature.auth.generated.resources.login_provider_unavailable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.operator.map
import com.arkivanov.decompose.value.update
import com.arkivanov.essenty.instancekeeper.InstanceKeeper
import com.arkivanov.essenty.instancekeeper.getOrCreate
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

internal class DefaultLoginComponent(
    componentContext: ComponentContext,
    modelFactory: LoginModel.Factory,
    private val output: (LoginComponent.Output) -> Unit,
) : LoginComponent, ComponentContext by componentContext {

    private val model = instanceKeeper.getOrCreate(modelFactory::create)

    override val news: Flow<LoginComponent.News> = model.news
    override val uiState: Value<LoginComponent.UiState> = model.dataState.map(LoginModel.DataState::toUiState)

    override fun dispatch(event: LoginComponent.Event) {
        when (event) {
            is LoginComponent.Event.LoginClicked -> onLoginClicked()
            is LoginComponent.Event.ProviderSelected -> model.onProviderSelected(providerId = event.providerId)
        }
    }

    private fun onLoginClicked() {
        if (!model.canOpenProviderSelection()) return

        output(LoginComponent.Output.OpenProviderSelection)
    }

    class Factory(
        private val modelFactory: LoginModel.Factory,
    ) : LoginComponent.Factory {

        override fun create(
            componentContext: ComponentContext,
            output: (LoginComponent.Output) -> Unit,
        ): LoginComponent = DefaultLoginComponent(
            componentContext = componentContext,
            modelFactory = modelFactory,
            output = output,
        )
    }
}

/**
 * Owns the login attempt: the loading flag, the duplicate-attempt guard, and the one-shot messages
 * for a disabled provider and for a recoverable failure. Cancellation is silent (R-029, R-090,
 * AC-023, AC-042).
 */
internal class LoginModel(
    coroutineDispatchers: CoroutineDispatchers,
    private val logInUseCase: LogInUseCase,
    private val observeLoginProvidersUseCase: ObserveLoginProvidersUseCase,
) : BaseModel(coroutineDispatchers), InstanceKeeper.Instance {

    private val newsChannel = Channel<LoginComponent.News>(Channel.BUFFERED)
    private val mutableDataState = MutableValue(DataState(isLoading = false, providers = emptyList()))

    val dataState: Value<DataState> = mutableDataState
    val news: Flow<LoginComponent.News> = newsChannel.receiveAsFlow()

    init {
        modelScope.launch {
            observeLoginProvidersUseCase().collect { providers ->
                mutableDataState.update { state -> state.copy(providers = providers) }
            }
        }
    }

    fun canOpenProviderSelection(): Boolean = !mutableDataState.value.isLoading

    fun onProviderSelected(providerId: LoginProviderId) {
        if (mutableDataState.value.isLoading) return
        val provider = mutableDataState.value.providers.firstOrNull { it.id == providerId } ?: return

        if (provider.isEnabled) {
            startAttempt(provider = provider)
        } else {
            newsChannel.trySend(
                LoginComponent.News.ShowSnackbar(
                    formatArgs = listOf(provider.displayName),
                    message = Res.string.login_provider_coming_soon,
                ),
            )
        }
    }

    override fun onDestroy() = clear()

    override fun onCleared() {
        newsChannel.close()
    }

    private fun startAttempt(provider: LoginProvider) {
        mutableDataState.update { state -> state.copy(isLoading = true) }
        modelScope.launch { runAttempt(provider = provider) }
    }

    private suspend fun runAttempt(provider: LoginProvider) {
        val outcome = logInUseCase(providerId = provider.id)
        mutableDataState.update { state -> state.copy(isLoading = false) }

        when (outcome) {
            is LoginOutcome.Cancelled -> Unit
            is LoginOutcome.Failure -> newsChannel.trySend(
                failureNews(displayName = provider.displayName, reason = outcome.reason),
            )

            is LoginOutcome.Success -> Unit
        }
    }

    private fun failureNews(displayName: String, reason: LoginFailure): LoginComponent.News.ShowSnackbar =
        when (reason) {
            LoginFailure.Configuration -> LoginComponent.News.ShowSnackbar(
                formatArgs = listOf(displayName),
                message = Res.string.login_provider_unavailable,
            )

            LoginFailure.Connectivity -> LoginComponent.News.ShowSnackbar(
                formatArgs = emptyList(),
                message = Res.string.login_error_connectivity,
            )

            LoginFailure.Provider -> LoginComponent.News.ShowSnackbar(
                formatArgs = listOf(displayName),
                message = Res.string.login_error_provider,
            )
        }

    data class DataState(
        val isLoading: Boolean,
        val providers: List<LoginProvider>,
    )

    class Factory(
        private val coroutineDispatchers: CoroutineDispatchers,
        private val logInUseCase: LogInUseCase,
        private val observeLoginProvidersUseCase: ObserveLoginProvidersUseCase,
    ) {

        fun create(): LoginModel = LoginModel(
            coroutineDispatchers = coroutineDispatchers,
            logInUseCase = logInUseCase,
            observeLoginProvidersUseCase = observeLoginProvidersUseCase,
        )
    }
}
