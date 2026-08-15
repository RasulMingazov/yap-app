package app.yap.feature.auth.presentation.login

import androidx.lifecycle.viewModelScope
import app.yap.core.common.navigation.Navigator
import app.yap.core.common.platform.MotionPreferences
import app.yap.core.common.presentation.BaseViewModel
import app.yap.feature.auth.api.AuthNavKey
import app.yap.feature.auth.api.entity.AuthProvider
import app.yap.feature.auth.api.entity.LegalLinks
import app.yap.feature.auth.api.usecase.GetLegalLinksUseCase
import app.yap.feature.auth.api.usecase.LoginUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource

internal class LoginViewModel(
    private val getLegalLinksUseCase: GetLegalLinksUseCase,
    private val loginUseCase: LoginUseCase,
    private val motionPreferences: MotionPreferences,
    private val navigator: Navigator,
    private val newsMapper: LoginNewsMapper,
    private val uiStateMapper: LoginUiStateMapper,
) : BaseViewModel() {

    private val dataState = MutableStateFlow(DataState())
    val uiState: StateFlow<UiState> = dataState.mapState { state ->
        uiStateMapper(
            dataState = state,
            isMotionReduced = motionPreferences.isReduced()
        )
    }

    private val newsChannel = Channel<News>(Channel.BUFFERED)
    val news: Flow<News> = newsChannel.receiveAsFlow()

    init {
        viewModelScope.launch {
            val legalLinks = getLegalLinksUseCase()
            dataState.update { state -> state.copy(legalLinks = legalLinks) }
        }
    }

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
            newsMapper(outcome = outcome, provider = provider)?.let(newsChannel::trySend)
        }
    }

    override fun onCleared() {
        super.onCleared()
        newsChannel.close()
    }

    data class DataState(
        val isLoggingIn: Boolean = false,
        val legalLinks: LegalLinks = LegalLinks(privacyUrl = null, termsUrl = null),
    )

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
