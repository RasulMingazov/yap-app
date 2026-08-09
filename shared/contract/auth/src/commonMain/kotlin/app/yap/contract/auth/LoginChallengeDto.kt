package app.yap.contract.auth

import kotlinx.serialization.Serializable

/**
 * A freshly issued, single-use login challenge.
 *
 * [challengeId] is opaque and cryptographically random. [nonce] is the raw value returned once for
 * nonce-bound providers; the server stores only its hash and never accepts it back from the client.
 */
@Serializable
data class LoginChallengeDto(
    val challengeId: String,
    val nonce: String? = null,
    val expiresAtEpochSeconds: Long,
)
