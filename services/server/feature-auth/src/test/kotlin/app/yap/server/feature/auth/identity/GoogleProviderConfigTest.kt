package app.yap.server.feature.auth.identity

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class GoogleProviderConfigTest {

    @Test
    fun `GIVEN no google server client id WHEN reading configuration THEN google is not configured`() {
        val result = GoogleProviderConfig.fromEnvironment { null }

        assertNull(result)
    }

    @Test
    fun `GIVEN a blank google server client id WHEN reading configuration THEN it fails as a configuration error`() {
        assertFailsWith<IllegalArgumentException> {
            GoogleProviderConfig.fromEnvironment { variable ->
                "".takeIf { variable == GoogleProviderConfig.SERVER_CLIENT_ID_VARIABLE }
            }
        }
    }

    @Test
    fun `GIVEN every google client id WHEN reading configuration THEN each one is an allowed authorized party`() {
        val result = GoogleProviderConfig.fromEnvironment { variable -> variable }

        assertEquals(
            expected = setOf(
                GoogleProviderConfig.ANDROID_CLIENT_ID_VARIABLE,
                GoogleProviderConfig.IOS_CLIENT_ID_VARIABLE,
                GoogleProviderConfig.SERVER_CLIENT_ID_VARIABLE,
            ),
            actual = result?.allowedAuthorizedParties,
        )
    }
}
