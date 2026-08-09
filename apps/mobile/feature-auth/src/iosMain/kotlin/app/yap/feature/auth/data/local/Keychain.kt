package app.yap.feature.auth.data.local

/**
 * Narrow seam over `SecItem*`. The session item is always written with a device-only
 * accessibility class (R-079, AC-050).
 */
internal interface Keychain {

    fun delete(query: KeychainQuery)

    fun read(query: KeychainQuery): String?

    fun write(query: KeychainQuery, value: String)
}

internal data class KeychainQuery(
    val accessibility: KeychainAccessibility,
    val account: String,
    val service: String,
)

internal enum class KeychainAccessibility {
    AfterFirstUnlockThisDeviceOnly,
}
