package app.yap.feature.auth.data.local

import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec

private const val GCM_IV_LENGTH_BYTES = 12
private const val GCM_TAG_LENGTH_BITS = 128
private const val TRANSFORMATION = "AES/GCM/NoPadding"

/**
 * Writes `iv || ciphertext` so the blob on disk is unreadable without the Keystore-held key.
 */
internal class AesGcmSessionCipher(
    private val secretKeyProvider: SessionSecretKeyProvider,
) : SessionCipher {

    override fun decrypt(blob: ByteArray): ByteArray? {
        if (blob.size <= GCM_IV_LENGTH_BYTES) return null

        return runCatching {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE,
                secretKeyProvider.getOrCreateKey(),
                GCMParameterSpec(GCM_TAG_LENGTH_BITS, blob, 0, GCM_IV_LENGTH_BYTES),
            )
            cipher.doFinal(blob, GCM_IV_LENGTH_BYTES, blob.size - GCM_IV_LENGTH_BYTES)
        }.getOrNull()
    }

    override fun encrypt(plain: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKeyProvider.getOrCreateKey())
        return cipher.iv + cipher.doFinal(plain)
    }
}
