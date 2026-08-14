package app.yap.feature.auth.domain.usecase

import app.yap.feature.auth.domain.repository.StubAuthRepository
import kotlin.test.Test
import kotlinx.coroutines.test.runTest

internal class DefaultRenewSessionUseCaseTest {

    @Test
    fun `GIVEN the access token has already expired WHEN renewal runs THEN exactly one renewal is made`() = runTest {
        val env = Environment(accessTokenLifetimeSeconds = -1L)

        env.useCase()

        env.authRepository.renewSessionCall.called(times = 1)
    }

    @Test
    fun `GIVEN the access token expires inside the margin WHEN renewal runs THEN exactly one renewal is made`() =
        runTest {
            val env = Environment(accessTokenLifetimeSeconds = MARGIN_SECONDS)

            env.useCase()

            env.authRepository.renewSessionCall.called(times = 1)
        }

    @Test
    fun `GIVEN the access token expires beyond the margin WHEN renewal runs THEN no renewal is made`() = runTest {
        val env = Environment(accessTokenLifetimeSeconds = MARGIN_SECONDS + 1)

        env.useCase()

        env.authRepository.renewSessionCall.notCalled()
    }

    @Test
    fun `GIVEN a healthy access token WHEN renewal runs THEN no renewal is made`() = runTest {
        val env = Environment(accessTokenLifetimeSeconds = ACCESS_TOKEN_TTL_SECONDS)

        env.useCase()

        env.authRepository.renewSessionCall.notCalled()
    }

    @Test
    fun `GIVEN no stored session WHEN renewal runs THEN no renewal is made`() = runTest {
        val env = Environment(accessTokenLifetimeSeconds = null)

        env.useCase()

        env.authRepository.renewSessionCall.notCalled()
    }

    @Test
    fun `GIVEN a renewal is due WHEN it runs THEN the use case decides only whether and not what follows`() = runTest {
        val env = Environment(accessTokenLifetimeSeconds = -1L)

        env.useCase()

        env.authRepository.renewSessionCall.called(times = 1)
        env.authRepository.loginWithGoogleCall.notCalled()
    }

    private class Environment(
        accessTokenLifetimeSeconds: Long?,
    ) {

        val authRepository = StubAuthRepository(accessTokenLifetimeSeconds = accessTokenLifetimeSeconds)
        val useCase = DefaultRenewSessionUseCase(authRepository = authRepository)
    }

    private companion object {
        const val MARGIN_SECONDS = 300L
        const val ACCESS_TOKEN_TTL_SECONDS = 900L
    }
}
