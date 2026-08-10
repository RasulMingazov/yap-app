package app.yap.feature.auth.presentation.login

import app.yap.feature.auth.domain.entity.LoginProvider
import app.yap.feature.auth.domain.entity.StubLoginProvider
import app.yap.feature.auth.generated.resources.Res
import app.yap.feature.auth.generated.resources.login_body
import app.yap.feature.auth.generated.resources.login_button
import app.yap.feature.auth.generated.resources.login_caption
import app.yap.feature.auth.generated.resources.login_error_connectivity
import app.yap.feature.auth.generated.resources.login_error_provider
import app.yap.feature.auth.generated.resources.login_hero
import app.yap.feature.auth.generated.resources.login_marquee
import app.yap.feature.auth.generated.resources.login_provider_coming_soon
import app.yap.feature.auth.generated.resources.login_provider_unavailable
import app.yap.feature.auth.generated.resources.login_topic_dating
import app.yap.feature.auth.generated.resources.login_topic_rejections
import app.yap.feature.auth.generated.resources.login_topic_small_talk
import org.jetbrains.compose.resources.StringResource

internal object LoginStubs {

    fun stubDataState(
        isLoading: Boolean = false,
        providers: List<LoginProvider> = StubLoginProvider.stubIosProviders(),
    ) = LoginModel.DataState(
        isLoading = isLoading,
        providers = providers,
    )

    fun stubUiState(
        body: StringResource = Res.string.login_body,
        button: LoginComponent.UiState.Button = stubLabelButton(),
        caption: StringResource = Res.string.login_caption,
        hero: StringResource = Res.string.login_hero,
        marquee: StringResource = Res.string.login_marquee,
        topics: List<StringResource> = listOf(
            Res.string.login_topic_small_talk,
            Res.string.login_topic_rejections,
            Res.string.login_topic_dating,
        ),
    ) = LoginComponent.UiState(
        body = body,
        button = button,
        caption = caption,
        hero = hero,
        marquee = marquee,
        topics = topics,
    )

    fun stubLabelButton(
        text: StringResource = Res.string.login_button,
    ): LoginComponent.UiState.Button = LoginComponent.UiState.Button.Label(text = text)

    fun stubSnackbarNews(
        formatArgs: List<String>,
        message: StringResource,
    ) = LoginComponent.News.ShowSnackbar(
        formatArgs = formatArgs,
        message = message,
    )

    fun stubComingSoonNews(
        displayName: String = StubLoginProvider.APPLE_DISPLAY_NAME,
    ): LoginComponent.News = stubSnackbarNews(
        formatArgs = listOf(displayName),
        message = Res.string.login_provider_coming_soon,
    )

    fun stubConnectivityFailureNews(): LoginComponent.News = stubSnackbarNews(
        formatArgs = emptyList(),
        message = Res.string.login_error_connectivity,
    )

    fun stubProviderFailureNews(
        displayName: String = StubLoginProvider.GOOGLE_DISPLAY_NAME,
    ): LoginComponent.News = stubSnackbarNews(
        formatArgs = listOf(displayName),
        message = Res.string.login_error_provider,
    )

    fun stubUnavailableNews(
        displayName: String = StubLoginProvider.GOOGLE_DISPLAY_NAME,
    ): LoginComponent.News = stubSnackbarNews(
        formatArgs = listOf(displayName),
        message = Res.string.login_provider_unavailable,
    )
}
