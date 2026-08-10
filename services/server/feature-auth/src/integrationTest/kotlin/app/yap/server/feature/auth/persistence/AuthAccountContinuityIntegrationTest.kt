package app.yap.server.feature.auth.persistence

import app.yap.server.feature.auth.model.AuthAccount
import app.yap.server.feature.auth.model.ProviderId
import app.yap.server.feature.auth.model.VerifiedIdentity
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlinx.coroutines.runBlocking

/**
 * The stable Yap account ID is the ownership key for learning progress, so its continuity is
 * verified against the real database rather than against a returned value alone.
 */
@Suppress("FunctionNaming")
class AuthAccountContinuityIntegrationTest {

    private val repository = AuthDatabase.repository()

    @BeforeTest
    fun clearDatabase() {
        AuthDatabase.clear()
    }

    @Test
    fun `GIVEN a returning identity WHEN it logs in again THEN the same account owns the session`() {
        val identity = StubAuthRow.stubVerifiedIdentity()
        val first = login(identity)

        val second = login(identity)

        assertEquals(expected = first.id, actual = second.id)
    }

    @Test
    fun `GIVEN unlinked identities from different providers WHEN both log in THEN they own different accounts`() {
        val google = login(StubAuthRow.stubVerifiedIdentity(provider = ProviderId.Google))

        val apple = login(StubAuthRow.stubVerifiedIdentity(provider = ProviderId("apple")))

        assertNotEquals(illegal = google.id, actual = apple.id)
    }

    @Test
    fun `GIVEN a returning identity WHEN it logs in again THEN no second account is created`() {
        val identity = StubAuthRow.stubVerifiedIdentity()
        login(identity)

        login(identity)

        assertEquals(expected = 1, actual = accountIds().size)
    }

    /** One successful login: a fresh challenge, consumed, with the account it resolved. */
    private fun login(identity: VerifiedIdentity): AuthAccount = runBlocking {
        val challenge = StubAuthRow.stubAuthChallenge(provider = identity.provider)
        repository.insertChallenge(challenge)

        checkNotNull(
            repository.consumeChallengeAndCreateSession(
                challenge = challenge,
                identity = identity,
                session = StubAuthRow.stubNewSession(),
            ),
        )
    }
}
