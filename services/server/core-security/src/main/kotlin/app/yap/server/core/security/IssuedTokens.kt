package app.yap.server.core.security

data class IssuedTokens(
    val accessToken: String,
    val refreshToken: String,
    val accessTokenExpiresAtEpochSeconds: Long,
)
