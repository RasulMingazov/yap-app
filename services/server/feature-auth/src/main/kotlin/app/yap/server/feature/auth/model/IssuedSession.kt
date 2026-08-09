package app.yap.server.feature.auth.model

/**
 * Yap session credentials returned after a successful login. They are produced only once the login
 * transaction has committed, so a rolled-back attempt discloses nothing.
 */
internal data class IssuedSession(
    val accessToken: String,
    val accessTokenExpiresAtEpochSeconds: Long,
    val accountId: String,
    val refreshToken: String,
)
