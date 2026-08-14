package app.yap.feature.auth.presentation.login

import app.yap.feature.auth.generated.resources.Res
import app.yap.feature.auth.generated.resources.login_topic_meeting_people
import app.yap.feature.auth.generated.resources.login_topic_rejections
import app.yap.feature.auth.generated.resources.login_topic_small_talk
import kotlin.test.Test
import kotlin.test.assertEquals

internal class LoginUiStateMapperTest {

    @Test
    fun `GIVEN an idle screen WHEN the state is mapped THEN it carries the topics and both destinations`() {
        val uiState = map()

        assertEquals(
            expected = LoginViewModel.UiState(
                isLoggingIn = false,
                isMotionReduced = false,
                privacyUrl = PRIVACY_URL,
                termsUrl = TERMS_URL,
                topics = listOf(
                    Res.string.login_topic_small_talk,
                    Res.string.login_topic_rejections,
                    Res.string.login_topic_meeting_people,
                ),
            ),
            actual = uiState,
        )
    }

    @Test
    fun `GIVEN an attempt in progress WHEN the state is mapped THEN progress is shown`() {
        val uiState = map(dataState = LoginViewModel.DataState(isLoggingIn = true))

        assertEquals(expected = true, actual = uiState.isLoggingIn)
    }

    @Test
    fun `GIVEN the system asks for reduced motion WHEN the state is mapped THEN animation is off`() {
        assertEquals(expected = true, actual = map(isMotionReduced = true).isMotionReduced)
        assertEquals(expected = false, actual = map(isMotionReduced = false).isMotionReduced)
    }

    @Test
    fun `GIVEN an unset legal destination WHEN the state is mapped THEN that link has nowhere to go`() {
        val uiState = map(privacyUrl = null, termsUrl = TERMS_URL)

        assertEquals(expected = null, actual = uiState.privacyUrl)
        assertEquals(expected = TERMS_URL, actual = uiState.termsUrl)
    }

    private fun map(
        dataState: LoginViewModel.DataState = LoginViewModel.DataState(),
        isMotionReduced: Boolean = false,
        privacyUrl: String? = PRIVACY_URL,
        termsUrl: String? = TERMS_URL,
    ): LoginViewModel.UiState = LoginUiStateMapper(
        dataState = dataState,
        isMotionReduced = isMotionReduced,
        privacyUrl = privacyUrl,
        termsUrl = termsUrl,
    )

    private companion object {
        const val PRIVACY_URL = "https://yap.app/privacy"
        const val TERMS_URL = "https://yap.app/terms"
    }
}
