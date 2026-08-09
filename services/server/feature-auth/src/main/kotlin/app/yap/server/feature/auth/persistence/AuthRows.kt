package app.yap.server.feature.auth.persistence

import app.yap.server.feature.auth.model.AuthChallenge
import app.yap.server.feature.auth.model.ProviderId
import app.yap.server.feature.auth.model.ProviderIdentity
import java.util.UUID
import org.jetbrains.exposed.sql.ResultRow

/** Translations between authentication rows and feature models. */
internal fun ResultRow.toAuthChallenge(): AuthChallenge = AuthChallenge(
    createdAt = this[ChallengeTable.createdAt],
    expiresAt = this[ChallengeTable.expiresAt],
    id = this[ChallengeTable.id].toString(),
    nonceHash = this[ChallengeTable.nonceHash],
    proof = this[ChallengeTable.proof],
    provider = ProviderId(this[ChallengeTable.provider]),
)

internal fun ResultRow.toProviderIdentity(): ProviderIdentity = ProviderIdentity(
    accountId = this[ProviderIdentityTable.accountId].toString(),
    createdAt = this[ProviderIdentityTable.createdAt],
    email = this[ProviderIdentityTable.email],
    id = this[ProviderIdentityTable.id].toString(),
    isEmailVerified = this[ProviderIdentityTable.isEmailVerified],
    lastLoginAt = this[ProviderIdentityTable.lastLoginAt],
    provider = ProviderId(this[ProviderIdentityTable.provider]),
    subject = this[ProviderIdentityTable.subject],
)

/** A client-supplied identifier that is not a UUID simply identifies no row. */
internal fun String.toUuidOrNull(): UUID? = runCatching { UUID.fromString(this) }.getOrNull()
