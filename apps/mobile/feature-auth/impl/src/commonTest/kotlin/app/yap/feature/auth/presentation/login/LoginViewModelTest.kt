package app.yap.feature.auth.presentation.login

import app.yap.core.common.platform.MotionPreferences
import app.yap.core.test.runViewModelTest
import app.yap.feature.auth.api.AuthNavKey
import app.yap.feature.auth.api.entity.AuthProvider
import app.yap.feature.auth.api.entity.LoginOutcome
import app.yap.feature.auth.domain.usecase.StubLoginUseCase
import app.yap.feature.auth.presentation.StubNavigator
import app.yap.feature.auth.generated.resources.Res
import app.yap.feature.auth.generated.resources.login_failed
import app.yap.feature.auth.generated.resources.login_provider_soon
import app.yap.feature.auth.generated.resources.login_provider_t_id
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent

internal class LoginViewModelTest {

    @Test
    fun `GIVEN the login screen WHEN the primary action is activated THEN the selection destination opens`() =
        runViewModelTest {
            val env = Environment()

            env.viewModel.onEvent(LoginViewModel.Event.PrimaryActionClicked)
            runCurrent()

            env.navigator.navigateCall.called(times = 1)
            env.navigator.navigateCall.calledWith(AuthNavKey.SelectAuthProvider)
        }

    @Test
    fun `GIVEN a provider is chosen WHEN the attempt starts THEN the login runs once for that provider`() =
        runViewModelTest {
            val env = Environment()

            env.viewModel.onEvent(LoginViewModel.Event.ProviderChosen(GOOGLE))
            advanceUntilIdle()

            env.loginUseCase.invokeCall.called(times = 1)
            env.loginUseCase.invokeCall.calledWith(GOOGLE)
        }

    @Test
    fun `GIVEN a provider is chosen WHEN the attempt runs THEN progress is shown and then cleared`() =
        runViewModelTest {
            val env = Environment()
            val progress = mutableListOf<Boolean>()
            backgroundScope.launch { env.viewModel.uiState.collect { state -> progress += state.isLoggingIn } }
            env.viewModel.onEvent(LoginViewModel.Event.PrimaryActionClicked)
            runCurrent()

            env.viewModel.onEvent(LoginViewModel.Event.ProviderChosen(GOOGLE))
            advanceUntilIdle()
            runCurrent()

            assertEquals(expected = listOf(false, true, false), actual = progress)
        }

    @Test
    fun `GIVEN an attempt fails WHEN it concludes THEN exactly one message is emitted`() = runViewModelTest {
        val env = Environment(outcome = LoginOutcome.Failed)
        val messages = mutableListOf<LoginViewModel.News>()
        val collection = launch { env.viewModel.news.toList(messages) }

        env.viewModel.onEvent(LoginViewModel.Event.ProviderChosen(GOOGLE))
        advanceUntilIdle()

        assertEquals(
            expected = listOf<LoginViewModel.News>(LoginViewModel.News.ShowMessage(Res.string.login_failed)),
            actual = messages,
        )
        assertEquals(expected = false, actual = env.viewModel.uiState.value.isLoggingIn)
        collection.cancel()
    }

    @Test
    fun `GIVEN a provider with no login path WHEN it is chosen THEN the not-yet-available notice is shown`() =
        runViewModelTest {
            val env = Environment(outcome = LoginOutcome.Unavailable)

            env.viewModel.onEvent(LoginViewModel.Event.ProviderChosen(T_ID))

            assertEquals(
                expected = LoginViewModel.News.ShowMessage(
                    message = Res.string.login_provider_soon,
                    argument = Res.string.login_provider_t_id,
                ),
                actual = env.viewModel.news.first(),
            )
        }

    @Test
    fun `GIVEN an attempt succeeds WHEN it concludes THEN the screen returns to idle in silence`() = runViewModelTest {
        val env = Environment(outcome = LoginOutcome.Success)
        val messages = mutableListOf<LoginViewModel.News>()
        val collection = launch { env.viewModel.news.toList(messages) }

        env.viewModel.onEvent(LoginViewModel.Event.ProviderChosen(GOOGLE))
        advanceUntilIdle()

        assertEquals(expected = false, actual = env.viewModel.uiState.value.isLoggingIn)
        assertEquals(expected = emptyList(), actual = messages)
        collection.cancel()
    }

    @Test
    fun `GIVEN an attempt is cancelled WHEN it concludes THEN the screen returns to idle in silence`() =
        runViewModelTest {
            val env = Environment(outcome = LoginOutcome.Cancelled)
            val messages = mutableListOf<LoginViewModel.News>()
            val collection = launch { env.viewModel.news.toList(messages) }
            env.viewModel.onEvent(LoginViewModel.Event.PrimaryActionClicked)

            env.viewModel.onEvent(LoginViewModel.Event.ProviderChosen(GOOGLE))
            advanceUntilIdle()

            assertEquals(expected = false, actual = env.viewModel.uiState.value.isLoggingIn)
            assertEquals(expected = emptyList(), actual = messages)
            collection.cancel()
        }

    @Test
    fun `GIVEN an attempt is in progress WHEN the primary action is activated again THEN nothing opens`() =
        runViewModelTest {
            val env = Environment(isAttemptHeldOpen = true)
            env.viewModel.onEvent(LoginViewModel.Event.ProviderChosen(GOOGLE))
            runCurrent()

            env.viewModel.onEvent(LoginViewModel.Event.PrimaryActionClicked)
            runCurrent()

            env.navigator.navigateCall.notCalled()
            env.releaseAttempt()
            advanceUntilIdle()
            env.loginUseCase.invokeCall.called(times = 1)
        }

    @Test
    fun `GIVEN an attempt is in progress WHEN a second provider is chosen THEN nothing else starts or is said`() =
        runViewModelTest {
            val env = Environment(isAttemptHeldOpen = true)
            val messages = mutableListOf<LoginViewModel.News>()
            val collection = launch { env.viewModel.news.toList(messages) }
            env.viewModel.onEvent(LoginViewModel.Event.ProviderChosen(GOOGLE))
            runCurrent()

            env.viewModel.onEvent(LoginViewModel.Event.ProviderChosen(T_ID))
            runCurrent()

            assertEquals(expected = emptyList(), actual = messages)
            env.releaseAttempt()
            advanceUntilIdle()
            env.loginUseCase.invokeCall.called(times = 1)
            collection.cancel()
        }

    private class Environment(
        isAttemptHeldOpen: Boolean = false,
        outcome: LoginOutcome = LoginOutcome.Success,
    ) {

        private val gate = CompletableDeferred<Unit>().apply { if (!isAttemptHeldOpen) complete(Unit) }

        val loginUseCase = StubLoginUseCase(outcome = outcome, gate = gate)
        val navigator = StubNavigator()
        val viewModel = LoginViewModel(
            loginUseCase = loginUseCase,
            motionPreferences = MotionPreferences { false },
            navigator = navigator,
            privacyUrl = "https://yap.app/privacy",
            termsUrl = "https://yap.app/terms",
        )

        fun releaseAttempt() = gate.complete(Unit)
    }

    private companion object {
        val GOOGLE: AuthProvider = AuthProvider.Google(isEnabled = true, isVisible = true)
        val T_ID: AuthProvider = AuthProvider.TId(isEnabled = false, isVisible = true)
    }
}
