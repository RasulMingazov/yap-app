package app.yap.contract.auth

import kotlinx.serialization.Serializable

@Serializable
data class SessionDto(
    val accessToken: String,
    val refreshToken: String,
    val accessTokenExpiresAtEpochSeconds: Long,
    val refreshTokenExpiresAtEpochSeconds: Long,
)
