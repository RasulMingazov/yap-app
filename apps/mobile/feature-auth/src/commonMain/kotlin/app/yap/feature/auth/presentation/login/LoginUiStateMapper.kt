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

/**
 * Selects every repeatable value the screen renders, including the loading state that replaces the
 * button label (R-003, R-069, AC-044).
 */
internal fun LoginModel.DataState.toUiState(): LoginComponent.UiState = LoginComponent.UiState(
    body = Res.string.login_body,
    button = if (isLoading) {
        LoginComponent.UiState.Button.Loading
    } else {
        LoginComponent.UiState.Button.Label(text = Res.string.login_button)
    },
    caption = Res.string.login_caption,
    hero = Res.string.login_hero,
    marquee = Res.string.login_marquee,
    topics = listOf(
        Res.string.login_topic_small_talk,
        Res.string.login_topic_rejections,
        Res.string.login_topic_dating,
    ),
)
