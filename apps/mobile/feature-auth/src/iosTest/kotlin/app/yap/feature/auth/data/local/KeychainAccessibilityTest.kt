package app.yap.feature.auth.data.local

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Security.kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalForeignApi::class)
internal class KeychainAccessibilityTest {

    @Test
    fun `GIVEN the device-only accessibility WHEN mapping it for SecItem THEN the after-first-unlock attribute is used`() {
        val attribute = KeychainAccessibility.AfterFirstUnlockThisDeviceOnly.toSecAttribute()

        assertEquals(
            expected = kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly,
            actual = attribute,
        )
    }
}
