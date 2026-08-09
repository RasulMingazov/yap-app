package app.yap.feature.auth.data.local

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private const val SESSION_FILE_NAME = "yap-session.bin"

internal class AndroidSessionStorage(
    private val cipher: SessionCipher,
    private val file: File,
) : SessionStorage {

    override suspend fun clear() {
        withContext(Dispatchers.IO) { file.delete() }
    }

    override suspend fun read(): SessionDb? = withContext(Dispatchers.IO) {
        if (!file.exists()) return@withContext null

        val decrypted = cipher.decrypt(file.readBytes()) ?: return@withContext null
        SessionSerialization.decode(decrypted.decodeToString())
    }

    override suspend fun write(session: SessionDb) {
        withContext(Dispatchers.IO) {
            file.parentFile?.mkdirs()
            val plain = SessionSerialization.encode(session).encodeToByteArray()
            file.writeBytes(cipher.encrypt(plain))
        }
    }
}

/** Private app storage plus a Keystore-held AES-GCM key (R-079). */
internal fun createSessionStorage(context: Context): SessionStorage = AndroidSessionStorage(
    cipher = AesGcmSessionCipher(secretKeyProvider = KeystoreSessionSecretKeyProvider()),
    file = File(context.filesDir, SESSION_FILE_NAME),
)
