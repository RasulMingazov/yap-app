package app.yap.feature.auth.domain.usecase

import app.yap.feature.auth.domain.repository.StubAuthSessionRepository
import kotlin.test.Test
import kotlinx.coroutines.test.runTest

internal class DefaultRefreshSessionUseCaseTest {

    @Test
    fun `GIVEN a launch WHEN the use case runs THEN it asks the repository to refresh exactly once`() = runTest {
        val authSessionRepository = StubAuthSessionRepository()
        val useCase = DefaultRefreshSessionUseCase(authSessionRepository = authSessionRepository)

        useCase()

        authSessionRepository.refreshCall.called(times = 1)
    }
}
