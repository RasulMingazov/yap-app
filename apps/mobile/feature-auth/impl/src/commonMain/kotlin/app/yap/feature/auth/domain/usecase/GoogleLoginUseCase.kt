package app.yap.feature.auth.domain.usecase

import app.yap.feature.auth.api.entity.LoginOutcome
import app.yap.feature.auth.api.usecase.LoginUseCase
import app.yap.feature.auth.domain.repository.AuthRepository
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.withTimeoutOrNull

private val ATTEMPT_BOUND = 60.seconds

internal class GoogleLoginUseCase(
    private val authRepository: AuthRepository,
) : LoginUseCase {

    override suspend fun invoke(): LoginOutcome =
        withTimeoutOrNull(ATTEMPT_BOUND) { authRepository.loginWithGoogle() } ?: LoginOutcome.Cancelled
}
