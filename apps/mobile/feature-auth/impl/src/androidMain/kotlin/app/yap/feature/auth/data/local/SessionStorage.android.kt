package app.yap.feature.auth.data.local

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.serialization.json.Json
import org.koin.core.scope.Scope

private const val DATA_STORE_NAME = "yap_session"
private const val KEY_ALIAS = "yap_session_key"
private const val KEY_STORE_TYPE = "AndroidKeyStore"
private const val TRANSFORMATION = "AES/GCM/NoPadding"
private const val GCM_TAG_LENGTH_BITS = 128
private const val GCM_IV_LENGTH_BYTES = 12

private val Context.sessionDataStore: DataStore<Preferences> by preferencesDataStore(DATA_STORE_NAME)

internal actual fun Scope.createSessionStorage(): SessionStorage =
    AndroidSessionStorage(context = get())

internal class AndroidSessionStorage(
    private val context: Context,
) : SessionStorage {

    private val key = stringPreferencesKey("session")

    override suspend fun clear() {
        context.sessionDataStore.edit { preferences -> preferences.remove(key) }
    }

    override suspend fun read(): SessionLocal? {
        val stored = context.sessionDataStore.data.firstOrNull()?.get(key) ?: return null
        return runCatching { Json.decodeFromString<SessionLocal>(decrypt(stored)) }.getOrNull()
    }

    override suspend fun write(session: SessionLocal) {
        val encrypted = encrypt(Json.encodeToString(session))
        context.sessionDataStore.edit { preferences -> preferences[key] = encrypted }
    }

    private fun encrypt(value: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val payload = cipher.iv + cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(payload, Base64.NO_WRAP)
    }

    private fun decrypt(value: String): String {
        val payload = Base64.decode(value, Base64.NO_WRAP)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            secretKey(),
            GCMParameterSpec(GCM_TAG_LENGTH_BITS, payload, 0, GCM_IV_LENGTH_BYTES),
        )
        val ciphertext = payload.copyOfRange(GCM_IV_LENGTH_BYTES, payload.size)
        return cipher.doFinal(ciphertext).toString(Charsets.UTF_8)
    }

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEY_STORE_TYPE).apply { load(null) }
        val existing = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
        if (existing != null) return existing.secretKey

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEY_STORE_TYPE)
        generator.init(
            KeyGenParameterSpec
                .Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build(),
        )
        return generator.generateKey()
    }
}
