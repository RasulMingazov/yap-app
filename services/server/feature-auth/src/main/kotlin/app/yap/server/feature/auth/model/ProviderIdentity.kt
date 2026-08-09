package app.yap.server.feature.auth.model

import java.time.Instant

/**
 * A provider-owned identity linked to exactly one [AuthAccount]. The pair [provider] + [subject] is
 * unique and is the only lookup key: identities are never resolved or linked by email.
 */
internal data class ProviderIdentity(
    val accountId: String,
    val createdAt: Instant,
    val email: String?,
    val id: String,
    val isEmailVerified: Boolean?,
    val lastLoginAt: Instant,
    val provider: ProviderId,
    val subject: String,
)
