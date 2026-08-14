package app.yap.feature.auth.presentation.login

import androidx.lifecycle.viewModelScope
import app.yap.core.common.navigation.Navigator
import app.yap.core.common.platform.MotionPreferences
import app.yap.core.common.presentation.BaseViewModel
import app.yap.feature.auth.api.AuthNavKey
import app.yap.feature.auth.api.entity.AuthProvider
import app.yap.feature.auth.api.entity.LoginOutcome
import app.yap.feature.auth.api.usecase.LoginUseCase
import app.yap.feature.auth.generated.resources.Res
import app.yap.feature.auth.generated.resources.login_failed
import app.yap.feature.auth.generated.resources.login_provider_soon
import app.yap.feature.auth.presentation.AuthProviderResources
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource

internal class LoginViewModel(
    private val loginUseCase: LoginUseCase,
    private val motionPreferences: MotionPreferences,
    private val navigator: Navigator,
    private val privacyUrl: String?,
    private val termsUrl: String?,
) : BaseViewModel() {

    private val dataState = MutableStateFlow(DataState())
    private val newsChannel = Channel<News>(Channel.BUFFERED)

    val uiState: StateFlow<UiState> = dataState.mapState { state ->
        LoginUiStateMapper(
            dataState = state,
            isMotionReduced = motionPreferences.isReduced(),
            privacyUrl = privacyUrl,
            termsUrl = termsUrl,
        )
    }

    val news: Flow<News> = newsChannel.receiveAsFlow()

    fun onEvent(event: Event) = when (event) {
        is Event.ProviderChosen -> onProviderChosen(event.provider)
        is Event.PrimaryActionClicked -> onPrimaryActionClicked()
    }

    private fun onPrimaryActionClicked() {
        if (dataState.value.isLoggingIn) return
        navigator.navigate(AuthNavKey.SelectAuthProvider)
    }

    private fun onProviderChosen(provider: AuthProvider) {
        if (dataState.value.isLoggingIn) return

        dataState.update { state -> state.copy(isLoggingIn = true) }
        viewModelScope.launch {
            val outcome = loginUseCase(provider)
            dataState.update { state -> state.copy(isLoggingIn = false) }
            onOutcome(outcome = outcome, provider = provider)
        }
    }

    private fun onOutcome(outcome: LoginOutcome, provider: AuthProvider) = when (outcome) {
        is LoginOutcome.Success -> Unit
        is LoginOutcome.Cancelled -> Unit
        is LoginOutcome.Unavailable -> {
            newsChannel.trySend(
                News.ShowMessage(
                    message = Res.string.login_provider_soon,
                    argument = AuthProviderResources.labelOf(provider),
                ),
            )
            Unit
        }

        is LoginOutcome.Failed -> {
            newsChannel.trySend(News.ShowMessage(Res.string.login_failed))
            Unit
        }
    }

    override fun onCleared() {
        super.onCleared()
        newsChannel.close()
    }

    data class DataState(val isLoggingIn: Boolean = false)

    data class UiState(
        val isLoggingIn: Boolean,
        val isMotionReduced: Boolean,
        val privacyUrl: String?,
        val termsUrl: String?,
        val topics: List<StringResource>,
    )

    sealed interface News {

        data class ShowMessage(
            val message: StringResource,
            val argument: StringResource? = null,
        ) : News
    }

    sealed interface Event {

        data class ProviderChosen(val provider: AuthProvider) : Event

        data object PrimaryActionClicked : Event
    }
}
