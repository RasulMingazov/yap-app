package app.yap.feature.auth.data.local

internal const val SESSION_KEYCHAIN_ACCOUNT = "session"
internal const val SESSION_KEYCHAIN_SERVICE = "app.yap.feature.auth"

internal class KeychainSessionStorage(
    private val keychain: Keychain,
) : SessionStorage {

    private val query = KeychainQuery(
        accessibility = KeychainAccessibility.AfterFirstUnlockThisDeviceOnly,
        account = SESSION_KEYCHAIN_ACCOUNT,
        service = SESSION_KEYCHAIN_SERVICE,
    )

    override suspend fun clear() {
        keychain.delete(query)
    }

    override suspend fun read(): SessionLocal? {
        val stored = keychain.read(query) ?: return null
        return SessionSerialization.decode(stored)
    }

    override suspend fun write(session: SessionLocal) {
        keychain.write(query, SessionSerialization.encode(session))
    }
}

/** A device-only Keychain item; the session never leaves this device (R-079). */
internal fun createSessionStorage(): SessionStorage =
    KeychainSessionStorage(keychain = SecItemKeychain())
