package app.yap.server.feature.auth.persistence

import app.yap.server.feature.auth.identity.GoogleIdentity
import java.time.Instant

internal data class PersistedSession(
    val sessionId: String,
    val userId: String,
    val refreshTokenHash: String,
    val expiresAt: Instant,
)

internal data class SessionRotation(
    val sessionId: String,
    val presentedRefreshTokenHash: String,
    val refreshTokenHash: String,
    val expiresAt: Instant,
)

internal interface AuthPersistence {

    fun createSession(session: PersistedSession)

    fun resolveOrCreateUserId(identity: GoogleIdentity): String

    fun rotateSession(rotation: SessionRotation): String?
}
