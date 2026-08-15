package app.yap.feature.auth.domain.usecase

import app.yap.feature.auth.api.entity.AuthSessionState
import app.yap.feature.auth.api.usecase.ObserveAuthSessionStateUseCase
import app.yap.feature.auth.domain.repository.AuthSessionRepository
import kotlinx.coroutines.flow.Flow

internal class DefaultObserveAuthSessionStateUseCase(
    private val authSessionRepository: AuthSessionRepository,
) : ObserveAuthSessionStateUseCase {

    override fun invoke(): Flow<AuthSessionState> = authSessionRepository.observe()
}
