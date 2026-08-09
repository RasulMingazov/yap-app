package app.yap.server.feature.auth.model

/**
 * The identity proven by a verified provider result. It is derived only from an active, correctly
 * scoped, signature-verified credential bound to the current challenge — never from a locally
 * decoded token.
 */
internal data class VerifiedIdentity(
    val email: String?,
    val isEmailVerified: Boolean?,
    val provider: ProviderId,
    val subject: String,
)
