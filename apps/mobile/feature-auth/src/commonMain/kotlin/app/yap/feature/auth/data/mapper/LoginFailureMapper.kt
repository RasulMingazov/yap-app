package app.yap.feature.auth.data.mapper

import app.yap.feature.auth.data.identity.ProviderFailureKind
import app.yap.feature.auth.data.remote.AuthApiFailureKind
import app.yap.feature.auth.domain.entity.LoginFailure

internal fun AuthApiFailureKind.toDomain(): LoginFailure = when (this) {
    AuthApiFailureKind.Rejected -> LoginFailure.Provider
    AuthApiFailureKind.Unavailable -> LoginFailure.Connectivity
}

/**
 * A scaffolded provider reports the same coarse reason as missing configuration, so no caller can
 * tell which providers are scaffolded (R-104, AC-030).
 */
internal fun ProviderFailureKind.toDomain(): LoginFailure = when (this) {
    ProviderFailureKind.Configuration -> LoginFailure.Configuration
    ProviderFailureKind.Connectivity -> LoginFailure.Connectivity
    ProviderFailureKind.IntegrationNotConfigured -> LoginFailure.Configuration
    ProviderFailureKind.Provider -> LoginFailure.Provider
}
