package app.yap.server.feature.auth.persistence

import app.yap.server.feature.auth.model.AuthAccount
import app.yap.server.feature.auth.model.AuthChallenge
import app.yap.server.feature.auth.model.NewSession
import app.yap.server.feature.auth.model.ProviderIdentity
import app.yap.server.feature.auth.model.VerifiedIdentity
import java.time.Clock
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

internal class ExposedAuthRepository(
    private val clock: Clock,
    private val database: Database,
) : AuthRepository {

    /**
     * Locks the challenge row first, so two attempts presenting one challenge serialize here and
     * exactly one of them reaches session creation. Expiry is then judged by the clock read while
     * the lock is held, because provider verification ran outside this transaction and may have
     * outlived the challenge. Any mismatch leaves the transaction without a write, so a rejected
     * attempt neither consumes the challenge nor creates a partial account.
     *
     * That same instant timestamps the account, the identity, and the session.
     */
    override suspend fun consumeChallengeAndCreateSession(
        challenge: AuthChallenge,
        identity: VerifiedIdentity,
        session: NewSession,
    ): AuthAccount? = query {
        val challengeId = challenge.id.toUuidOrNull() ?: return@query null
        val locked = ChallengeTable.selectAll()
            .where { ChallengeTable.id eq challengeId }
            .forUpdate()
            .singleOrNull()
            ?: return@query null

        val now = Instant.now(clock)
        if (locked.toAuthChallenge() != challenge) return@query null
        if (!locked[ChallengeTable.expiresAt].isAfter(now)) return@query null
        if (ChallengeTable.deleteWhere { ChallengeTable.id eq challengeId } != 1) return@query null

        val account = resolveAccount(identity = identity, now = now)
        SessionTable.insert {
            it[id] = UUID.fromString(session.id)
            it[accountId] = UUID.fromString(account.id)
            it[refreshTokenHash] = session.refreshTokenHash
            it[previousTokenHash] = null
            it[createdAt] = now
            it[lastUsedAt] = now
            it[absoluteExpiresAt] = now.plus(session.absoluteLifetime)
            it[revokedAt] = null
        }
        account
    }

    override suspend fun findChallenge(id: String): AuthChallenge? {
        val challengeId = id.toUuidOrNull() ?: return null
        return query {
            ChallengeTable.selectAll()
                .where { ChallengeTable.id eq challengeId }
                .singleOrNull()
                ?.toAuthChallenge()
        }
    }

    override suspend fun insertChallenge(challenge: AuthChallenge) {
        query {
            ChallengeTable.insert {
                it[id] = UUID.fromString(challenge.id)
                it[provider] = challenge.provider.value
                it[nonceHash] = challenge.nonceHash
                it[proof] = challenge.proof
                it[createdAt] = challenge.createdAt
                it[expiresAt] = challenge.expiresAt
            }
        }
    }

    /**
     * Resolves the account owning [identity] by the unique provider and subject pair, creating one
     * on first login. A concurrent first login is settled by the unique constraint: the attempt
     * that loses re-reads the winning identity and removes the account it had just created, so one
     * identity never ends up with two accounts.
     */
    private fun resolveAccount(identity: VerifiedIdentity, now: Instant): AuthAccount {
        val existing = findIdentity(identity) ?: return createAccount(identity = identity, now = now)
        updateIdentity(existing = existing, identity = identity, now = now)

        return readAccount(existing.accountId)
    }

    private fun createAccount(identity: VerifiedIdentity, now: Instant): AuthAccount {
        val account = AuthAccount(createdAt = now, id = UUID.randomUUID().toString())
        AccountTable.insert {
            it[id] = UUID.fromString(account.id)
            it[createdAt] = account.createdAt
        }
        insertIdentity(account = account, identity = identity, now = now)

        val stored = checkNotNull(findIdentity(identity)) { "Provider identity must exist after insert" }
        if (stored.accountId == account.id) return account

        AccountTable.deleteWhere { AccountTable.id eq UUID.fromString(account.id) }
        return readAccount(stored.accountId)
    }

    private fun findIdentity(identity: VerifiedIdentity): ProviderIdentity? =
        ProviderIdentityTable.selectAll()
            .where {
                (ProviderIdentityTable.provider eq identity.provider.value) and
                    (ProviderIdentityTable.subject eq identity.subject)
            }
            .singleOrNull()
            ?.toProviderIdentity()

    private fun insertIdentity(account: AuthAccount, identity: VerifiedIdentity, now: Instant) {
        ProviderIdentityTable.insertIgnore {
            it[id] = UUID.randomUUID()
            it[accountId] = UUID.fromString(account.id)
            it[provider] = identity.provider.value
            it[subject] = identity.subject
            it[email] = identity.email
            it[isEmailVerified] = identity.isEmailVerified
            it[createdAt] = now
            it[lastLoginAt] = now
        }
    }

    /** A response that omits the email never erases an email stored by an earlier login. */
    private fun updateIdentity(
        existing: ProviderIdentity,
        identity: VerifiedIdentity,
        now: Instant,
    ) {
        ProviderIdentityTable.update({ ProviderIdentityTable.id eq UUID.fromString(existing.id) }) {
            it[lastLoginAt] = now
            if (identity.email != null) {
                it[email] = identity.email
                it[isEmailVerified] = identity.isEmailVerified
            }
        }
    }

    private fun readAccount(accountId: String): AuthAccount {
        val row = checkNotNull(
            AccountTable.selectAll()
                .where { AccountTable.id eq UUID.fromString(accountId) }
                .singleOrNull(),
        ) { "Account of an existing provider identity must exist" }

        return AuthAccount(
            createdAt = row[AccountTable.createdAt],
            id = row[AccountTable.id].toString(),
        )
    }

    private suspend fun <T> query(block: Transaction.() -> T): T =
        withContext(Dispatchers.IO) { transaction(database, block) }
}
