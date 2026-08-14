package app.yap.feature.auth.presentation.login.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import app.yap.feature.auth.generated.resources.Res
import app.yap.feature.auth.generated.resources.login_failed
import app.yap.feature.auth.presentation.ComposeUiTestCase
import app.yap.feature.auth.presentation.login.LoginViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.MutableSharedFlow

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

    @Test
    fun `GIVEN reduced motion WHEN a message arrives THEN it shows without motion and still leaves on time`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            val news = MutableSharedFlow<LoginViewModel.News>(extraBufferCapacity = NEWS_BUFFER)
            setContent {
                LoginScreenTestHost(uiState = stubLoginUiState(isMotionReduced = true), news = news)
            }

            news.tryEmit(LoginViewModel.News.ShowMessage(Res.string.login_failed))
            mainClock.advanceTimeBy(SETTLE)
            assertEquals(expected = 1, actual = onAllNodesWithTag(LoginTestTags.SNACKBAR).fetchSemanticsNodes().size)

            mainClock.advanceTimeBy(SNACKBAR_MILLIS - SETTLE * 2)
            assertEquals(expected = 1, actual = onAllNodesWithTag(LoginTestTags.SNACKBAR).fetchSemanticsNodes().size)

            mainClock.advanceTimeBy(SETTLE * 2)

            assertEquals(expected = 0, actual = onAllNodesWithTag(LoginTestTags.SNACKBAR).fetchSemanticsNodes().size)
        }

    private companion object {
        const val FIRST_TOPIC = "SMALL TALK"
        const val SECOND_TOPIC = "ОТКАЗЫ"
        const val TIME_FOR_ONE_ROTATION = 2_500L
        const val TIME_FOR_SEVERAL_ROTATIONS = 12_000L
        const val NEWS_BUFFER = 4
        const val SETTLE = 128L
        const val SNACKBAR_MILLIS = 2_600L
    }
}
