package app.yap.feature.auth.presentation.login.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.runComposeUiTest
import app.yap.feature.auth.presentation.ComposeUiTestCase
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
internal class LoginScreenSemanticsTest : ComposeUiTestCase() {

    @Test
    fun `GIVEN the login screen WHEN it is read aloud THEN the primary action has a spoken name`() =
        runComposeUiTest {
            setContent { LoginScreenTestHost(uiState = stubLoginUiState()) }

            onNodeWithContentDescription("Войти в Yap").assertIsDisplayed()
        }

    @Test
    fun `GIVEN the login screen WHEN it is read aloud THEN both legal links have spoken names`() = runComposeUiTest {
        setContent { LoginScreenTestHost(uiState = stubLoginUiState()) }

        onNodeWithContentDescription("Открыть условия использования").assertIsDisplayed()
        onNodeWithContentDescription("Открыть политику конфиденциальности").assertIsDisplayed()
    }
}
