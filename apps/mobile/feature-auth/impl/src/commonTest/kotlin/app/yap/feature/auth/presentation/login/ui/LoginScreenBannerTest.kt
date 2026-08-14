package app.yap.feature.auth.presentation.login.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import app.yap.feature.auth.generated.resources.Res
import app.yap.feature.auth.generated.resources.login_failed
import app.yap.feature.auth.generated.resources.login_provider_not_available
import app.yap.feature.auth.presentation.ComposeUiTestCase
import app.yap.feature.auth.presentation.login.LoginViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.MutableSharedFlow

@OptIn(ExperimentalTestApi::class)
internal class LoginScreenBannerTest : ComposeUiTestCase() {

    @Test
    fun `GIVEN one message WHEN it arrives THEN exactly one banner is shown`() = runComposeUiTest {
        val news = MutableSharedFlow<LoginViewModel.News>(extraBufferCapacity = NEWS_BUFFER)
        setContent { LoginScreenTestHost(uiState = stubLoginUiState(), news = news) }

        news.tryEmit(LoginViewModel.News.ShowMessage(Res.string.login_failed))
        waitForIdle()

        onNodeWithTag(LoginTestTags.BANNER).assertIsDisplayed()
        assertEquals(expected = 1, actual = onAllNodesWithTag(LoginTestTags.BANNER).fetchSemanticsNodes().size)
    }

    @Test
    fun `GIVEN a banner is showing WHEN four seconds pass THEN it is gone`() = runComposeUiTest {
        mainClock.autoAdvance = false
        val news = MutableSharedFlow<LoginViewModel.News>(extraBufferCapacity = NEWS_BUFFER)
        setContent { LoginScreenTestHost(uiState = stubLoginUiState(), news = news) }

        news.tryEmit(LoginViewModel.News.ShowMessage(Res.string.login_failed))
        mainClock.advanceTimeBy(BANNER_MILLIS / 2)
        assertEquals(expected = 1, actual = onAllNodesWithTag(LoginTestTags.BANNER).fetchSemanticsNodes().size)

        mainClock.advanceTimeBy(BANNER_MILLIS)

        assertEquals(expected = 0, actual = onAllNodesWithTag(LoginTestTags.BANNER).fetchSemanticsNodes().size)
    }

    @Test
    fun `GIVEN a banner is showing WHEN the primary action is tapped THEN the tap reaches it rather than the banner`() =
        runComposeUiTest {
            val events = mutableListOf<LoginViewModel.Event>()
            val news = MutableSharedFlow<LoginViewModel.News>(extraBufferCapacity = NEWS_BUFFER)
            setContent {
                LoginScreenTestHost(
                    uiState = stubLoginUiState(),
                    news = news,
                    onEvent = { event -> events += event },
                )
            }
            news.tryEmit(LoginViewModel.News.ShowMessage(Res.string.login_failed))
            waitForIdle()

            onNodeWithTag(LoginTestTags.PRIMARY_ACTION).performClick()

            assertEquals(
                expected = listOf<LoginViewModel.Event>(LoginViewModel.Event.PrimaryActionClicked),
                actual = events,
            )
        }

    @Test
    fun `GIVEN a banner is showing WHEN a second message arrives THEN a second banner is not stacked`() =
        runComposeUiTest {
            val news = MutableSharedFlow<LoginViewModel.News>(extraBufferCapacity = NEWS_BUFFER)
            setContent { LoginScreenTestHost(uiState = stubLoginUiState(), news = news) }

            news.tryEmit(LoginViewModel.News.ShowMessage(Res.string.login_failed))
            waitForIdle()
            news.tryEmit(LoginViewModel.News.ShowMessage(Res.string.login_provider_not_available))
            waitForIdle()

            assertEquals(expected = 1, actual = onAllNodesWithTag(LoginTestTags.BANNER).fetchSemanticsNodes().size)
        }

    private companion object {
        const val BANNER_MILLIS = 4_000L
        const val NEWS_BUFFER = 4
    }
}
