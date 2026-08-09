package app.yap.feature.auth.domain.usecase

import app.yap.feature.auth.domain.entity.LoginOutcome
import app.yap.feature.auth.domain.entity.LoginProviderId
import app.yap.feature.auth.domain.repository.SessionRepository

internal interface LogInUseCase {

    suspend operator fun invoke(providerId: LoginProviderId): LoginOutcome
}

internal class DefaultLogInUseCase(
    private val sessionRepository: SessionRepository,
) : LogInUseCase {

    override suspend fun invoke(providerId: LoginProviderId): LoginOutcome =
        sessionRepository.logIn(providerId = providerId)
}
