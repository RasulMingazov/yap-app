package app.yap.server.feature.auth

import app.yap.server.core.security.SecurityChallenge
import app.yap.server.feature.auth.model.AuthChallenge
import app.yap.server.feature.auth.model.ProviderId
import java.time.Instant

internal object StubAuthChallenge {

    const val CHALLENGE_ID = "22222222-2222-2222-2222-222222222222"
    const val NONCE = "nonce-value"

    /** The RFC 7636 example pair, so an expected S256 value never comes from production code. */
    const val CODE_CHALLENGE = "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM"
    const val CODE_VERIFIER = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"

    /** Five minutes, the challenge lifetime the server issues. */
    const val TTL_SECONDS = 300L

    val EXPIRES_AT: Instant = StubAuth.NOW.plusSeconds(TTL_SECONDS)

    fun stubAuthChallenge(
        createdAt: Instant = StubAuth.NOW,
        expiresAt: Instant = EXPIRES_AT,
        id: String = CHALLENGE_ID,
        nonceHash: String? = StubAuth.HASH,
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

    fun stubSecurityChallenge(
        expiresAt: Instant = EXPIRES_AT,
        id: String = CHALLENGE_ID,
        nonce: String = NONCE,
    ): SecurityChallenge = SecurityChallenge(
        id = id,
        nonce = nonce,
        expiresAt = expiresAt,
    )
}
