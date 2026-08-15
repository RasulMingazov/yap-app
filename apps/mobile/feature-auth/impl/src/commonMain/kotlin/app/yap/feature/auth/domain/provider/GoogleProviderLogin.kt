package app.yap.feature.auth.domain.provider

import app.yap.feature.auth.api.entity.AuthProviderType
import app.yap.feature.auth.api.entity.LoginOutcome
import app.yap.feature.auth.domain.repository.GoogleAuthRepository

internal class GoogleProviderLogin(
    private val googleAuthRepository: GoogleAuthRepository,
) : ProviderLogin {

    override val type: AuthProviderType = AuthProviderType.GOOGLE

    override suspend fun login(): LoginOutcome = googleAuthRepository.login()
}
