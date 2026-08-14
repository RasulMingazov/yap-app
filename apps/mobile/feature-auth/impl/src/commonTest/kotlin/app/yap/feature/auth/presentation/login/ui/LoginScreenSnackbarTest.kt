package app.yap.feature.auth.presentation.login.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import app.yap.feature.auth.generated.resources.Res
import app.yap.feature.auth.generated.resources.login_failed
import app.yap.feature.auth.generated.resources.login_provider_soon
import app.yap.feature.auth.generated.resources.login_provider_t_id
import app.yap.feature.auth.presentation.ComposeUiTestCase
import app.yap.feature.auth.presentation.login.LoginViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.MutableSharedFlow

@OptIn(ExperimentalTestApi::class)
internal class LoginScreenSnackbarTest : ComposeUiTestCase() {

    @Test
    fun `GIVEN one message WHEN it arrives THEN exactly one snackbar is shown`() = runComposeUiTest {
        val news = MutableSharedFlow<LoginViewModel.News>(extraBufferCapacity = NEWS_BUFFER)
        setContent { LoginScreenTestHost(uiState = stubLoginUiState(isMotionReduced = false), news = news) }

        news.tryEmit(LoginViewModel.News.ShowMessage(Res.string.login_failed))
        waitForIdle()

        onNodeWithTag(LoginTestTags.SNACKBAR).assertIsDisplayed()
        assertEquals(expected = 1, actual = onAllNodesWithTag(LoginTestTags.SNACKBAR).fetchSemanticsNodes().size)
    }

    @Test
    fun `GIVEN a message names a provider WHEN it is shown THEN the provider's name is in the text`() =
        runComposeUiTest {
            val news = MutableSharedFlow<LoginViewModel.News>(extraBufferCapacity = NEWS_BUFFER)
            setContent { LoginScreenTestHost(uiState = stubLoginUiState(isMotionReduced = false), news = news) }

            news.tryEmit(
                LoginViewModel.News.ShowMessage(
                    message = Res.string.login_provider_soon,
                    argument = Res.string.login_provider_t_id,
                ),
            )
            waitForIdle()

            onNodeWithText("Вход через T-ID скоро появится").assertIsDisplayed()
        }

    @Test
    fun `GIVEN a snackbar is showing WHEN its time is up THEN it is gone`() = runComposeUiTest {
        mainClock.autoAdvance = false
        val news = MutableSharedFlow<LoginViewModel.News>(extraBufferCapacity = NEWS_BUFFER)
        setContent { LoginScreenTestHost(uiState = stubLoginUiState(isMotionReduced = false), news = news) }

        news.tryEmit(LoginViewModel.News.ShowMessage(Res.string.login_failed))
        mainClock.advanceTimeBy(SNACKBAR_MILLIS / 2)
        assertEquals(expected = 1, actual = onAllNodesWithTag(LoginTestTags.SNACKBAR).fetchSemanticsNodes().size)

        mainClock.advanceTimeBy(SNACKBAR_MILLIS)

        assertEquals(expected = 0, actual = onAllNodesWithTag(LoginTestTags.SNACKBAR).fetchSemanticsNodes().size)
    }

    @Test
    fun `GIVEN two messages in a row WHEN the first leaves THEN the second follows without either being dropped`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            val news = MutableSharedFlow<LoginViewModel.News>(extraBufferCapacity = NEWS_BUFFER)
            setContent { LoginScreenTestHost(uiState = stubLoginUiState(isMotionReduced = false), news = news) }

            news.tryEmit(LoginViewModel.News.ShowMessage(Res.string.login_failed))
            mainClock.advanceTimeBy(SETTLED_ENTER)
            news.tryEmit(
                LoginViewModel.News.ShowMessage(
                    message = Res.string.login_provider_soon,
                    argument = Res.string.login_provider_t_id,
                ),
            )
            mainClock.advanceTimeBy(SETTLED_ENTER)

            onNodeWithText(FAILED_TEXT).assertIsDisplayed()
            assertEquals(expected = 1, actual = onAllNodesWithTag(LoginTestTags.SNACKBAR).fetchSemanticsNodes().size)

            mainClock.advanceTimeBy(SNACKBAR_MILLIS - SETTLED_ENTER * 2 + EXIT_MILLIS / 2)

            onNodeWithText(FAILED_TEXT).assertIsDisplayed()

            mainClock.advanceTimeBy(EXIT_MILLIS + SETTLED_ENTER)

            onNodeWithText(SOON_TEXT).assertIsDisplayed()
            assertEquals(expected = 1, actual = onAllNodesWithTag(LoginTestTags.SNACKBAR).fetchSemanticsNodes().size)
        }

    @Test
    fun `GIVEN a snackbar is showing WHEN the primary action is tapped THEN the tap reaches it rather than the snackbar`() =
        runComposeUiTest {
            val events = mutableListOf<LoginViewModel.Event>()
            val news = MutableSharedFlow<LoginViewModel.News>(extraBufferCapacity = NEWS_BUFFER)
            setContent {
                LoginScreenTestHost(
                    uiState = stubLoginUiState(isMotionReduced = false),
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

    private companion object {
        const val EXIT_MILLIS = 220L
        const val FAILED_TEXT = "Не удалось войти. Попробуйте ещё раз"
        const val NEWS_BUFFER = 4
        const val SETTLED_ENTER = 320L
        const val SNACKBAR_MILLIS = 2_600L
        const val SOON_TEXT = "Вход через T-ID скоро появится"
    }
}
