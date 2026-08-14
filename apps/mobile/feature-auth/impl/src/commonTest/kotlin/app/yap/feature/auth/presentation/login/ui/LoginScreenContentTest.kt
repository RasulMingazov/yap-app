package app.yap.feature.auth.presentation.login.ui

import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import app.yap.feature.auth.presentation.ComposeUiTestCase
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
internal class LoginScreenContentTest : ComposeUiTestCase() {

    @Test
    fun `GIVEN the login screen WHEN it is shown THEN every element is present top to bottom`() = runComposeUiTest {
        setContent { LoginScreenTestHost(uiState = stubLoginUiState()) }

        onNodeWithTag(LoginTestTags.MARQUEE).assertIsDisplayed()
        onNodeWithTag(LoginTestTags.HERO).assertIsDisplayed()
        onNodeWithTag(LoginTestTags.TOPIC).assertIsDisplayed()
        onNodeWithTag(LoginTestTags.BODY).assertIsDisplayed()
        onNodeWithTag(LoginTestTags.PRIMARY_ACTION).assertIsDisplayed()
        onNodeWithTag(LoginTestTags.CAPTION).assertIsDisplayed()
        onNodeWithTag(LoginTestTags.LEGAL_LINE).assertIsDisplayed()
    }

    @Test
    fun `GIVEN an attempt in progress WHEN the screen is shown THEN the primary action shows progress`() =
        runComposeUiTest {
            setContent { LoginScreenTestHost(uiState = stubLoginUiState(isLoggingIn = true)) }

            onNodeWithTag(LoginTestTags.PRIMARY_ACTION_PROGRESS).assertIsDisplayed()
        }

    @Test
    fun `GIVEN a configured terms destination WHEN its link is tapped THEN exactly that address is opened`() =
        runComposeUiTest {
            val uriHandler = RecordingUriHandler()
            setContent { LoginScreenTestHost(uiState = stubLoginUiState(), uriHandler = uriHandler) }

            onNodeWithTag(LoginTestTags.LEGAL_TERMS_LINK).performClick()

            assertEquals(expected = listOf(TERMS_URL), actual = uriHandler.opened)
        }

    @Test
    fun `GIVEN a configured privacy destination WHEN its link is tapped THEN exactly that address is opened`() =
        runComposeUiTest {
            val uriHandler = RecordingUriHandler()
            setContent { LoginScreenTestHost(uiState = stubLoginUiState(), uriHandler = uriHandler) }

            onNodeWithTag(LoginTestTags.LEGAL_PRIVACY_LINK).performClick()

            assertEquals(expected = listOf(PRIVACY_URL), actual = uriHandler.opened)
        }

    @Test
    fun `GIVEN an unset terms destination WHEN its link is tapped THEN nothing is opened and nothing fails`() =
        runComposeUiTest {
            val uriHandler = RecordingUriHandler()
            setContent {
                LoginScreenTestHost(uiState = stubLoginUiState(termsUrl = null), uriHandler = uriHandler)
            }

            onNodeWithTag(LoginTestTags.LEGAL_TERMS_LINK).performClick()

            assertEquals(expected = emptyList(), actual = uriHandler.opened)
        }

    private class RecordingUriHandler : UriHandler {

        val opened = mutableListOf<String>()

        override fun openUri(uri: String) {
            opened += uri
        }
    }

}
