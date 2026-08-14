package app.yap.contract.auth

import kotlinx.serialization.Serializable

@Serializable
data class GoogleCredentialsDto(
    val idToken: String,
    val nonce: String,
)
