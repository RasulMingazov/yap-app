package app.yap.feature.auth.domain.provider

import app.yap.feature.auth.api.entity.AuthProvider
import app.yap.feature.auth.api.entity.LoginOutcome
import app.yap.feature.auth.domain.repository.AuthRepository
import kotlin.reflect.KClass

internal class GoogleProviderLogin(
    private val authRepository: AuthRepository,
) : ProviderLogin {

    override val provider: KClass<out AuthProvider> = AuthProvider.Google::class

    override suspend fun login(): LoginOutcome = authRepository.loginWithGoogle()
}
