package app.yap.feature.auth.domain.usecase

import app.yap.feature.auth.domain.entity.Session
import app.yap.feature.auth.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow

internal interface ObserveSessionUseCase {

    operator fun invoke(): Flow<Session?>
}

internal class DefaultObserveSessionUseCase(
    private val sessionRepository: SessionRepository,
) : ObserveSessionUseCase {

    override fun invoke(): Flow<Session?> = sessionRepository.observe()
}
