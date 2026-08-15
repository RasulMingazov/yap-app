package app.yap.server.feature.auth.model

internal data class AuthenticatedSession(
    val userId: String,
    val accessToken: String,
    val refreshToken: String,
    val accessTokenExpiresAtEpochSeconds: Long,
    val refreshTokenExpiresAtEpochSeconds: Long,
)
