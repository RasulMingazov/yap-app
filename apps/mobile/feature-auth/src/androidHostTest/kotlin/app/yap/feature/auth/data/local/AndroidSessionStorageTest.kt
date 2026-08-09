package app.yap.feature.auth.data.local

import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

// Detekt's default test-source excludes do not cover the `androidHostTest` source set yet.
@Suppress("FunctionNaming", "MagicNumber")
internal class AndroidSessionStorageTest {

    private val storageDirectory: File = createTempDirectory()

    @AfterTest
    fun deleteStorageDirectory() {
        storageDirectory.deleteRecursively()
    }

    @Test
    fun `GIVEN a written session WHEN reading THEN the stored session is returned`() = runTest {
        val env = Environment(storageDirectory = storageDirectory)
        val session = StubSessionLocal.stubSessionLocal()

        env.storage.write(session)

        assertEquals(expected = session, actual = env.storage.read())
    }

    @Test
    fun `GIVEN a written session WHEN inspecting private storage THEN only ciphertext is on disk`() = runTest {
        val env = Environment(storageDirectory = storageDirectory)
        val session = StubSessionLocal.stubSessionLocal()

        env.storage.write(session)

        assertEquals(
            expected = false,
            actual = env.file.readBytes().decodeToString().contains(session.refreshToken),
        )
    }

    @Test
    fun `GIVEN a written session WHEN clearing THEN no session remains in private storage`() = runTest {
        val env = Environment(storageDirectory = storageDirectory)
        env.storage.write(StubSessionLocal.stubSessionLocal())

        env.storage.clear()

        assertNull(env.storage.read())
    }

    @Test
    fun `GIVEN an unreadable blob WHEN reading THEN no session is returned`() = runTest {
        val env = Environment(storageDirectory = storageDirectory)
        env.file.writeBytes(byteArrayOf(1, 2, 3))

        assertNull(env.storage.read())
    }

    private class Environment(storageDirectory: File) {

        val file = File(storageDirectory, "session.bin")
        val storage: SessionStorage = AndroidSessionStorage(
            cipher = AesGcmSessionCipher(secretKeyProvider = StubSessionSecretKeyProvider()),
            file = file,
        )
    }
}

private fun createTempDirectory(): File =
    File.createTempFile("session-storage", "").let { file ->
        file.delete()
        file.mkdirs()
        file
    }
