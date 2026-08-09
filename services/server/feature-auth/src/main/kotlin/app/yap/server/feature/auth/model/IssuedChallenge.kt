package app.yap.server.feature.auth.model

/** A freshly issued challenge. [nonce] is the raw value, disclosed to the client exactly once. */
internal data class IssuedChallenge(
    val challengeId: String,
    val expiresAtEpochSeconds: Long,
    val nonce: String?,
)
