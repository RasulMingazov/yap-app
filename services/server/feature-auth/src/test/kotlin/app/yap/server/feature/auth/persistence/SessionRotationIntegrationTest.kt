package app.yap.server.feature.auth.persistence

import app.yap.server.feature.auth.identity.GoogleIdentity
import java.time.Duration
import java.time.Instant
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Assumptions.assumeTrue

internal class SessionRotationIntegrationTest {

    @Test
    fun `GIVEN two concurrent rotations of one token WHEN both run THEN one succeeds and one is refused`() {
        assumeDocker()

        PostgresTestSupport.withMigratedDatabase {
            val repository = AuthPersistenceRepository()
            val session = repository.givenSession()

            val outcomes = rotateConcurrently(repository = repository, session = session)

            assertEquals(expected = 1, actual = outcomes.count { userId -> userId != null })
            assertEquals(expected = 1, actual = outcomes.count { userId -> userId == null })
        }
    }

    @Test
    fun `GIVEN a session near the end of its window WHEN it is rotated THEN the window moves on from now`() {
        assumeDocker()

        PostgresTestSupport.withMigratedDatabase { database ->
            val repository = AuthPersistenceRepository()
            val session = repository.givenSession(expiresAt = Instant.now().plus(Duration.ofDays(1)))
            val renewedExpiry = Instant.now().plus(Duration.ofDays(WINDOW_DAYS))

            val userId = repository.rotateSession(session.rotationTo(HASH_AFTER, renewedExpiry))

            assertNotNull(actual = userId)
            val stored = database.storedExpiry(session.sessionId)
            assertTrue(
                actual = Duration.between(renewedExpiry, stored).abs() < TOLERANCE,
                message = "expected the window to move to $renewedExpiry, but it was $stored",
            )
        }
    }

    @Test
    fun `GIVEN a rotated session WHEN the old token is presented again THEN it is refused`() {
        assumeDocker()

        PostgresTestSupport.withMigratedDatabase {
            val repository = AuthPersistenceRepository()
            val session = repository.givenSession()
            repository.rotateSession(session.rotationTo(HASH_AFTER))

            val replayed = repository.rotateSession(session.rotationTo(HASH_AFTER))

            assertNull(actual = replayed)
        }
    }

    @Test
    fun `GIVEN a session past its expiry WHEN it is rotated THEN it is refused`() {
        assumeDocker()

        PostgresTestSupport.withMigratedDatabase {
            val repository = AuthPersistenceRepository()
            val session = repository.givenSession(expiresAt = Instant.now().minusSeconds(1))

            val rotated = repository.rotateSession(session.rotationTo(HASH_AFTER))

            assertNull(actual = rotated)
        }
    }

    @Test
    fun `GIVEN an unknown session WHEN it is rotated THEN it is refused`() {
        assumeDocker()

        PostgresTestSupport.withMigratedDatabase {
            val repository = AuthPersistenceRepository()

            val rotated = repository.rotateSession(
                SessionRotation(
                    sessionId = UUID.randomUUID().toString(),
                    presentedRefreshTokenHash = HASH_BEFORE,
                    refreshTokenHash = HASH_AFTER,
                    expiresAt = Instant.now().plus(Duration.ofDays(WINDOW_DAYS)),
                ),
            )

            assertNull(actual = rotated)
        }
    }

    @Test
    fun `GIVEN an existing session WHEN the same account logs in again THEN only the newer one survives`() {
        assumeDocker()

        PostgresTestSupport.withMigratedDatabase { database ->
            val repository = AuthPersistenceRepository()
            val first = repository.givenSession()

            val second = repository.givenSession()

            assertEquals(expected = 1, actual = database.sessionCount())
            assertEquals(expected = listOf(second.sessionId), actual = database.sessionIds())
            assertNull(actual = repository.rotateSession(first.rotationTo(HASH_AFTER)))
        }
    }

    private fun rotateConcurrently(
        repository: AuthPersistenceRepository,
        session: CreatedSession,
    ): List<String?> {
        val executor = Executors.newFixedThreadPool(CONCURRENT_ROTATIONS)
        val start = CountDownLatch(1)

        return try {
            val pending = List(CONCURRENT_ROTATIONS) { index ->
                executor.submit<String?> {
                    start.await()
                    repository.rotateSession(session.rotationTo("$HASH_AFTER-$index"))
                }
            }
            start.countDown()
            pending.map { result -> result.get(ROTATE_TIMEOUT_SECONDS, TimeUnit.SECONDS) }
        } finally {
            executor.shutdownNow()
        }
    }

    private fun AuthPersistenceRepository.givenSession(
        expiresAt: Instant = Instant.now().plus(Duration.ofDays(WINDOW_DAYS)),
    ): CreatedSession {
        val userId = resolveOrCreateUserId(
            GoogleIdentity(
                subject = "sub-1",
                email = "learner@example.com",
                displayName = "Learner",
                avatarUrl = null,
            ),
        )
        val sessionId = UUID.randomUUID().toString()

        createSession(
            PersistedSession(
                sessionId = sessionId,
                userId = userId,
                refreshTokenHash = HASH_BEFORE,
                expiresAt = expiresAt,
            ),
        )
        return CreatedSession(sessionId = sessionId, userId = userId)
    }

    private fun CreatedSession.rotationTo(
        refreshTokenHash: String,
        expiresAt: Instant = Instant.now().plus(Duration.ofDays(WINDOW_DAYS)),
    ): SessionRotation = SessionRotation(
        sessionId = sessionId,
        presentedRefreshTokenHash = HASH_BEFORE,
        refreshTokenHash = refreshTokenHash,
        expiresAt = expiresAt,
    )

    private fun assumeDocker() = assumeTrue(
        PostgresTestSupport.isDockerAvailable,
        "Docker is not available: the PostgreSQL integration suite did not run",
    )

    private fun Database.sessionCount(): Int =
        transaction(this) { SessionsTable.selectAll().count().toInt() }

    private fun Database.sessionIds(): List<String> = transaction(this) {
        SessionsTable.selectAll().map { row -> row[SessionsTable.id].value.toString() }
    }

    private fun Database.storedExpiry(sessionId: String): Instant = transaction(this) {
        SessionsTable
            .selectAll()
            .where { SessionsTable.id eq UUID.fromString(sessionId) }
            .single()[SessionsTable.expiresAt]
    }

    private data class CreatedSession(
        val sessionId: String,
        val userId: String,
    )

    private companion object {
        const val CONCURRENT_ROTATIONS = 2
        const val HASH_AFTER = "hash-after"
        const val HASH_BEFORE = "hash-before"
        const val ROTATE_TIMEOUT_SECONDS = 30L
        const val WINDOW_DAYS = 90L
        val TOLERANCE: Duration = Duration.ofMinutes(1)
    }
}
