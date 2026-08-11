package app.yap.feature.auth.presentation.login

import app.yap.core.common.coroutines.CoroutineDispatchers
import app.yap.core.test.testDispatchers
import app.yap.feature.auth.domain.entity.LoginFailure
import app.yap.feature.auth.domain.entity.LoginOutcome
import app.yap.feature.auth.domain.entity.LoginProvider
import app.yap.feature.auth.domain.entity.LoginProviderId
import app.yap.feature.auth.domain.entity.StubLoginProvider
import app.yap.feature.auth.domain.usecase.StubLogInUseCase
import app.yap.feature.auth.domain.usecase.StubObserveLoginProvidersUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
internal class LoginViewModelTest {

    @Test
    fun `GIVEN a visible disabled provider WHEN it is selected THEN one coming soon message appears and no attempt starts`() =
        runTest {
            val env = Environment(dispatchers = testDispatchers())
            val news = collectNews(model = env.model)
            advanceUntilIdle()

            env.selectProvider(providerId = LoginProviderId.Apple)
            advanceUntilIdle()

            assertEquals(
                expected = listOf(StubLoginNews.stubComingSoonNews(displayName = StubLoginProvider.APPLE_DISPLAY_NAME)),
                actual = news,
            )
            env.logInUseCase.invokeCall.notCalled()
            assertEquals(expected = StubLoginUiState.stubLabelButton(), actual = env.model.uiState.value.button)
        }

    @Test
    fun `GIVEN an enabled provider WHEN it is selected THEN the login attempt runs for that provider`() = runTest {
        val env = Environment(dispatchers = testDispatchers())
        advanceUntilIdle()

        env.selectProvider(providerId = LoginProviderId.Google)
        advanceUntilIdle()

        env.logInUseCase.invokeCall.calledWith(LoginProviderId.Google)
    }

    @Test
    fun `GIVEN the login button is clicked WHEN the screen is idle THEN provider selection is requested`() = runTest {
        val env = Environment(dispatchers = testDispatchers())
        advanceUntilIdle()

        env.model.dispatch(LoginEvent.LoginClicked)
        advanceUntilIdle()

        assertEquals(expected = listOf<LoginOutput>(LoginOutput.OpenProviderSelection), actual = env.outputs)
    }

    @Test
    fun `GIVEN an unconfigured enabled provider WHEN the attempt fails THEN the temporarily unavailable message appears`() =
        runTest {
            val env = Environment(
                dispatchers = testDispatchers(),
                outcome = LoginOutcome.Failure(reason = LoginFailure.Configuration),
            )
            val news = collectNews(model = env.model)
            advanceUntilIdle()

            env.selectProvider(providerId = LoginProviderId.Google)
            advanceUntilIdle()

            assertEquals(
                expected = listOf(StubLoginNews.stubUnavailableNews(displayName = StubLoginProvider.GOOGLE_DISPLAY_NAME)),
                actual = news,
            )
        }

    @Test
    fun `GIVEN no connection WHEN the attempt fails THEN the connectivity message appears and loading ends`() = runTest {
        val env = Environment(
            dispatchers = testDispatchers(),
            outcome = LoginOutcome.Failure(reason = LoginFailure.Connectivity),
        )
        val news = collectNews(model = env.model)
        advanceUntilIdle()

        env.selectProvider(providerId = LoginProviderId.Google)
        advanceUntilIdle()

        assertEquals(expected = listOf(StubLoginNews.stubConnectivityFailureNews()), actual = news)
        assertEquals(expected = StubLoginUiState.stubLabelButton(), actual = env.model.uiState.value.button)
    }

    @Test
    fun `GIVEN a recoverable provider failure WHEN the attempt fails THEN the retry message names the provider`() =
        runTest {
            val env = Environment(
                dispatchers = testDispatchers(),
                outcome = LoginOutcome.Failure(reason = LoginFailure.Provider),
            )
            val news = collectNews(model = env.model)
            advanceUntilIdle()

            env.selectProvider(providerId = LoginProviderId.Google)
            advanceUntilIdle()

            assertEquals(
                expected = listOf(
                    StubLoginNews.stubProviderFailureNews(displayName = StubLoginProvider.GOOGLE_DISPLAY_NAME),
                ),
                actual = news,
            )
        }

    @Test
    fun `GIVEN the user cancels the provider flow WHEN the attempt ends THEN no message appears and loading ends`() =
        runTest {
            val env = Environment(dispatchers = testDispatchers(), outcome = LoginOutcome.Cancelled)
            val news = collectNews(model = env.model)
            advanceUntilIdle()

            env.selectProvider(providerId = LoginProviderId.Google)
            advanceUntilIdle()

            assertEquals(expected = emptyList(), actual = news)
            assertEquals(expected = StubLoginUiState.stubLabelButton(), actual = env.model.uiState.value.button)
        }

    @Test
    fun `GIVEN a failed attempt WHEN the same provider is selected again THEN a second attempt starts`() = runTest {
        val env = Environment(
            dispatchers = testDispatchers(),
            outcome = LoginOutcome.Failure(reason = LoginFailure.Provider),
        )
        advanceUntilIdle()

        env.selectProvider(providerId = LoginProviderId.Google)
        advanceUntilIdle()
        env.selectProvider(providerId = LoginProviderId.Google)
        advanceUntilIdle()

        env.logInUseCase.invokeCall.called(times = 2)
    }

    @Test
    fun `GIVEN a running attempt WHEN the provider is selected again THEN at most one attempt is started`() = runTest {
        val env = Environment(dispatchers = testDispatchers())
        advanceUntilIdle()

        env.selectProvider(providerId = LoginProviderId.Google)
        env.selectProvider(providerId = LoginProviderId.Google)
        advanceUntilIdle()

        env.logInUseCase.invokeCall.called(times = 1)
    }

    private class Environment(
        dispatchers: CoroutineDispatchers,
        outcome: LoginOutcome = LoginOutcome.Cancelled,
        providers: List<LoginProvider> = StubLoginProvider.stubIosProviders(),
    ) {

        val logInUseCase = StubLogInUseCase(outcome = outcome)
        val observeLoginProvidersUseCase = StubObserveLoginProvidersUseCase(providers = providers)
        val outputs = mutableListOf<LoginOutput>()
        val model: LoginViewModel = LoginViewModel.Factory(
            coroutineDispatchers = dispatchers,
            logInUseCase = logInUseCase,
            observeLoginProvidersUseCase = observeLoginProvidersUseCase,
        ).invoke(output = outputs::add)

        fun selectProvider(providerId: LoginProviderId) {
            model.dispatch(LoginEvent.ProviderSelected(providerId = providerId))
        }
    }
}

/** Returns a list that keeps receiving the view model's one-shot news for the rest of the test. */
@OptIn(ExperimentalCoroutinesApi::class)
private fun TestScope.collectNews(model: LoginViewModel): List<LoginNews> {
    val news = mutableListOf<LoginNews>()
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.news.toList(news) }
    return news
}
