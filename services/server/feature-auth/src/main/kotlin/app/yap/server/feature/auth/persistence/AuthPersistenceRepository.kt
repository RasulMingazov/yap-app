package app.yap.server.feature.auth.persistence

import app.yap.server.feature.auth.identity.GoogleIdentity
import java.sql.SQLException
import java.time.Instant
import java.util.UUID
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

private const val GOOGLE_PROVIDER = "google"
private const val UNIQUE_VIOLATION_SQL_STATE = "23505"

internal class AuthPersistenceRepository : AuthPersistence {

    override fun createSession(session: PersistedSession) {
        transaction {
            maxAttempts = 1

            SessionsTable.deleteWhere { userId eq UUID.fromString(session.userId) }
            SessionsTable.insert { row ->
                row[id] = UUID.fromString(session.sessionId)
                row[userId] = UUID.fromString(session.userId)
                row[refreshTokenHash] = session.refreshTokenHash
                row[expiresAt] = session.expiresAt
                row[createdAt] = Instant.now()
            }
        }
    }

    override fun rotateSession(rotation: SessionRotation): String? = transaction {
        maxAttempts = 1

        val sessionId = UUID.fromString(rotation.sessionId)
        val rotatedRows = SessionsTable.update(
            where = {
                (SessionsTable.id eq sessionId) and
                    (SessionsTable.refreshTokenHash eq rotation.presentedRefreshTokenHash) and
                    (SessionsTable.expiresAt greater Instant.now())
            },
        ) { row ->
            row[refreshTokenHash] = rotation.refreshTokenHash
            row[expiresAt] = rotation.expiresAt
            row[rotatedAt] = Instant.now()
        }

        if (rotatedRows == 0) {
            null
        } else {
            SessionsTable
                .selectAll()
                .where { SessionsTable.id eq sessionId }
                .single()[SessionsTable.userId]
                .value
                .toString()
        }
    }

    override fun resolveOrCreateUserId(identity: GoogleIdentity): String = try {
        resolveOrCreate(identity)
    } catch (error: ExposedSQLException) {
        if (!error.isUniqueViolation()) throw error
        resolveWinner(identity) ?: throw error
    }

    private fun resolveOrCreate(identity: GoogleIdentity): String = transaction {
        maxAttempts = 1

        val existingUserId = findUserId(identity.subject)
        if (existingUserId == null) {
            createUser(identity)
        } else {
            refreshDescriptiveColumns(identity)
            existingUserId
        }
    }

    private fun resolveWinner(identity: GoogleIdentity): String? = transaction {
        maxAttempts = 1
        findUserId(identity.subject)?.also { refreshDescriptiveColumns(identity) }
    }

    private fun findUserId(subject: String): String? = ProviderIdentitiesTable
        .selectAll()
        .where {
            (ProviderIdentitiesTable.provider eq GOOGLE_PROVIDER) and
                (ProviderIdentitiesTable.providerUserId eq subject)
        }
        .limit(1)
        .firstOrNull()
        ?.get(ProviderIdentitiesTable.userId)
        ?.value
        ?.toString()

    private fun createUser(identity: GoogleIdentity): String {
        val createdAtInstant = Instant.now()
        val newUserId = UUID.randomUUID()

        UsersTable.insert { row ->
            row[id] = newUserId
            row[createdAt] = createdAtInstant
        }
        ProviderIdentitiesTable.insert { row ->
            row[id] = UUID.randomUUID()
            row[userId] = newUserId
            row[provider] = GOOGLE_PROVIDER
            row[providerUserId] = identity.subject
            row[email] = identity.email
            row[displayName] = identity.displayName
            row[avatarUrl] = identity.avatarUrl
            row[createdAt] = createdAtInstant
        }
        return newUserId.toString()
    }

    private fun refreshDescriptiveColumns(identity: GoogleIdentity) {
        ProviderIdentitiesTable.update(
            where = {
                (ProviderIdentitiesTable.provider eq GOOGLE_PROVIDER) and
                    (ProviderIdentitiesTable.providerUserId eq identity.subject)
            },
        ) { row ->
            row[email] = identity.email
            row[displayName] = identity.displayName
            row[avatarUrl] = identity.avatarUrl
        }
    }

    private fun Throwable.isUniqueViolation(): Boolean = generateSequence(this, Throwable::cause)
        .filterIsInstance<SQLException>()
        .any { sqlException -> sqlException.sqlState == UNIQUE_VIOLATION_SQL_STATE }
}
