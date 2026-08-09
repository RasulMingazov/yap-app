package app.yap.feature.auth.data.local

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.UByteVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import platform.CoreFoundation.CFDataCreate
import platform.CoreFoundation.CFDataGetBytePtr
import platform.CoreFoundation.CFDataGetLength
import platform.CoreFoundation.CFDataRef
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFDictionarySetValue
import platform.CoreFoundation.CFMutableDictionaryRef
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFStringCreateWithCString
import platform.CoreFoundation.CFStringRef
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.kCFBooleanTrue
import platform.CoreFoundation.kCFStringEncodingUTF8
import platform.CoreFoundation.kCFTypeDictionaryKeyCallBacks
import platform.CoreFoundation.kCFTypeDictionaryValueCallBacks
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

@OptIn(ExperimentalForeignApi::class)
internal class SecItemKeychain : Keychain {

    override fun delete(query: KeychainQuery) {
        val request = query.toSecQuery()
        SecItemDelete(request)
        CFRelease(request)
    }

    override fun read(query: KeychainQuery): String? = memScoped {
        val request = query.toSecQuery()
        CFDictionarySetValue(request, kSecReturnData, kCFBooleanTrue)
        CFDictionarySetValue(request, kSecMatchLimit, kSecMatchLimitOne)

        val found = alloc<CFTypeRefVar>()
        val status = SecItemCopyMatching(request, found.ptr)
        CFRelease(request)
        if (status != errSecSuccess) return@memScoped null

        val data: CFDataRef? = found.value?.reinterpret()
        val text = data?.toKotlinString()
        data?.let { CFRelease(it) }
        text
    }

    override fun write(query: KeychainQuery, value: String) {
        delete(query)

        val request = query.toSecQuery()
        CFDictionarySetValue(request, kSecAttrAccessible, query.accessibility.toSecAttribute())
        val data = value.encodeToByteArray().toCFData()
        CFDictionarySetValue(request, kSecValueData, data)
        SecItemAdd(request, null)
        CFRelease(data)
        CFRelease(request)
    }
}

@OptIn(ExperimentalForeignApi::class)
internal fun KeychainAccessibility.toSecAttribute(): CFStringRef? = when (this) {
    KeychainAccessibility.AfterFirstUnlockThisDeviceOnly ->
        kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
}

@OptIn(ExperimentalForeignApi::class)
private fun KeychainQuery.toSecQuery(): CFMutableDictionaryRef {
    val request = requireNotNull(
        CFDictionaryCreateMutable(
            null,
            0,
            kCFTypeDictionaryKeyCallBacks.ptr,
            kCFTypeDictionaryValueCallBacks.ptr,
        ),
    )
    CFDictionarySetValue(request, kSecClass, kSecClassGenericPassword)

    val serviceValue = service.toCFString()
    CFDictionarySetValue(request, kSecAttrService, serviceValue)
    CFRelease(serviceValue)

    val accountValue = account.toCFString()
    CFDictionarySetValue(request, kSecAttrAccount, accountValue)
    CFRelease(accountValue)

    return request
}

@OptIn(ExperimentalForeignApi::class)
private fun CFDataRef.toKotlinString(): String? {
    val length = CFDataGetLength(this).convert<Int>()
    val bytes = CFDataGetBytePtr(this)?.reinterpret<ByteVar>()?.readBytes(length) ?: return null
    return bytes.decodeToString()
}

@OptIn(ExperimentalForeignApi::class)
private fun ByteArray.toCFData(): CFDataRef? = usePinned { pinned ->
    CFDataCreate(null, pinned.addressOf(0).reinterpret<UByteVar>(), size.convert())
}

@OptIn(ExperimentalForeignApi::class)
private fun String.toCFString(): CFStringRef? =
    CFStringCreateWithCString(null, this, kCFStringEncodingUTF8)
