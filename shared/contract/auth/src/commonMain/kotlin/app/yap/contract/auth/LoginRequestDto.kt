package app.yap.contract.auth

import kotlinx.serialization.Serializable

/**
 * Submits a provider result for an already-issued challenge.
 *
 * [credentialType] is `"identity_token"` or `"authorization_code"` and decides which of the
 * remaining fields are required. There is deliberately no nonce field: the server never treats a
 * client-echoed nonce as evidence and compares only the verified provider claim with its stored
 * hash.
 */
@Serializable
data class LoginRequestDto(
    val challengeId: String,
    val provider: String,
    val credentialType: String,
    val idToken: String? = null,
    val authorizationCode: String? = null,
    val codeVerifier: String? = null,
    val redirectUri: String? = null,
)
