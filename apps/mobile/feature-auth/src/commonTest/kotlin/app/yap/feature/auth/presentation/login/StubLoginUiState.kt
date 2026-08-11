package app.yap.feature.auth.presentation.login

import app.yap.feature.auth.generated.resources.Res
import app.yap.feature.auth.generated.resources.login_body
import app.yap.feature.auth.generated.resources.login_button
import app.yap.feature.auth.generated.resources.login_caption
import app.yap.feature.auth.generated.resources.login_hero
import app.yap.feature.auth.generated.resources.login_marquee
import app.yap.feature.auth.generated.resources.login_topic_dating
import app.yap.feature.auth.generated.resources.login_topic_rejections
import app.yap.feature.auth.generated.resources.login_topic_small_talk
import org.jetbrains.compose.resources.StringResource

internal object StubLoginUiState {

    fun stubLoginUiState(
        body: StringResource = Res.string.login_body,
        button: LoginUiState.Button = stubLabelButton(),
        caption: StringResource = Res.string.login_caption,
        hero: StringResource = Res.string.login_hero,
        marquee: StringResource = Res.string.login_marquee,
        topics: List<StringResource> = listOf(
            Res.string.login_topic_small_talk,
            Res.string.login_topic_rejections,
            Res.string.login_topic_dating,
        ),
    ) = LoginUiState(
        body = body,
        button = button,
        caption = caption,
        hero = hero,
        marquee = marquee,
        topics = topics,
    )

    fun stubLabelButton(
        text: StringResource = Res.string.login_button,
    ): LoginUiState.Button = LoginUiState.Button.Label(text = text)

    fun stubLoadingUiState() = stubLoginUiState(button = LoginUiState.Button.Loading)
}
