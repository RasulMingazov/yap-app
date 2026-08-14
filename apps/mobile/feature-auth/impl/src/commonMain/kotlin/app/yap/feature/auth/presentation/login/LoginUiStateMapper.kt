package app.yap.feature.auth.presentation.login

import app.yap.feature.auth.generated.resources.Res
import app.yap.feature.auth.generated.resources.login_topic_meeting_people
import app.yap.feature.auth.generated.resources.login_topic_rejections
import app.yap.feature.auth.generated.resources.login_topic_small_talk
import org.jetbrains.compose.resources.StringResource

internal object LoginUiStateMapper {

    private val TOPICS: List<StringResource> = listOf(
        Res.string.login_topic_small_talk,
        Res.string.login_topic_rejections,
        Res.string.login_topic_meeting_people,
    )

    operator fun invoke(
        dataState: LoginViewModel.DataState,
        isMotionReduced: Boolean,
        privacyUrl: String?,
        termsUrl: String?,
    ): LoginViewModel.UiState = LoginViewModel.UiState(
        isLoggingIn = dataState.isLoggingIn,
        isMotionReduced = isMotionReduced,
        privacyUrl = privacyUrl,
        termsUrl = termsUrl,
        topics = TOPICS,
    )
}
