package app.yap.server.feature.auth.model

import java.time.Duration

/**
 * One refresh attempt, expressed entirely in hashes: neither the presented nor the rotated
 * credential ever reaches persistence.
 *
 * [inactivityLimit] is the configured refresh lifetime rather than a deadline instant, so the
 * transaction judges inactivity by the clock it reads while it holds the session lock.
 */
internal data class SessionRotation(
    val inactivityLimit: Duration,
    val presentedTokenHash: String,
    val rotatedTokenHash: String,
    val sessionId: String,
)
