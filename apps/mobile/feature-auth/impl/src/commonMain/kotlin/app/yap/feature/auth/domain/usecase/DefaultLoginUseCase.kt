package app.yap.feature.auth.domain.usecase

import app.yap.feature.auth.api.entity.AuthProvider
import app.yap.feature.auth.api.entity.LoginOutcome
import app.yap.feature.auth.api.usecase.LoginUseCase
import app.yap.feature.auth.domain.provider.ProviderLogin
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.withTimeoutOrNull

private val ATTEMPT_BOUND = 60.seconds

internal class DefaultLoginUseCase(
    providerLogins: List<ProviderLogin>,
) : LoginUseCase {

    private val loginsByType = providerLogins.associateBy(ProviderLogin::type)

    override suspend fun invoke(provider: AuthProvider): LoginOutcome {
        val providerLogin = loginsByType[provider.type]
            ?.takeIf { provider.isEnabled }
            ?: return LoginOutcome.Unavailable

        return withTimeoutOrNull(ATTEMPT_BOUND) { providerLogin.login() } ?: LoginOutcome.Cancelled
    }
}
