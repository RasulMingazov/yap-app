package app.yap.feature.auth.data.local

/**
 * Platform secure storage for the Yap session: an AES-GCM blob in private app storage with its
 * key in Android Keystore, and a device-only Keychain item on iOS (R-079).
 */
internal interface SessionStorage {

    suspend fun clear()

    suspend fun read(): SessionLocal?

    suspend fun write(session: SessionLocal)
}
