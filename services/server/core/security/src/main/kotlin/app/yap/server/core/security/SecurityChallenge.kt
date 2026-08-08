package app.yap.server.core.security

import java.time.Instant

data class SecurityChallenge(
    val id: String,
    val nonce: String,
    val expiresAt: Instant,
)
