package app.yap.server.feature.auth.persistence

import app.yap.server.feature.auth.model.AuthAccount
import app.yap.server.feature.auth.model.NewSession
import app.yap.server.feature.auth.model.SessionRotationResult
import app.yap.server.feature.auth.model.VerifiedIdentity
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.exceptions.ExposedSQLException

/**
 * Every invariant here is PostgreSQL behavior: locking, constraints, atomicity, and rollback.
 *
 * The suppressions cover this source set only: Detekt excludes `src/test` from its test-shaped
 * naming and size rules, and that exclusion does not reach a source set added by this module.
 */
@Suppress("FunctionNaming", "TooManyFunctions")
class ExposedAuthRepositoryIntegrationTest {

    private val repository = AuthDatabase.repository()

    @BeforeTest
    fun clearDatabase() {
        AuthDatabase.clear()
    }

    @Test
    fun `GIVEN one challenge WHEN two attempts consume it concurrently THEN exactly one session exists`() {
        val challenge = StubAuthRow.stubAuthChallenge()
        runBlocking { repository.insertChallenge(challenge) }

        runConcurrently(
            first = {
                repository.consumeChallengeAndCreateSession(
                    challenge = challenge,
                    identity = StubAuthRow.stubVerifiedIdentity(),
                    session = StubAuthRow.stubNewSession(),
                )
            },
            second = {
                repository.consumeChallengeAndCreateSession(
                    challenge = challenge,
                    identity = StubAuthRow.stubVerifiedIdentity(),
                    session = StubAuthRow.stubNewSession(),
                )
            },
        )

        assertEquals(expected = 1, actual = sessionIds().size)
    }

    @Test
    fun `GIVEN a consumed challenge WHEN it is presented again THEN nothing is created`() {
        val challenge = StubAuthRow.stubAuthChallenge()
        runBlocking {
            repository.insertChallenge(challenge)
            repository.consumeChallengeAndCreateSession(
                challenge = challenge,
                identity = StubAuthRow.stubVerifiedIdentity(),
                session = StubAuthRow.stubNewSession(),
            )
        }

        val result = runBlocking {
            repository.consumeChallengeAndCreateSession(
                challenge = challenge,
                identity = StubAuthRow.stubVerifiedIdentity(),
                session = StubAuthRow.stubNewSession(),
            )
        }

        assertNull(result)
    }

    @Test
    fun `GIVEN an expired challenge WHEN it is consumed THEN nothing is created`() {
        val challenge = StubAuthRow.stubAuthChallenge(expiresAt = AuthDatabase.NOW.minusSeconds(1))
        runBlocking { repository.insertChallenge(challenge) }

        val result = runBlocking {
            repository.consumeChallengeAndCreateSession(
                challenge = challenge,
                identity = StubAuthRow.stubVerifiedIdentity(),
                session = StubAuthRow.stubNewSession(),
            )
        }

        assertNull(result)
    }

    @Test
    fun `GIVEN one identity WHEN two first logins run concurrently THEN exactly one account exists`() {
        val first = StubAuthRow.stubAuthChallenge()
        val second = StubAuthRow.stubAuthChallenge()
        runBlocking {
            repository.insertChallenge(first)
            repository.insertChallenge(second)
        }

        runConcurrently(
            first = {
                repository.consumeChallengeAndCreateSession(
                    challenge = first,
                    identity = StubAuthRow.stubVerifiedIdentity(),
                    session = StubAuthRow.stubNewSession(),
                )
            },
            second = {
                repository.consumeChallengeAndCreateSession(
                    challenge = second,
                    identity = StubAuthRow.stubVerifiedIdentity(),
                    session = StubAuthRow.stubNewSession(),
                )
            },
        )

        assertEquals(expected = 1, actual = accountIds().size)
    }

    @Test
    fun `GIVEN a stored identity WHEN the same provider and subject are stored again THEN the database refuses it`() {
        val account = login()

        assertFailsWith<ExposedSQLException> {
            insertProviderIdentity(accountId = account.id)
        }
    }

    @Test
    fun `GIVEN a login that fails after its account is created WHEN it rolls back THEN no account remains`() {
        val challenge = StubAuthRow.stubAuthChallenge()
        runBlocking { repository.insertChallenge(challenge) }

        assertFailsWith<IllegalArgumentException> {
            runBlocking {
                repository.consumeChallengeAndCreateSession(
                    challenge = challenge,
                    identity = StubAuthRow.stubVerifiedIdentity(),
                    session = StubAuthRow.stubNewSession(id = "not-a-session-identifier"),
                )
            }
        }

        assertEquals(expected = emptyList(), actual = accountIds())
    }

    @Test
    fun `GIVEN a current refresh value WHEN the session rotates THEN only the rotated and previous hashes remain`() {
        val session = StubAuthRow.stubNewSession()
        login(session = session)

        runBlocking { repository.rotateSession(StubAuthRow.stubSessionRotation(sessionId = session.id)) }

        assertEquals(
            expected = listOf(StubAuthRow.ROTATED_TOKEN_HASH, StubAuthRow.REFRESH_TOKEN_HASH),
            actual = sessionTokenHashes(session.id),
        )
    }

    @Test
    fun `GIVEN a rotated session WHEN the previous value is presented again THEN it is refused as a replay`() {
        val session = StubAuthRow.stubNewSession()
        login(session = session)
        val rotation = StubAuthRow.stubSessionRotation(sessionId = session.id)
        runBlocking { repository.rotateSession(rotation) }

        val result = runBlocking { repository.rotateSession(rotation) }

        assertEquals(expected = SessionRotationResult.Replayed, actual = result)
    }

    @Test
    fun `GIVEN a rotated session WHEN the previous value is presented again THEN the whole session is revoked`() {
        val session = StubAuthRow.stubNewSession()
        login(session = session)
        val rotation = StubAuthRow.stubSessionRotation(sessionId = session.id)
        runBlocking { repository.rotateSession(rotation) }

        runBlocking { repository.rotateSession(rotation) }

        assertEquals(expected = AuthDatabase.NOW, actual = sessionRevokedAt(session.id))
    }

    @Test
    fun `GIVEN a revoked session WHEN its current value is presented THEN it is refused as expired`() {
        val session = StubAuthRow.stubNewSession()
        login(session = session)
        val rotation = StubAuthRow.stubSessionRotation(sessionId = session.id)
        runBlocking {
            repository.rotateSession(rotation)
            repository.rotateSession(rotation)
        }

        val result = runBlocking {
            repository.rotateSession(
                StubAuthRow.stubSessionRotation(
                    sessionId = session.id,
                    presentedTokenHash = StubAuthRow.ROTATED_TOKEN_HASH,
                ),
            )
        }

        assertEquals(expected = SessionRotationResult.Expired, actual = result)
    }

    @Test
    fun `GIVEN expired and live challenges WHEN cleaning up THEN only the live one remains`() {
        val expired = StubAuthRow.stubAuthChallenge(expiresAt = AuthDatabase.NOW.minusSeconds(1))
        val live = StubAuthRow.stubAuthChallenge()
        runBlocking {
            repository.insertChallenge(expired)
            repository.insertChallenge(live)
        }

        runBlocking { repository.deleteExpiredChallenges() }

        assertEquals(expected = listOf(live.id), actual = challengeIds())
    }

    /** One successful login: a fresh challenge, consumed, with the account it resolved. */
    private fun login(
        identity: VerifiedIdentity = StubAuthRow.stubVerifiedIdentity(),
        session: NewSession = StubAuthRow.stubNewSession(),
    ): AuthAccount = runBlocking {
        val challenge = StubAuthRow.stubAuthChallenge()
        repository.insertChallenge(challenge)

        checkNotNull(
            repository.consumeChallengeAndCreateSession(
                challenge = challenge,
                identity = identity,
                session = session,
            ),
        )
    }
}
