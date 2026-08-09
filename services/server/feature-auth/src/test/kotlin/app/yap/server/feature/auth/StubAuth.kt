package app.yap.server.feature.auth

import java.time.Instant

/**
 * The few primitives every authentication value shares. Each value owns its own constants and
 * builders: see [StubAuthAccount], [StubAuthChallenge], [StubAuthSession], [StubLoginCredential],
 * and [StubVerifiedIdentity].
 */
internal object StubAuth {

    /** The single value [StubTokenService] hashes to, so a stored hash is never a raw value. */
    const val HASH = "hashed-value"

    /** The provider identifier as it arrives on the wire. */
    const val PROVIDER = "google"

    val NOW: Instant = Instant.parse("2026-08-09T12:00:00Z")
}
