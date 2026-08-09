package app.yap.server.feature.auth.model

/**
 * A provider result submitted for an already-issued challenge. There is deliberately no
 * client-echoed nonce: the nonce is evidence only when it arrives inside a verified provider token.
 */
internal sealed interface LoginCredential {

    data class AuthorizationCode(
        val code: String,
        val codeVerifier: String,
        val redirectUri: String,
    ) : LoginCredential

    data class IdentityToken(val idToken: String) : LoginCredential
}
