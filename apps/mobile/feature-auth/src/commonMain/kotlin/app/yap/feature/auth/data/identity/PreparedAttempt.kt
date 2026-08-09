package app.yap.feature.auth.data.identity

/**
 * Single-use material created before the backend challenge is requested (R-096). Only
 * [codeChallenge] leaves the device in the challenge request; [codeVerifier] stays in the data
 * layer until it is submitted with the authorization code (R-097, R-102).
 */
internal class PreparedAttempt internal constructor(
    val attemptId: String,
    val codeChallenge: String?,
    internal val codeVerifier: String?,
)
