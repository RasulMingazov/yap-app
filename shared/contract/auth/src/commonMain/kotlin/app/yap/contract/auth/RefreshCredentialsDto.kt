package app.yap.contract.auth

import kotlinx.serialization.Serializable

@Serializable
data class RefreshCredentialsDto(
    val refreshToken: String,
)
