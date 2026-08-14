package app.yap.contract.auth

import kotlinx.serialization.Serializable

@Serializable
data class GoogleAuthorizationCodeDto(
    val code: String,
    val codeVerifier: String,
    val redirectUri: String,
)
