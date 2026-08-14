package app.yap.feature.auth.domain.usecase

import app.yap.feature.auth.api.entity.AuthState
import app.yap.feature.auth.api.entity.LoginOutcome
import app.yap.feature.auth.domain.repository.AuthRepository
import app.yap.feature.auth.domain.repository.StubAuthRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

internal class GoogleLoginUseCaseTest {

    @Test
    fun `GIVEN a provider call still unresolved after 60 seconds WHEN the bound expires THEN it reads as cancelled`() =
        runTest {
            val authRepository = HangingAuthRepository()
            val useCase = GoogleLoginUseCase(authRepository = authRepository)

            val outcome = async { useCase() }
            advanceTimeBy(BOUND)
            runCurrent()

            assertEquals(expected = LoginOutcome.Cancelled, actual = outcome.await())
        }

    @Test
    fun `GIVEN a provider call still unresolved after 60 seconds WHEN the bound expires THEN the call is stopped`() =
        runTest {
            val authRepository = HangingAuthRepository()
            val useCase = GoogleLoginUseCase(authRepository = authRepository)

            val outcome = async { useCase() }
            advanceTimeBy(BOUND)
            runCurrent()
            outcome.await()

            assertTrue(
                actual = authRepository.wasCancelled,
                message = "the provider call was left running after the attempt ended",
            )
        }

    @Test
    fun `GIVEN a provider call that answers inside the bound WHEN it does THEN its own outcome stands`() = runTest {
        val authRepository = HangingAuthRepository()
        val useCase = GoogleLoginUseCase(authRepository = authRepository)

        val outcome = async { useCase() }
        advanceTimeBy(BOUND - POLL_INTERVAL * 10)
        authRepository.answer(LoginOutcome.Failed)
        advanceTimeBy(POLL_INTERVAL)
        runCurrent()

        assertEquals(expected = LoginOutcome.Failed, actual = outcome.await())
    }

    @Test
    fun `GIVEN a provider call that answers immediately WHEN it concludes THEN the bound changes nothing`() =
        runTest {
            val authRepository = StubAuthRepository(outcome = LoginOutcome.Success)
            val useCase = GoogleLoginUseCase(authRepository = authRepository)

            assertEquals(expected = LoginOutcome.Success, actual = useCase())
        }

    private class HangingAuthRepository : AuthRepository {

        var wasCancelled = false
            private set

        private var answer: LoginOutcome? = null

        fun answer(outcome: LoginOutcome) {
            answer = outcome
        }

        override fun observe(): Flow<AuthState> = flowOf(AuthState.Unknown)

        override suspend fun loginWithGoogle(): LoginOutcome = try {
            while (answer == null) {
                kotlinx.coroutines.delay(POLL_INTERVAL)
            }
            requireNotNull(answer)
        } catch (cancellation: CancellationException) {
            wasCancelled = true
            throw cancellation
        }

        override suspend fun accessTokenLifetimeSeconds(): Long? = null

        override suspend fun renewSession() = awaitCancellation()
    }

    private companion object {
        val BOUND = 60.seconds
        val POLL_INTERVAL = 1.seconds
    }
}
