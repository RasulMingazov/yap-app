package app.yap.feature.auth.data.local

import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull

private const val GCM_IV_LENGTH_BYTES = 12
private const val GCM_TAG_LENGTH_BITS = 128
private const val TRANSFORMATION = "AES/GCM/NoPadding"

// Detekt's default test-source excludes do not cover the `androidHostTest` source set yet.
@Suppress("FunctionNaming")
internal class AesGcmSessionCipherTest {

    @Test
    fun `GIVEN a session blob WHEN encrypting THEN AES-GCM decryption with the same key returns it`() {
        val env = Environment()
        val plain = SessionSerialization.encode(StubSessionLocal.stubSessionLocal()).encodeToByteArray()

        val encrypted = env.cipher.encrypt(plain)

        assertContentEquals(
            expected = plain,
            actual = decryptWithAesGcm(key = env.key, blob = encrypted),
        )
    }

    @Test
    fun `GIVEN a session blob WHEN encrypting THEN the blob holds no readable credential`() {
        val env = Environment()
        val session = StubSessionLocal.stubSessionLocal()

        val encrypted = env.cipher.encrypt(SessionSerialization.encode(session).encodeToByteArray())

        assertEquals(
            expected = false,
            actual = encrypted.decodeToString().contains(session.refreshToken),
        )
    }

    @Test
    fun `GIVEN a tampered blob WHEN decrypting THEN nothing is returned`() {
        val env = Environment()
        val encrypted = env.cipher.encrypt(
            SessionSerialization.encode(StubSessionLocal.stubSessionLocal()).encodeToByteArray(),
        )

        val tampered = encrypted.copyOf().also { it[it.lastIndex] = (it.last() + 1).toByte() }

        assertNull(env.cipher.decrypt(tampered))
    }

    @Test
    fun `GIVEN an encrypted blob WHEN decrypting THEN the original session bytes are returned`() {
        val env = Environment()
        val plain = SessionSerialization.encode(StubSessionLocal.stubSessionLocal()).encodeToByteArray()

        val decrypted = env.cipher.decrypt(env.cipher.encrypt(plain))

        assertContentEquals(expected = plain, actual = decrypted)
    }

    private fun decryptWithAesGcm(key: SecretKey, blob: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            key,
            GCMParameterSpec(GCM_TAG_LENGTH_BITS, blob, 0, GCM_IV_LENGTH_BYTES),
        )
        return cipher.doFinal(blob, GCM_IV_LENGTH_BYTES, blob.size - GCM_IV_LENGTH_BYTES)
    }

    private class Environment {

        val secretKeyProvider = StubSessionSecretKeyProvider()
        val key: SecretKey = secretKeyProvider.getOrCreateKey()
        val cipher: SessionCipher = AesGcmSessionCipher(secretKeyProvider = secretKeyProvider)
    }
}
