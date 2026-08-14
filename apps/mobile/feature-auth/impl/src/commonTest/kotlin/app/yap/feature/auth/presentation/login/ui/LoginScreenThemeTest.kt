package app.yap.feature.auth.presentation.login.ui

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.runComposeUiTest
import app.yap.feature.auth.presentation.ComposeUiTestCase
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
internal class LoginScreenThemeTest : ComposeUiTestCase() {

    @Test
    fun `GIVEN the light theme WHEN the screen is shown THEN every element is reachable`() = runComposeUiTest {
        setContent { LoginScreenTestHost(uiState = stubLoginUiState(), isDarkTheme = false) }

        assertEveryLoginElementReachable()
    }

    @Test
    fun `GIVEN the dark theme WHEN the screen is shown THEN every element is reachable`() = runComposeUiTest {
        setContent { LoginScreenTestHost(uiState = stubLoginUiState(), isDarkTheme = true) }

        assertEveryLoginElementReachable()
    }
}

@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.assertEveryLoginElementReachable() {
    onNodeWithTag(LoginTestTags.MARQUEE).performScrollTo().assertIsDisplayed()
    onNodeWithTag(LoginTestTags.HERO).performScrollTo().assertIsDisplayed()
    onNodeWithTag(LoginTestTags.TOPIC).performScrollTo().assertIsDisplayed()
    onNodeWithTag(LoginTestTags.BODY).performScrollTo().assertIsDisplayed()
    onNodeWithTag(LoginTestTags.PRIMARY_ACTION).performScrollTo().assertIsDisplayed()
    onNodeWithTag(LoginTestTags.CAPTION).performScrollTo().assertIsDisplayed()
    onNodeWithTag(LoginTestTags.LEGAL_LINE).performScrollTo().assertIsDisplayed()
}
