package app.yap.feature.auth.data.identity

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.coroutines.test.runTest

internal class IosSdkGoogleCredentialProviderTest {

    @Test
    fun `GIVEN the SDK returns an ID token WHEN a credential is requested THEN it comes back`() = runTest {
        val provider = IosSdkGoogleCredentialProvider { ID_TOKEN }

        assertEquals(
            expected = GoogleCredential.IdToken(value = ID_TOKEN),
            actual = provider.requestCredential(nonce = NONCE),
        )
    }

    @Test
    fun `GIVEN a nonce WHEN a credential is requested THEN the SDK receives it`() = runTest {
        var receivedNonce: String? = null
        val provider = IosSdkGoogleCredentialProvider { nonce ->
            receivedNonce = nonce
            ID_TOKEN
        }

        provider.requestCredential(nonce = NONCE)

        assertEquals(expected = NONCE, actual = receivedNonce)
    }

    @Test
    fun `GIVEN the SDK reports dismissal WHEN a credential is requested THEN it is a cancellation`() = runTest {
        val provider = IosSdkGoogleCredentialProvider { null }

        assertFailsWith<LoginCancelledException> {
            provider.requestCredential(nonce = NONCE)
        }
    }

    @Test
    fun `GIVEN the SDK returns an empty token WHEN a credential is requested THEN it fails`() = runTest {
        val provider = IosSdkGoogleCredentialProvider { "" }

        assertFailsWith<IllegalArgumentException> {
            provider.requestCredential(nonce = NONCE)
        }
    }

    private companion object {
        const val ID_TOKEN = "header.payload.signature"
        const val NONCE = "nonce-1"
    }
}
