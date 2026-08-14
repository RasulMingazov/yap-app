package app.yap.feature.auth.presentation.login

import app.yap.core.common.platform.MotionPreferences
import app.yap.core.common.platform.Platform
import app.yap.core.test.runViewModelTest
import app.yap.feature.auth.api.entity.AuthProvider
import app.yap.feature.auth.api.entity.LoginOutcome
import app.yap.feature.auth.api.usecase.LoginUseCase
import app.yap.feature.auth.domain.usecase.StubLoginUseCase
import app.yap.feature.auth.generated.resources.Res
import app.yap.feature.auth.generated.resources.login_provider_not_available
import app.yap.feature.auth.generated.resources.login_failed
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
    fun `GIVEN the login screen WHEN the primary action is activated THEN the provider sheet opens`() =
        runViewModelTest {
            val env = Environment()

            env.viewModel.onEvent(LoginViewModel.Event.PrimaryActionClicked)
            runCurrent()

            assertEquals(expected = true, actual = env.viewModel.uiState.value.isProviderSheetVisible)
        }

    @Test
    fun `GIVEN the provider sheet is open WHEN Google is chosen THEN exactly one attempt starts`() = runViewModelTest {
        val env = Environment()
        env.viewModel.onEvent(LoginViewModel.Event.PrimaryActionClicked)

        env.viewModel.onEvent(LoginViewModel.Event.ProviderChosen(AuthProvider.GOOGLE))
        advanceUntilIdle()

        env.loginUseCase.invokeCall.called(times = 1)
    }

    @Test
    fun `GIVEN the provider sheet is open WHEN Apple is chosen THEN no attempt starts`() = runViewModelTest {
        val env = Environment()
        env.viewModel.onEvent(LoginViewModel.Event.PrimaryActionClicked)

        env.viewModel.onEvent(LoginViewModel.Event.ProviderChosen(AuthProvider.APPLE))
        advanceUntilIdle()

        env.loginUseCase.invokeCall.notCalled()
        assertEquals(expected = false, actual = env.viewModel.uiState.value.isLoggingIn)
    }

    @Test
    fun `GIVEN the provider sheet is open WHEN Apple is chosen THEN the not-yet-available notice is shown`() =
        runViewModelTest {
            val env = Environment()
            env.viewModel.onEvent(LoginViewModel.Event.PrimaryActionClicked)

            env.viewModel.onEvent(LoginViewModel.Event.ProviderChosen(AuthProvider.APPLE))

            assertEquals(
                expected = LoginViewModel.News.ShowMessage(Res.string.login_provider_not_available),
                actual = env.viewModel.news.first(),
            )
        }

    @Test
    fun `GIVEN the provider sheet is open WHEN T-ID is chosen THEN the not-yet-available notice is shown`() =
        runViewModelTest {
            val env = Environment()
            env.viewModel.onEvent(LoginViewModel.Event.PrimaryActionClicked)

            env.viewModel.onEvent(LoginViewModel.Event.ProviderChosen(AuthProvider.T_ID))

            assertEquals(
                expected = LoginViewModel.News.ShowMessage(Res.string.login_provider_not_available),
                actual = env.viewModel.news.first(),
            )
        }

    @Test
    fun `GIVEN Google was chosen WHEN the attempt runs THEN progress is shown and then cleared`() = runViewModelTest {
        val env = Environment()
        val progress = mutableListOf<Boolean>()
        backgroundScope.launch { env.viewModel.uiState.collect { state -> progress += state.isLoggingIn } }
        env.viewModel.onEvent(LoginViewModel.Event.PrimaryActionClicked)
        runCurrent()

        env.viewModel.onEvent(LoginViewModel.Event.ProviderChosen(AuthProvider.GOOGLE))
        advanceUntilIdle()
        runCurrent()

        assertEquals(expected = listOf(false, true, false), actual = progress)
        assertEquals(expected = false, actual = env.viewModel.uiState.value.isProviderSheetVisible)
    }

    @Test
    fun `GIVEN an attempt fails WHEN it concludes THEN one message is shown and login stays available`() =
        runViewModelTest {
            val env = Environment(outcome = LoginOutcome.Failed)
            env.viewModel.onEvent(LoginViewModel.Event.PrimaryActionClicked)

            env.viewModel.onEvent(LoginViewModel.Event.ProviderChosen(AuthProvider.GOOGLE))
            advanceUntilIdle()

            assertEquals(
                expected = LoginViewModel.News.ShowMessage(Res.string.login_failed),
                actual = env.viewModel.news.first(),
            )
            assertEquals(expected = false, actual = env.viewModel.uiState.value.isLoggingIn)
        }

    @Test
    fun `GIVEN a provider declared usable WHEN it is chosen THEN its own login path runs exactly once`() =
        runViewModelTest {
            val env = Environment(
                declarations = listOf(stubAuthProviderDeclaration(provider = CHOSEN, isUsable = true)),
                loginPathFor = CHOSEN,
            )

            env.viewModel.onEvent(LoginViewModel.Event.ProviderChosen(CHOSEN))
            advanceUntilIdle()

            env.loginUseCase.invokeCall.called(times = 1)
        }

    @Test
    fun `GIVEN a provider declared unusable WHEN it is chosen THEN the notice is shown with no attempt`() =
        runViewModelTest {
            val env = Environment(
                declarations = listOf(stubAuthProviderDeclaration(provider = CHOSEN, isUsable = false)),
                loginPathFor = CHOSEN,
            )

            env.viewModel.onEvent(LoginViewModel.Event.ProviderChosen(CHOSEN))

            assertEquals(
                expected = LoginViewModel.News.ShowMessage(Res.string.login_provider_not_available),
                actual = env.viewModel.news.first(),
            )
            advanceUntilIdle()
            env.loginUseCase.invokeCall.notCalled()
            assertEquals(expected = false, actual = env.viewModel.uiState.value.isLoggingIn)
        }

    @Test
    fun `GIVEN a provider declared usable but hidden WHEN it is chosen THEN the notice is shown with no attempt`() =
        runViewModelTest {
            val env = Environment(
                declarations = listOf(
                    stubAuthProviderDeclaration(provider = CHOSEN, isUsable = true, shownOn = setOf(Platform.IOS)),
                ),
                loginPathFor = CHOSEN,
            )

            env.viewModel.onEvent(LoginViewModel.Event.ProviderChosen(CHOSEN))

            assertEquals(
                expected = LoginViewModel.News.ShowMessage(Res.string.login_provider_not_available),
                actual = env.viewModel.news.first(),
            )
            advanceUntilIdle()
            env.loginUseCase.invokeCall.notCalled()
            assertEquals(expected = false, actual = env.viewModel.uiState.value.isLoggingIn)
        }

    @Test
    fun `GIVEN an attempt is cancelled WHEN it concludes THEN the screen returns to idle in silence`() =
        runViewModelTest {
            val env = Environment(outcome = LoginOutcome.Cancelled)
            val messages = mutableListOf<LoginViewModel.News>()
            val collection = launch { env.viewModel.news.toList(messages) }
            env.viewModel.onEvent(LoginViewModel.Event.PrimaryActionClicked)

            env.viewModel.onEvent(LoginViewModel.Event.ProviderChosen(AuthProvider.GOOGLE))
            advanceUntilIdle()

            assertEquals(expected = false, actual = env.viewModel.uiState.value.isLoggingIn)
            assertEquals(expected = false, actual = env.viewModel.uiState.value.isProviderSheetVisible)
            assertEquals(expected = emptyList(), actual = messages)
            collection.cancel()
        }

    @Test
    fun `GIVEN an attempt fails WHEN it concludes THEN exactly one message is emitted`() = runViewModelTest {
        val env = Environment(outcome = LoginOutcome.Failed)
        val messages = mutableListOf<LoginViewModel.News>()
        val collection = launch { env.viewModel.news.toList(messages) }

        env.viewModel.onEvent(LoginViewModel.Event.ProviderChosen(AuthProvider.GOOGLE))
        advanceUntilIdle()

        assertEquals(
            expected = listOf<LoginViewModel.News>(
                LoginViewModel.News.ShowMessage(Res.string.login_failed),
            ),
            actual = messages,
        )
        assertEquals(expected = false, actual = env.viewModel.uiState.value.isLoggingIn)
        collection.cancel()
    }

    @Test
    fun `GIVEN an attempt is in progress WHEN the primary action is activated again THEN the sheet stays closed`() =
        runViewModelTest {
            val env = Environment(isAttemptHeldOpen = true)
            env.viewModel.onEvent(LoginViewModel.Event.ProviderChosen(AuthProvider.GOOGLE))
            runCurrent()

            env.viewModel.onEvent(LoginViewModel.Event.PrimaryActionClicked)
            runCurrent()

            assertEquals(expected = false, actual = env.viewModel.uiState.value.isProviderSheetVisible)
            env.releaseAttempt()
            advanceUntilIdle()
            env.loginUseCase.invokeCall.called(times = 1)
        }

    @Test
    fun `GIVEN an attempt is in progress WHEN the same provider is chosen again THEN no second attempt starts`() =
        runViewModelTest {
            val env = Environment(isAttemptHeldOpen = true)
            env.viewModel.onEvent(LoginViewModel.Event.ProviderChosen(AuthProvider.GOOGLE))
            runCurrent()

            env.viewModel.onEvent(LoginViewModel.Event.ProviderChosen(AuthProvider.GOOGLE))
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
            env.viewModel.onEvent(LoginViewModel.Event.ProviderChosen(AuthProvider.GOOGLE))
            runCurrent()

            env.viewModel.onEvent(LoginViewModel.Event.ProviderChosen(AuthProvider.T_ID))
            runCurrent()

            assertEquals(expected = emptyList(), actual = messages)
            env.releaseAttempt()
            advanceUntilIdle()
            env.loginUseCase.invokeCall.called(times = 1)
            collection.cancel()
        }

    @Test
    fun `GIVEN the provider sheet is open WHEN it is dismissed THEN it is neither an error nor an attempt`() =
        runViewModelTest {
            val env = Environment()
            val messages = mutableListOf<LoginViewModel.News>()
            val collection = launch { env.viewModel.news.toList(messages) }
            env.viewModel.onEvent(LoginViewModel.Event.PrimaryActionClicked)
            runCurrent()

            env.viewModel.onEvent(LoginViewModel.Event.ProviderSheetDismissed)
            advanceUntilIdle()

            assertEquals(expected = false, actual = env.viewModel.uiState.value.isProviderSheetVisible)
            assertEquals(expected = false, actual = env.viewModel.uiState.value.isLoggingIn)
            assertEquals(expected = emptyList(), actual = messages)
            env.loginUseCase.invokeCall.notCalled()
            collection.cancel()
        }

    private class Environment(
        outcome: LoginOutcome = LoginOutcome.Success,
        declarations: List<AuthProviderDeclaration> = AuthProviderCatalog.DECLARATIONS,
        isAttemptHeldOpen: Boolean = false,
        loginPathFor: AuthProvider = AuthProvider.GOOGLE,
        platform: Platform = Platform.ANDROID,
    ) {

        private val gate = CompletableDeferred<Unit>().apply { if (!isAttemptHeldOpen) complete(Unit) }

        val loginUseCase = StubLoginUseCase(outcome = outcome, gate = gate)
        val viewModel = LoginViewModel(
            loginUseCases = mapOf<AuthProvider, LoginUseCase>(loginPathFor to loginUseCase),
            motionPreferences = MotionPreferences { false },
            platform = platform,
            privacyUrl = "https://yap.app/privacy",
            termsUrl = "https://yap.app/terms",
            declarations = declarations,
        )

        fun releaseAttempt() = gate.complete(Unit)
    }

    private companion object {
        val CHOSEN = AuthProvider.T_ID
    }
}
