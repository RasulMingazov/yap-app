package app.yap.feature.auth.presentation.login

import app.yap.core.common.coroutines.CoroutineDispatchers
import app.yap.core.decompose.presentation.BaseViewModel
import app.yap.feature.auth.domain.entity.LoginOutcome
import app.yap.feature.auth.domain.entity.LoginProvider
import app.yap.feature.auth.domain.entity.LoginProviderId
import app.yap.feature.auth.domain.usecase.LogInUseCase
import app.yap.feature.auth.domain.usecase.ObserveLoginProvidersUseCase
import app.yap.feature.auth.generated.resources.Res
import app.yap.feature.auth.generated.resources.login_provider_coming_soon
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.operator.map
import com.arkivanov.decompose.value.update
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * Owns the login attempt: the loading flag, the duplicate-attempt guard, and the one-shot messages
 * for a disabled provider and for a recoverable failure. Cancellation is silent (R-029, R-090,
 * AC-023, AC-042).
 */
internal class LoginViewModel(
    private val logInUseCase: LogInUseCase,
    private val observeLoginProvidersUseCase: ObserveLoginProvidersUseCase,
    private val output: (LoginOutput) -> Unit,
    coroutineDispatchers: CoroutineDispatchers,
) : BaseViewModel(coroutineDispatchers) {

    private val dataState = MutableValue(LoginDataState())
    val uiState: Value<LoginUiState> = dataState.map { it.toUiState() }

    private val newsChannel = Channel<LoginNews>(Channel.BUFFERED)
    val news: Flow<LoginNews> = newsChannel.receiveAsFlow()

    init {
        viewModelScope.launch {
            observeLoginProvidersUseCase().collect { providers ->
                dataState.update { state -> state.copy(providers = providers) }
            }
        }
    }

    fun dispatch(event: LoginEvent) {
        when (event) {
            is LoginEvent.LoginClicked -> onLoginClicked()
            is LoginEvent.ProviderSelected -> onProviderSelected(providerId = event.providerId)
        }
    }

    private fun onLoginClicked() {
        if (dataState.value.isLoading) return

        output(LoginOutput.OpenProviderSelection)
    }

    private fun onProviderSelected(providerId: LoginProviderId) {
        if (dataState.value.isLoading) return
        val provider = dataState.value.providers.firstOrNull { it.id == providerId } ?: return

        if (!provider.isEnabled) {
            newsChannel.trySend(
                LoginNews.ShowSnackbar(
                    formatArgs = listOf(provider.displayName),
                    message = Res.string.login_provider_coming_soon,
                ),
            )
            return
        }

        dataState.update { state -> state.copy(isLoading = true) }

        viewModelScope.launch { logIn(provider = provider) }
    }

    private suspend fun logIn(provider: LoginProvider) {
        val outcome = logInUseCase(providerId = provider.id)
        dataState.update { state -> state.copy(isLoading = false) }

        when (outcome) {
            is LoginOutcome.Cancelled -> Unit
            is LoginOutcome.Failure -> newsChannel.trySend(
                outcome.reason.toNews(displayName = provider.displayName),
            )

            is LoginOutcome.Success -> Unit
        }
    }

    override fun onCleared() {
        newsChannel.close()
    }

    class Factory(
        private val coroutineDispatchers: CoroutineDispatchers,
        private val logInUseCase: LogInUseCase,
        private val observeLoginProvidersUseCase: ObserveLoginProvidersUseCase,
    ) {

        operator fun invoke(output: (LoginOutput) -> Unit): LoginViewModel = LoginViewModel(
            coroutineDispatchers = coroutineDispatchers,
            logInUseCase = logInUseCase,
            observeLoginProvidersUseCase = observeLoginProvidersUseCase,
            output = output,
        )
    }
}
