package app.yap.server.feature.auth.model

import java.time.Duration

/**
 * The session row created by a successful login. Only [refreshTokenHash] is persisted, never the
 * refresh credential itself.
 *
 * The session carries its [absoluteLifetime] rather than precomputed instants, so its timestamps
 * are anchored to the time the login transaction reads while it holds the challenge lock.
 */
internal data class NewSession(
    val absoluteLifetime: Duration,
    val id: String,
    val refreshTokenHash: String,
)
