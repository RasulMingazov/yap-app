package app.yap.feature.auth.data.identity

import app.yap.feature.auth.domain.entity.LoginProviderId

/**
 * The only place aware of PKCE mechanics and provider SDKs. It exposes no SDK type to domain or
 * presentation code (R-102).
 */
internal interface LoginProviderAdapter {

    val providerId: LoginProviderId

    /** Creates fresh, single-use attempt material before the backend challenge is requested. */
    suspend fun prepareAttempt(): PreparedAttempt

    /** Runs the provider flow for an already-prepared attempt bound to [challenge]. */
    suspend fun authenticate(attempt: PreparedAttempt, challenge: LoginChallenge): ProviderAuthResult

    /** Discards prepared material; idempotent and safe to call after any outcome. */
    fun discard(attempt: PreparedAttempt)
}
