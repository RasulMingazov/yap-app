package app.yap.feature.auth.domain.usecase

import app.yap.feature.auth.api.entity.AuthProvider
import app.yap.feature.auth.api.entity.LoginOutcome
import app.yap.feature.auth.domain.provider.StubProviderLogin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

internal class DefaultLoginUseCaseTest {

    @Test
    fun `GIVEN a registered provider WHEN it is chosen THEN its own handler runs`() = runTest {
        val env = Environment()

        val outcome = env.loginUseCase(ENABLED_GOOGLE)

        assertEquals(expected = LoginOutcome.Success, actual = outcome)
        env.googleLogin.loginCall.called(times = 1)
    }

    @Test
    fun `GIVEN a provider with no registered handler WHEN it is chosen THEN nothing runs and it is unavailable`() =
        runTest {
            val env = Environment()

            val outcome = env.loginUseCase(AuthProvider.TId(isEnabled = true, isVisible = true))

            assertEquals(expected = LoginOutcome.Unavailable, actual = outcome)
            env.googleLogin.loginCall.notCalled()
        }

    @Test
    fun `GIVEN a registered provider the roster disables WHEN it is chosen THEN its handler is left alone`() = runTest {
        val env = Environment()

        val outcome = env.loginUseCase(AuthProvider.Google(isEnabled = false, isVisible = true))

        assertEquals(expected = LoginOutcome.Unavailable, actual = outcome)
        env.googleLogin.loginCall.notCalled()
    }

    @Test
    fun `GIVEN a handler still unresolved after 60 seconds WHEN the bound expires THEN it reads as cancelled`() =
        runTest {
            val env = Environment(isAttemptHeldOpen = true)

            val outcome = async { env.loginUseCase(ENABLED_GOOGLE) }
            advanceTimeBy(ATTEMPT_BOUND)
            runCurrent()

            assertEquals(expected = LoginOutcome.Cancelled, actual = outcome.await())
        }

    @Test
    fun `GIVEN a handler that answers inside the bound WHEN it does THEN its own outcome stands`() = runTest {
        val env = Environment(isAttemptHeldOpen = true, outcome = LoginOutcome.Failed)

        val outcome = async { env.loginUseCase(ENABLED_GOOGLE) }
        advanceTimeBy(ATTEMPT_BOUND - 1.seconds)
        env.releaseAttempt()
        runCurrent()

        assertEquals(expected = LoginOutcome.Failed, actual = outcome.await())
    }

    private class Environment(
        isAttemptHeldOpen: Boolean = false,
        outcome: LoginOutcome = LoginOutcome.Success,
    ) {

        private val gate = CompletableDeferred<Unit>().apply { if (!isAttemptHeldOpen) complete(Unit) }

        val googleLogin = StubProviderLogin(
            provider = AuthProvider.Google::class,
            outcome = outcome,
            gate = gate,
        )
        val loginUseCase = DefaultLoginUseCase(providerLogins = listOf(googleLogin))

        fun releaseAttempt() = gate.complete(Unit)
    }

    private companion object {
        val ATTEMPT_BOUND = 60.seconds
        val ENABLED_GOOGLE = AuthProvider.Google(isEnabled = true, isVisible = true)
    }
}
