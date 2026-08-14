package app.yap.feature.auth.data.local

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import kotlinx.serialization.json.Json
import org.koin.core.scope.Scope
import platform.CoreFoundation.CFDictionaryAddValue
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFMutableDictionaryRef
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.kCFBooleanTrue
import platform.CoreFoundation.kCFTypeDictionaryKeyCallBacks
import platform.CoreFoundation.kCFTypeDictionaryValueCallBacks
import platform.Foundation.CFBridgingRelease
import platform.Foundation.CFBridgingRetain
import platform.Foundation.NSData
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccessible
import platform.Security.kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnData
import platform.Security.kSecValueData

private const val SERVICE = "app.yap.auth"
private const val ACCOUNT = "session"

internal actual fun Scope.createSessionStorage(): SessionStorage = KeychainSessionStorage()

@OptIn(ExperimentalForeignApi::class)
internal class KeychainSessionStorage : SessionStorage {

    override suspend fun clear() {
        val query = baseQuery()
        SecItemDelete(query)
        platform.CoreFoundation.CFRelease(query)
    }

    override suspend fun read(): SessionLocal? = memScoped {
        val query = baseQuery()
        CFDictionaryAddValue(query, kSecReturnData, kCFBooleanTrue)
        CFDictionaryAddValue(query, kSecMatchLimit, kSecMatchLimitOne)

        val holder = alloc<CFTypeRefVar>()
        val status = SecItemCopyMatching(query, holder.ptr)
        platform.CoreFoundation.CFRelease(query)
        if (status != errSecSuccess) return@memScoped null

        val data = CFBridgingRelease(holder.value) as? NSData ?: return@memScoped null
        val json = NSString.create(data = data, encoding = NSUTF8StringEncoding) as String?
            ?: return@memScoped null
        runCatching { Json.decodeFromString<SessionLocal>(json) }.getOrNull()
    }

    override suspend fun write(session: SessionLocal) {
        clear()

        val json = Json.encodeToString(session) as NSString
        val data = json.dataUsingEncoding(NSUTF8StringEncoding) ?: return
        val query = baseQuery()
        val value = CFBridgingRetain(data)
        CFDictionaryAddValue(query, kSecValueData, value)
        CFDictionaryAddValue(
            query,
            kSecAttrAccessible,
            kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly,
        )
        SecItemAdd(query, null)
        platform.CoreFoundation.CFRelease(value)
        platform.CoreFoundation.CFRelease(query)
    }

    private fun baseQuery(): CFMutableDictionaryRef {
        val query = CFDictionaryCreateMutable(
            null,
            0,
            kCFTypeDictionaryKeyCallBacks.ptr,
            kCFTypeDictionaryValueCallBacks.ptr,
        )
        CFDictionaryAddValue(query, kSecClass, kSecClassGenericPassword)
        CFDictionaryAddValue(query, kSecAttrService, CFBridgingRetain(SERVICE as NSString))
        CFDictionaryAddValue(query, kSecAttrAccount, CFBridgingRetain(ACCOUNT as NSString))
        return query!!
    }
}
