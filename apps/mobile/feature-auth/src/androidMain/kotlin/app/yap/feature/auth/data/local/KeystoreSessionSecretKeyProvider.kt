package app.yap.feature.auth.data.local

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

private const val ANDROID_KEY_STORE = "AndroidKeyStore"
private const val KEY_SIZE_BITS = 256

/**
 * The session key is generated inside Android Keystore and is never exported, so the encrypted
 * blob in private app storage is the only session material on the file system (R-079, AC-050).
 */
internal class KeystoreSessionSecretKeyProvider(
    private val keyAlias: String = DEFAULT_KEY_ALIAS,
) : SessionSecretKeyProvider {

    override fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
        return keyStore.getKey(keyAlias, null) as? SecretKey ?: generateKey()
    }

    private fun generateKey(): SecretKey {
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE)
        generator.init(
            KeyGenParameterSpec
                .Builder(
                    keyAlias,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(KEY_SIZE_BITS)
                .build(),
        )
        return generator.generateKey()
    }

    companion object {
        const val DEFAULT_KEY_ALIAS = "app.yap.feature.auth.session"
    }
}
