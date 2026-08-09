package app.yap.feature.auth.data.local

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class KeychainSessionStorageTest {

    @Test
    fun `GIVEN a session WHEN writing THEN the keychain item is device-only after first unlock`() = runTest {
        val env = Environment()
        val session = StubSessionDb.stubSessionDb()

        env.storage.write(session)

        env.keychain.writeCall.calledWith(
            KeychainQuery(
                accessibility = KeychainAccessibility.AfterFirstUnlockThisDeviceOnly,
                account = SESSION_KEYCHAIN_ACCOUNT,
                service = SESSION_KEYCHAIN_SERVICE,
            ),
            SessionSerialization.encode(session),
        )
    }

    @Test
    fun `GIVEN a written session WHEN reading THEN the stored session is returned`() = runTest {
        val env = Environment()
        val session = StubSessionDb.stubSessionDb()

        env.storage.write(session)

        assertEquals(expected = session, actual = env.storage.read())
    }

    @Test
    fun `GIVEN a written session WHEN clearing THEN no session remains in the keychain`() = runTest {
        val env = Environment()
        env.storage.write(StubSessionDb.stubSessionDb())

        env.storage.clear()

        assertNull(env.storage.read())
    }

    @Test
    fun `GIVEN an unreadable keychain item WHEN reading THEN no session is returned`() = runTest {
        val env = Environment(stored = "not-a-session")

        assertNull(env.storage.read())
    }

    private class Environment(stored: String? = null) {

        val keychain = StubKeychain(stored = stored)
        val storage: SessionStorage = KeychainSessionStorage(keychain = keychain)
    }
}
