package app.yap.feature.auth.domain.provider

import app.yap.feature.auth.api.entity.AuthProviderType
import app.yap.feature.auth.api.entity.LoginOutcome

internal interface ProviderLogin {

    val type: AuthProviderType

    suspend fun login(): LoginOutcome
}
