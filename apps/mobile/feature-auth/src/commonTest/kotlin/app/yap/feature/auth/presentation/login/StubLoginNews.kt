package app.yap.feature.auth.presentation.login

import app.yap.feature.auth.domain.entity.StubLoginProvider
import app.yap.feature.auth.generated.resources.Res
import app.yap.feature.auth.generated.resources.login_error_connectivity
import app.yap.feature.auth.generated.resources.login_error_provider
import app.yap.feature.auth.generated.resources.login_provider_coming_soon
import app.yap.feature.auth.generated.resources.login_provider_unavailable
import org.jetbrains.compose.resources.StringResource

internal object StubLoginNews {

    fun stubSnackbarNews(
        formatArgs: List<String>,
        message: StringResource,
    ) = LoginNews.ShowSnackbar(
        formatArgs = formatArgs,
        message = message,
    )

    fun stubComingSoonNews(
        displayName: String = StubLoginProvider.APPLE_DISPLAY_NAME,
    ): LoginNews = stubSnackbarNews(
        formatArgs = listOf(displayName),
        message = Res.string.login_provider_coming_soon,
    )

    fun stubConnectivityFailureNews(): LoginNews = stubSnackbarNews(
        formatArgs = emptyList(),
        message = Res.string.login_error_connectivity,
    )

    fun stubProviderFailureNews(
        displayName: String = StubLoginProvider.GOOGLE_DISPLAY_NAME,
    ): LoginNews = stubSnackbarNews(
        formatArgs = listOf(displayName),
        message = Res.string.login_error_provider,
    )

    fun stubUnavailableNews(
        displayName: String = StubLoginProvider.GOOGLE_DISPLAY_NAME,
    ): LoginNews = stubSnackbarNews(
        formatArgs = listOf(displayName),
        message = Res.string.login_provider_unavailable,
    )
}
