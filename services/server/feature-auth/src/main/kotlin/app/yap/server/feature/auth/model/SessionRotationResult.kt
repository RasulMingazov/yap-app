package app.yap.server.feature.auth.model

/**
 * The outcome of a [SessionRotation], decided while the session row is locked.
 *
 * The outcomes are distinguished by what the locked row actually contains, never by how many rows a
 * conditional update happened to affect: an unknown credential is [Unknown], and only a credential
 * equal to the stored previous hash is [Replayed]. Every outcome other than [Rotated] is the same
 * opaque rejection to the client.
 */
internal sealed interface SessionRotationResult {

    /** The presented credential was the current one; the session now holds the rotated hash. */
    data class Rotated(val accountId: String) : SessionRotationResult

    /** The session was revoked, absolutely expired, or unused past its inactivity limit. */
    data object Expired : SessionRotationResult

    /** The presented credential was a previously rotated one, so the whole session was revoked. */
    data object Replayed : SessionRotationResult

    /** No such session exists, or the credential was never issued for it. */
    data object Unknown : SessionRotationResult
}
