package app.yap.server.feature.auth.persistence

import app.yap.server.feature.auth.model.AuthChallenge
import app.yap.server.feature.auth.model.NewSession
import app.yap.server.feature.auth.model.ProviderId
import app.yap.server.feature.auth.model.SessionRotation
import app.yap.server.feature.auth.model.VerifiedIdentity
import java.time.Duration
import java.time.Instant
import java.util.UUID

/** The persisted authentication values these scenarios store and read back. */
internal object StubAuthRow {

    const val EMAIL = "user@example.com"
    const val NONCE_HASH = "nonce-hash"
    const val REFRESH_TOKEN_HASH = "refresh-token-hash"
    const val ROTATED_TOKEN_HASH = "rotated-token-hash"
    const val SUBJECT = "google-subject"

    val ABSOLUTE_LIFETIME: Duration = Duration.ofDays(180)
    val CHALLENGE_TTL: Duration = Duration.ofMinutes(5)
    val INACTIVITY_LIMIT: Duration = Duration.ofDays(30)

    fun stubAuthChallenge(
        createdAt: Instant = AuthDatabase.NOW,
        expiresAt: Instant = AuthDatabase.NOW.plus(CHALLENGE_TTL),
        id: String = identifier(),
        nonceHash: String? = NONCE_HASH,
        proof: String? = null,
        provider: ProviderId = ProviderId.Google,
    ): AuthChallenge = AuthChallenge(
        createdAt = createdAt,
        expiresAt = expiresAt,
        id = id,
        nonceHash = nonceHash,
        proof = proof,
        provider = provider,
    )

    fun stubNewSession(
        absoluteLifetime: Duration = ABSOLUTE_LIFETIME,
        id: String = identifier(),
        refreshTokenHash: String = REFRESH_TOKEN_HASH,
    ): NewSession = NewSession(
        absoluteLifetime = absoluteLifetime,
        id = id,
        refreshTokenHash = refreshTokenHash,
    )

    fun stubSessionRotation(
        sessionId: String,
        inactivityLimit: Duration = INACTIVITY_LIMIT,
        presentedTokenHash: String = REFRESH_TOKEN_HASH,
        rotatedTokenHash: String = ROTATED_TOKEN_HASH,
    ): SessionRotation = SessionRotation(
        inactivityLimit = inactivityLimit,
        presentedTokenHash = presentedTokenHash,
        rotatedTokenHash = rotatedTokenHash,
        sessionId = sessionId,
    )

    fun stubVerifiedIdentity(
        email: String? = EMAIL,
        isEmailVerified: Boolean? = true,
        provider: ProviderId = ProviderId.Google,
        subject: String = SUBJECT,
    ): VerifiedIdentity = VerifiedIdentity(
        email = email,
        isEmailVerified = isEmailVerified,
        provider = provider,
        subject = subject,
    )

    fun identifier(): String = UUID.randomUUID().toString()
}
