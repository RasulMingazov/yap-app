package app.yap.feature.auth.data.identity

internal data class LoginChallenge(
    val challengeId: String,
    val expiresAtEpochSeconds: Long,
    val nonce: String?,
)
