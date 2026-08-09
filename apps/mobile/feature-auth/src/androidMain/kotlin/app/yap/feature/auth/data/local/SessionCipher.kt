package app.yap.feature.auth.data.local

/**
 * Encrypts the session blob that is written to private app storage. The key never leaves Android
 * Keystore, so only the ciphertext is stored on disk (R-079, AC-050).
 */
internal interface SessionCipher {

    fun decrypt(blob: ByteArray): ByteArray?

    fun encrypt(plain: ByteArray): ByteArray
}
