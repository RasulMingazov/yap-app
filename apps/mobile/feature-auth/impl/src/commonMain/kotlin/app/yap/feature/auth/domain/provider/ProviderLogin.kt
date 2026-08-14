package app.yap.feature.auth.domain.provider

import app.yap.feature.auth.api.entity.AuthProvider
import app.yap.feature.auth.api.entity.LoginOutcome
import kotlin.reflect.KClass

internal interface ProviderLogin {

    val provider: KClass<out AuthProvider>

    suspend fun login(): LoginOutcome
}
