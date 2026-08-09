package app.yap.server.feature.auth.model

import java.time.Instant

/**
 * A persisted, single-use login challenge.
 *
 * [nonceHash] is the SHA-256 hash of the issued nonce; the raw value is returned to the client once
 * and never stored. [proof] holds the exact base64url S256 code challenge supplied by a PKCE client
 * in the challenge request, or `null` for a flow without PKCE.
 */
internal data class AuthChallenge(
    val createdAt: Instant,
    val expiresAt: Instant,
    val id: String,
    val nonceHash: String?,
    val proof: String?,
    val provider: ProviderId,
)
