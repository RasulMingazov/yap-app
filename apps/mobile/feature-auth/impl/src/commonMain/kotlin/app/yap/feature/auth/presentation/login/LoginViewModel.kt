package app.yap.feature.auth.presentation.login

import app.yap.core.common.platform.MotionPreferences
import app.yap.core.common.platform.Platform
import app.yap.core.common.presentation.BaseViewModel
import app.yap.feature.auth.api.entity.AuthProvider
import app.yap.feature.auth.api.entity.LoginOutcome
import app.yap.feature.auth.api.usecase.LoginUseCase
import app.yap.feature.auth.generated.resources.Res
import app.yap.feature.auth.generated.resources.login_provider_not_available
import app.yap.feature.auth.generated.resources.login_failed
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource

internal class LoginViewModel(
    private val loginUseCases: Map<AuthProvider, LoginUseCase>,
    private val motionPreferences: MotionPreferences,
    private val platform: Platform,
    private val privacyUrl: String?,
    private val termsUrl: String?,
    private val declarations: List<AuthProviderDeclaration> = AuthProviderCatalog.DECLARATIONS,
) : BaseViewModel() {

    private val dataState = MutableStateFlow(DataState())
    private val newsChannel = Channel<News>(Channel.BUFFERED)

    val uiState: StateFlow<UiState> = dataState.mapState { state ->
        LoginUiStateMapper(
            dataState = state,
            isMotionReduced = motionPreferences.isReduced(),
            platform = platform,
            privacyUrl = privacyUrl,
            termsUrl = termsUrl,
            declarations = declarations,
        )
    }

    val news: Flow<News> = newsChannel.receiveAsFlow()

    fun onEvent(event: Event) = when (event) {
        is Event.ProviderChosen -> onProviderChosen(event.provider)
        is Event.ProviderSheetDismissed -> onProviderSheetDismissed()
        is Event.PrimaryActionClicked -> onPrimaryActionClicked()
    }

    private fun onPrimaryActionClicked() {
        if (dataState.value.isLoggingIn) return
        dataState.update { state -> state.copy(isProviderSheetVisible = true) }
    }

    private fun onProviderChosen(provider: AuthProvider) {
        if (dataState.value.isLoggingIn) return

        val loginUseCase = loginPathFor(provider)
        if (loginUseCase == null) {
            newsChannel.trySend(News.ShowMessage(Res.string.login_provider_not_available))
            return
        }

        dataState.update { state -> state.copy(isProviderSheetVisible = false, isLoggingIn = true) }
        viewModelScope.launch {
            val outcome = loginUseCase()
            dataState.update { state -> state.copy(isLoggingIn = false) }
            onOutcome(outcome)
        }
    }

    private fun loginPathFor(provider: AuthProvider): LoginUseCase? = declarations
        .firstOrNull { declaration -> declaration.provider == provider }
        ?.takeIf { declaration -> platform in declaration.shownOn && declaration.isUsable }
        ?.let { loginUseCases[provider] }

    private fun onProviderSheetDismissed() {
        dataState.update { state -> state.copy(isProviderSheetVisible = false) }
    }

    private fun onOutcome(outcome: LoginOutcome) = when (outcome) {
        is LoginOutcome.Success -> Unit
        is LoginOutcome.Cancelled -> Unit
        is LoginOutcome.Failed -> {
            newsChannel.trySend(News.ShowMessage(Res.string.login_failed))
            Unit
        }
    }

    override fun onCleared() {
        super.onCleared()
        newsChannel.close()
    }

    data class DataState(
        val isProviderSheetVisible: Boolean = false,
        val isLoggingIn: Boolean = false,
    )

    data class UiState(
        val isProviderSheetVisible: Boolean,
        val isMotionReduced: Boolean,
        val isLoggingIn: Boolean,
        val providers: List<Provider>,
        val privacyUrl: String?,
        val termsUrl: String?,
        val topics: List<StringResource>,
    ) {

        data class Provider(
            val isAvailable: Boolean,
            val labelRes: StringResource,
            val provider: AuthProvider,
        )
    }

    sealed interface News {

        data class ShowMessage(val message: StringResource) : News
    }

    sealed interface Event {

        data class ProviderChosen(val provider: AuthProvider) : Event

        data object ProviderSheetDismissed : Event

        data object PrimaryActionClicked : Event
    }
}
