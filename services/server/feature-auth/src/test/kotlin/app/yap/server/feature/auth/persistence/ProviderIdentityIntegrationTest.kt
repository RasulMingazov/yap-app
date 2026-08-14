package app.yap.server.feature.auth.persistence

import app.yap.server.feature.auth.identity.GoogleIdentity
import java.time.Instant
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Assumptions.assumeTrue

internal class ProviderIdentityIntegrationTest {

    @Test
    fun `GIVEN two concurrent first logins for one provider account WHEN both resolve THEN one account exists`() {
        assumeDocker()

        PostgresTestSupport.withMigratedDatabase { database ->
            val repository = AuthPersistenceRepository()

            val userIds = resolveConcurrently(repository = repository, identity = stubIdentity())

            assertEquals(expected = 1, actual = userIds.distinct().size)
            assertEquals(expected = 1, actual = database.countOf(UsersTable))
            assertEquals(expected = 1, actual = database.countOf(ProviderIdentitiesTable))
        }
    }

    @Test
    fun `GIVEN a second login WHEN it resolves THEN the descriptive columns are refreshed on the same account`() {
        assumeDocker()

        PostgresTestSupport.withMigratedDatabase { database ->
            val repository = AuthPersistenceRepository()
            val first = repository.resolveOrCreateUserId(stubIdentity())

            val second = repository.resolveOrCreateUserId(
                stubIdentity(
                    email = "renamed@example.com",
                    displayName = "Renamed",
                    avatarUrl = "https://example.com/renamed.png",
                ),
            )

            assertEquals(expected = first, actual = second)
            assertEquals(expected = 1, actual = database.countOf(UsersTable))
            assertEquals(
                expected = listOf(
                    Descriptive(
                        email = "renamed@example.com",
                        displayName = "Renamed",
                        avatarUrl = "https://example.com/renamed.png",
                    ),
                ),
                actual = database.descriptiveColumns(),
            )
        }
    }

    @Test
    fun `GIVEN a token omitting the name and picture WHEN it resolves THEN both are stored absent and login proceeds`() {
        assumeDocker()

        PostgresTestSupport.withMigratedDatabase { database ->
            val repository = AuthPersistenceRepository()

            val userId = repository.resolveOrCreateUserId(
                stubIdentity(displayName = null, avatarUrl = null),
            )

            assertEquals(expected = 1, actual = database.countOf(UsersTable))
            assertEquals(expected = userId, actual = database.singleIdentityUserId())
            assertNull(actual = database.descriptiveColumns().single().displayName)
            assertNull(actual = database.descriptiveColumns().single().avatarUrl)
        }
    }

    @Test
    fun `GIVEN a second login that drops the name WHEN it resolves THEN the stored name becomes absent`() {
        assumeDocker()

        PostgresTestSupport.withMigratedDatabase { database ->
            val repository = AuthPersistenceRepository()
            repository.resolveOrCreateUserId(stubIdentity())

            repository.resolveOrCreateUserId(stubIdentity(displayName = null, avatarUrl = null))

            assertNull(actual = database.descriptiveColumns().single().displayName)
            assertNull(actual = database.descriptiveColumns().single().avatarUrl)
        }
    }

    @Test
    fun `GIVEN another provider already holding an address WHEN Google reports it too THEN the accounts stay separate`() {
        assumeDocker()

        PostgresTestSupport.withMigratedDatabase { database ->
            val repository = AuthPersistenceRepository()
            val otherProviderUserId = database.insertForeignProviderIdentity(email = SHARED_EMAIL)

            val googleUserId = repository.resolveOrCreateUserId(stubIdentity(email = SHARED_EMAIL))

            assertEquals(expected = 2, actual = database.countOf(UsersTable))
            assertEquals(expected = 2, actual = database.countOf(ProviderIdentitiesTable))
            assertEquals(expected = 2, actual = listOf(otherProviderUserId, googleUserId).distinct().size)
        }
    }

    private fun resolveConcurrently(
        repository: AuthPersistenceRepository,
        identity: GoogleIdentity,
    ): List<String> {
        val executor = Executors.newFixedThreadPool(CONCURRENT_LOGINS)
        val start = CountDownLatch(1)

        return try {
            val pending = List(CONCURRENT_LOGINS) {
                executor.submit<String> {
                    start.await()
                    repository.resolveOrCreateUserId(identity)
                }
            }
            start.countDown()
            pending.map { result -> result.get(RESOLVE_TIMEOUT_SECONDS, TimeUnit.SECONDS) }
        } finally {
            executor.shutdownNow()
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

    private fun Database.countOf(table: UUIDTable): Int =
        transaction(this) { table.selectAll().count().toInt() }

    private fun Database.descriptiveColumns(): List<Descriptive> = transaction(this) {
        ProviderIdentitiesTable
            .selectAll()
            .where { ProviderIdentitiesTable.provider eq GOOGLE_PROVIDER }
            .map { row ->
                Descriptive(
                    email = row[ProviderIdentitiesTable.email],
                    displayName = row[ProviderIdentitiesTable.displayName],
                    avatarUrl = row[ProviderIdentitiesTable.avatarUrl],
                )
            }
    }

    private fun Database.singleIdentityUserId(): String = transaction(this) {
        ProviderIdentitiesTable.selectAll().single()[ProviderIdentitiesTable.userId].value.toString()
    }

    private fun Database.insertForeignProviderIdentity(email: String): String = transaction(this) {
        val createdAt = Instant.now()
        val userId = UUID.randomUUID()

        UsersTable.insert { row ->
            row[id] = userId
            row[UsersTable.createdAt] = createdAt
        }
        ProviderIdentitiesTable.insert { row ->
            row[id] = UUID.randomUUID()
            row[ProviderIdentitiesTable.userId] = userId
            row[provider] = FOREIGN_PROVIDER
            row[providerUserId] = "foreign-sub-1"
            row[ProviderIdentitiesTable.email] = email
            row[displayName] = null
            row[avatarUrl] = null
            row[ProviderIdentitiesTable.createdAt] = createdAt
        }
        userId.toString()
    }

    private data class Descriptive(
        val email: String?,
        val displayName: String?,
        val avatarUrl: String?,
    )

    private companion object {
        const val CONCURRENT_LOGINS = 2
        const val FOREIGN_PROVIDER = "apple"
        const val GOOGLE_PROVIDER = "google"
        const val RESOLVE_TIMEOUT_SECONDS = 30L
        const val SHARED_EMAIL = "shared@example.com"
    }
}
