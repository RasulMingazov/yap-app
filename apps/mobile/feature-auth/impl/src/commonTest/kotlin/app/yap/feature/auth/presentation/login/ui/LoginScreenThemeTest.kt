package app.yap.feature.auth.presentation.login.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.runComposeUiTest
import app.yap.core.design.theme.YapTheme
import app.yap.feature.auth.presentation.ComposeUiTestCase
import kotlin.test.Test
import kotlin.test.assertEquals

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

    @Test
    fun `GIVEN either theme WHEN a message is shown THEN it carries the same colours in both`() = runComposeUiTest {
        var light: Pair<Color, Color>? = null
        var dark: Pair<Color, Color>? = null

        setContent {
            YapTheme(darkTheme = false) { light = YapTheme.colors.notice to YapTheme.colors.onNotice }
            YapTheme(darkTheme = true) { dark = YapTheme.colors.notice to YapTheme.colors.onNotice }
        }

        assertEquals(expected = light, actual = dark)
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
