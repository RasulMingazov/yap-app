package app.yap.feature.auth.domain.usecase

import app.yap.feature.auth.domain.entity.Session
import app.yap.feature.auth.domain.repository.SessionRepository

internal interface RestoreSessionUseCase {

    suspend operator fun invoke(): Session?
}

internal class DefaultRestoreSessionUseCase(
    private val sessionRepository: SessionRepository,
) : RestoreSessionUseCase {

    override suspend fun invoke(): Session? = sessionRepository.get(forceUpdate = true)
}
