package app.yap.server.feature.auth

import app.yap.server.feature.auth.identity.GoogleIdentity
import app.yap.server.feature.auth.persistence.AuthPersistence
import app.yap.server.feature.auth.persistence.PersistedSession
import app.yap.server.feature.auth.persistence.SessionRotation

internal class StubAuthPersistence(
    private val userId: String = "user-1",
    private val rotatedUserId: String? = "user-1",
) : AuthPersistence {

    val resolvedIdentities = mutableListOf<GoogleIdentity>()
    val createdSessions = mutableListOf<PersistedSession>()
    val rotations = mutableListOf<SessionRotation>()

    override fun createSession(session: PersistedSession) {
        createdSessions += session
    }

    override fun resolveOrCreateUserId(identity: GoogleIdentity): String {
        resolvedIdentities += identity
        return userId
    }

    override fun rotateSession(rotation: SessionRotation): String? {
        rotations += rotation
        return rotatedUserId
    }
}
