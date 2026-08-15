package app.yap.feature.auth.domain.usecase

import app.yap.feature.auth.api.usecase.RefreshSessionUseCase
import app.yap.feature.auth.domain.repository.AuthSessionRepository

internal class DefaultRefreshSessionUseCase(
    private val authSessionRepository: AuthSessionRepository,
) : RefreshSessionUseCase {

    override suspend fun invoke() = authSessionRepository.refresh()
}
