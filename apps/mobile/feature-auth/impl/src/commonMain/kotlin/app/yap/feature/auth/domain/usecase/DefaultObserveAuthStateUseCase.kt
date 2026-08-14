package app.yap.feature.auth.domain.usecase

import app.yap.feature.auth.api.entity.AuthState
import app.yap.feature.auth.api.usecase.ObserveAuthStateUseCase
import app.yap.feature.auth.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow

internal class DefaultObserveAuthStateUseCase(
    private val authRepository: AuthRepository,
) : ObserveAuthStateUseCase {

    override fun invoke(): Flow<AuthState> = authRepository.observe()
}
