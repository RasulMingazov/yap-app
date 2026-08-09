package app.yap.contract.auth

import kotlinx.serialization.Serializable

/**
 * Requests a fresh, single-use login challenge for [provider].
 *
 * [codeChallenge] is the public base64url S256 value of a client-generated `code_verifier`, sent
 * without padding and only by PKCE-capable attempts. The server persists it verbatim as the
 * challenge proof. [codeChallengeMethod] is `"S256"` exactly when [codeChallenge] is present.
 */
@Serializable
data class LoginChallengeRequestDto(
    val provider: String,
    val codeChallenge: String? = null,
    val codeChallengeMethod: String? = null,
)
