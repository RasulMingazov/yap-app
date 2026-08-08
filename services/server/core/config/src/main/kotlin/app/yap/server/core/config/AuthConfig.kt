package app.yap.server.core.config

/**
 * Generic JWT issuance settings owned by core infrastructure. Identity-provider-specific
 * settings (Google/Apple client IDs) belong to the feature-auth module that consumes them.
 */
data class AuthConfig(
    val jwtSecret: String,
    val jwtIssuer: String,
    val jwtAudience: String,
    val accessTokenTtlSeconds: Long,
    val refreshTokenTtlSeconds: Long,
) {
    init {
        require(jwtSecret.length >= MIN_JWT_SECRET_LENGTH) {
            "JWT_SECRET must contain at least $MIN_JWT_SECRET_LENGTH characters"
        }
        require(jwtSecret !in FORBIDDEN_JWT_SECRETS) { "JWT_SECRET must be generated securely" }
        require(jwtIssuer.isNotBlank() && jwtIssuer.length <= MAX_JWT_CLAIM_LENGTH) { "JWT_ISSUER is invalid" }
        require(jwtAudience.isNotBlank() && jwtAudience.length <= MAX_JWT_CLAIM_LENGTH) { "JWT_AUDIENCE is invalid" }
        require(accessTokenTtlSeconds in MIN_ACCESS_TOKEN_TTL_SECONDS..MAX_ACCESS_TOKEN_TTL_SECONDS) {
            "ACCESS_TOKEN_TTL_SECONDS must be between $MIN_ACCESS_TOKEN_TTL_SECONDS and $MAX_ACCESS_TOKEN_TTL_SECONDS"
        }
        require(refreshTokenTtlSeconds in MIN_REFRESH_TOKEN_TTL_SECONDS..MAX_REFRESH_TOKEN_TTL_SECONDS) {
            "REFRESH_TOKEN_TTL_SECONDS must be between " +
                "$MIN_REFRESH_TOKEN_TTL_SECONDS and $MAX_REFRESH_TOKEN_TTL_SECONDS"
        }
        require(refreshTokenTtlSeconds > accessTokenTtlSeconds) {
            "Refresh token TTL must exceed access token TTL"
        }
    }

    private companion object {
        const val MIN_JWT_SECRET_LENGTH = 43
        const val MAX_JWT_CLAIM_LENGTH = 255
        const val MIN_ACCESS_TOKEN_TTL_SECONDS = 60L
        const val MAX_ACCESS_TOKEN_TTL_SECONDS = 3_600L
        const val MIN_REFRESH_TOKEN_TTL_SECONDS = 3_600L
        const val MAX_REFRESH_TOKEN_TTL_SECONDS = 31_536_000L
        val FORBIDDEN_JWT_SECRETS = setOf(
            "change-me-in-production",
            "local-development-secret-change-before-production",
            "replace-with-a-long-random-secret",
        )
    }
}
