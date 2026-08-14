package app.yap.feature.auth.data.local

import kotlinx.serialization.Serializable

@Serializable
internal data class SessionLocal(
    val accessToken: String,
    val refreshToken: String,
    val accessTokenExpiresAtEpochSeconds: Long,
    val refreshTokenExpiresAtEpochSeconds: Long,
)
