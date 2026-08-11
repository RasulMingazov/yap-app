package app.yap.feature.auth.presentation.login

import app.yap.feature.auth.domain.entity.LoginFailure
import app.yap.feature.auth.generated.resources.Res
import app.yap.feature.auth.generated.resources.login_error_connectivity
import app.yap.feature.auth.generated.resources.login_error_provider
import app.yap.feature.auth.generated.resources.login_provider_unavailable

/**
 * Resolves the snackbar copy for a recoverable failure. Only a provider-specific reason names the
 * provider; a connectivity failure is about the device (R-072, AC-025, AC-030).
 */
internal fun LoginFailure.toNews(displayName: String): LoginNews.ShowSnackbar = when (this) {
    LoginFailure.Configuration -> LoginNews.ShowSnackbar(
        formatArgs = listOf(displayName),
        message = Res.string.login_provider_unavailable,
    )

    LoginFailure.Connectivity -> LoginNews.ShowSnackbar(
        formatArgs = emptyList(),
        message = Res.string.login_error_connectivity,
    )

    LoginFailure.Provider -> LoginNews.ShowSnackbar(
        formatArgs = listOf(displayName),
        message = Res.string.login_error_provider,
    )
}
