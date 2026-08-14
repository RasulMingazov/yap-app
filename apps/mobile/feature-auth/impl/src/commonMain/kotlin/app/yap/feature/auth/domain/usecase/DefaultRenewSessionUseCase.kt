package app.yap.feature.auth.domain.usecase

import app.yap.feature.auth.api.usecase.RenewSessionUseCase
import app.yap.feature.auth.domain.repository.AuthRepository

private const val RENEWAL_MARGIN_SECONDS = 300L

internal class DefaultRenewSessionUseCase(
    private val authRepository: AuthRepository,
) : RenewSessionUseCase {

    override suspend fun invoke() {
        val lifetimeSeconds = authRepository.accessTokenLifetimeSeconds() ?: return
        if (lifetimeSeconds > RENEWAL_MARGIN_SECONDS) return

        authRepository.renewSession()
    }
}
