package app.yap.server.feature.auth.model

data class AuthenticatedSession(
    val userId: String,
    val accessToken: String,
    val refreshToken: String,
    val accessTokenExpiresAtEpochSeconds: Long,
    val refreshTokenExpiresAtEpochSeconds: Long,
)
