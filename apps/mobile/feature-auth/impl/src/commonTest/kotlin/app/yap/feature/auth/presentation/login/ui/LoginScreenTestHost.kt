package app.yap.feature.auth.presentation.login.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import app.yap.core.design.theme.YapTheme
import app.yap.feature.auth.presentation.login.LoginUiStateMapper
import app.yap.feature.auth.presentation.login.LoginViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

internal const val PRIVACY_URL = "https://yap.app/privacy"
internal const val TERMS_URL = "https://yap.app/terms"

@Composable
internal fun LoginScreenTestHost(
    uiState: LoginViewModel.UiState,
    isDarkTheme: Boolean = false,
    news: Flow<LoginViewModel.News> = emptyFlow(),
    onEvent: (LoginViewModel.Event) -> Unit = {},
    uriHandler: UriHandler? = null,
) {
    YapTheme(darkTheme = isDarkTheme) {
        if (uriHandler == null) {
            LoginScreenContent(uiState = uiState, onEvent = onEvent, news = news)
        } else {
            CompositionLocalProvider(LocalUriHandler provides uriHandler) {
                LoginScreenContent(uiState = uiState, onEvent = onEvent, news = news)
            }
        }
    }
}

internal fun stubLoginUiState(
    isMotionReduced: Boolean = true,
    isLoggingIn: Boolean = false,
    privacyUrl: String? = PRIVACY_URL,
    termsUrl: String? = TERMS_URL,
): LoginViewModel.UiState = LoginUiStateMapper(
    dataState = LoginViewModel.DataState(isLoggingIn = isLoggingIn),
    isMotionReduced = isMotionReduced,
    privacyUrl = privacyUrl,
    termsUrl = termsUrl,
)
