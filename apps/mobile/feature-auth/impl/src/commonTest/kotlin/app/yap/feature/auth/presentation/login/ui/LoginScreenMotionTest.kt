package app.yap.feature.auth.presentation.login.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import app.yap.feature.auth.presentation.ComposeUiTestCase
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
internal class LoginScreenMotionTest : ComposeUiTestCase() {

    @Test
    fun `GIVEN reduced motion WHEN time passes THEN the rotating topic stays on its first word`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            setContent { LoginScreenTestHost(uiState = stubLoginUiState(isMotionReduced = true)) }

            mainClock.advanceTimeBy(TIME_FOR_SEVERAL_ROTATIONS)

            onNodeWithText(FIRST_TOPIC).assertIsDisplayed()
        }

    @Test
    fun `GIVEN motion is allowed WHEN time passes THEN the rotating topic moves on`() = runComposeUiTest {
        mainClock.autoAdvance = false
        setContent { LoginScreenTestHost(uiState = stubLoginUiState(isMotionReduced = false)) }

        mainClock.advanceTimeBy(TIME_FOR_ONE_ROTATION)

        onNodeWithText(SECOND_TOPIC).assertIsDisplayed()
    }

    @Test
    fun `GIVEN reduced motion WHEN the screen is shown THEN the marquee band is still readable`() =
        runComposeUiTest {
            setContent { LoginScreenTestHost(uiState = stubLoginUiState(isMotionReduced = true)) }

            onNodeWithTag(LoginTestTags.MARQUEE).assertIsDisplayed()
        }

    private companion object {
        const val FIRST_TOPIC = "SMALL TALK"
        const val SECOND_TOPIC = "ОТКАЗЫ"
        const val TIME_FOR_ONE_ROTATION = 2_500L
        const val TIME_FOR_SEVERAL_ROTATIONS = 12_000L
    }
}
