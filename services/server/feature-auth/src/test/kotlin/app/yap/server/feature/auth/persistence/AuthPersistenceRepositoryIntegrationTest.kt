package app.yap.server.feature.auth.persistence

import app.yap.server.feature.auth.identity.GoogleIdentity
import java.time.Instant
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Assumptions.assumeTrue

internal class AuthPersistenceRepositoryIntegrationTest {

    @Test
    fun `GIVEN an unknown provider account WHEN it is resolved THEN one user and one identity are created`() {
        assumeDocker()

        PostgresTestSupport.withMigratedDatabase { database ->
            val repository = AuthPersistenceRepository()

            repository.resolveOrCreateUserId(stubIdentity())

            assertEquals(expected = 1, actual = database.countOf(UsersTable))
            assertEquals(expected = 1, actual = database.countOf(ProviderIdentitiesTable))
        }
    }

    @Test
    fun `GIVEN a known provider account WHEN it is resolved again THEN the same user comes back`() {
        assumeDocker()

        PostgresTestSupport.withMigratedDatabase { database ->
            val repository = AuthPersistenceRepository()

            val first = repository.resolveOrCreateUserId(stubIdentity())
            val second = repository.resolveOrCreateUserId(stubIdentity())

            assertEquals(expected = first, actual = second)
            assertEquals(expected = 1, actual = database.countOf(UsersTable))
            assertEquals(expected = 1, actual = database.countOf(ProviderIdentitiesTable))
        }
    }

    @Test
    fun `GIVEN two different provider accounts WHEN both are resolved THEN each gets its own user`() {
        assumeDocker()

        PostgresTestSupport.withMigratedDatabase { database ->
            val repository = AuthPersistenceRepository()

            val first = repository.resolveOrCreateUserId(stubIdentity(subject = "sub-1"))
            val second = repository.resolveOrCreateUserId(stubIdentity(subject = "sub-2"))

            assertNotEquals(illegal = first, actual = second)
            assertEquals(expected = 2, actual = database.countOf(UsersTable))
        }
    }

    @Test
    fun `GIVEN a resolved user WHEN a session is created THEN the row holds the hash and not the token`() {
        assumeDocker()

        PostgresTestSupport.withMigratedDatabase { database ->
            val repository = AuthPersistenceRepository()
            val userId = repository.resolveOrCreateUserId(stubIdentity())

            repository.createSession(
                PersistedSession(
                    sessionId = SESSION_ID,
                    userId = userId,
                    refreshTokenHash = REFRESH_TOKEN_HASH,
                    expiresAt = EXPIRES_AT,
                ),
            )

            val storedHashes = database.storedRefreshTokenHashes()
            assertEquals(expected = listOf(REFRESH_TOKEN_HASH), actual = storedHashes)
        }
    }

    private fun assumeDocker() = assumeTrue(
        PostgresTestSupport.isDockerAvailable,
        "Docker is not available: the PostgreSQL integration suite did not run",
    )

    private fun stubIdentity(
        subject: String = "sub-1",
        email: String? = "learner@example.com",
        displayName: String? = "Learner",
        avatarUrl: String? = "https://example.com/avatar.png",
    ): GoogleIdentity = GoogleIdentity(
        subject = subject,
        email = email,
        displayName = displayName,
        avatarUrl = avatarUrl,
    )

    private fun Database.countOf(table: org.jetbrains.exposed.dao.id.UUIDTable): Int =
        transaction(this) { table.selectAll().count().toInt() }

    private fun Database.storedRefreshTokenHashes(): List<String> = transaction(this) {
        SessionsTable.selectAll().map { row -> row[SessionsTable.refreshTokenHash] }
    }

    private companion object {
        const val REFRESH_TOKEN_HASH = "0123456789abcdef"
        val SESSION_ID: String = UUID.randomUUID().toString()
        val EXPIRES_AT: Instant = Instant.parse("2026-11-11T10:00:00Z")
    }
}
