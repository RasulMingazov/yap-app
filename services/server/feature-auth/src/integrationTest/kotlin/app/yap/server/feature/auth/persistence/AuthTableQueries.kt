package app.yap.server.feature.auth.persistence

import app.yap.server.feature.auth.model.ProviderId
import java.time.Instant
import java.util.UUID
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

/** Reads the persisted state back through plain queries, never through the adapter under test. */
internal fun accountIds(): List<String> = transaction(AuthDatabase.database) {
    AccountTable.selectAll().map { row -> row[AccountTable.id].toString() }
}

internal fun challengeIds(): List<String> = transaction(AuthDatabase.database) {
    ChallengeTable.selectAll().map { row -> row[ChallengeTable.id].toString() }
}

internal fun sessionIds(): List<String> = transaction(AuthDatabase.database) {
    SessionTable.selectAll().map { row -> row[SessionTable.id].toString() }
}

/** The current and the previous refresh hash of one session, in that order. */
internal fun sessionTokenHashes(sessionId: String): List<String?> = transaction(AuthDatabase.database) {
    val row = SessionTable.selectAll().where { SessionTable.id eq UUID.fromString(sessionId) }.single()
    listOf(row[SessionTable.refreshTokenHash], row[SessionTable.previousTokenHash])
}

internal fun sessionRevokedAt(sessionId: String): Instant? = transaction(AuthDatabase.database) {
    SessionTable.selectAll()
        .where { SessionTable.id eq UUID.fromString(sessionId) }
        .single()[SessionTable.revokedAt]
}

/** Stores an identity directly, so nothing but the database constraint judges the pair. */
internal fun insertProviderIdentity(
    accountId: String,
    provider: ProviderId = ProviderId.Google,
    subject: String = StubAuthRow.SUBJECT,
) {
    transaction(AuthDatabase.database) {
        ProviderIdentityTable.insert { row ->
            row[ProviderIdentityTable.id] = UUID.randomUUID()
            row[ProviderIdentityTable.accountId] = UUID.fromString(accountId)
            row[ProviderIdentityTable.provider] = provider.value
            row[ProviderIdentityTable.subject] = subject
            row[ProviderIdentityTable.email] = null
            row[ProviderIdentityTable.isEmailVerified] = null
            row[ProviderIdentityTable.createdAt] = AuthDatabase.NOW
            row[ProviderIdentityTable.lastLoginAt] = AuthDatabase.NOW
        }
    }
}
