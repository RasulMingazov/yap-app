package app.yap.feature.auth.data.identity

import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class AndroidGoogleCredentialProviderTest {

    @Test
    fun `GIVEN a credential provider answers WHEN a credential is requested THEN an id token comes back`() = runTest {
        val env = Environment(outcome = CredentialManagerOutcome.Success)

        val credential = env.provider.requestCredential(nonce = NONCE)

        assertEquals(expected = GoogleCredential.IdToken(value = ID_TOKEN), actual = credential)
        assertEquals(expected = 0, actual = env.browserAuthFlow.callCount)
    }

    @Test
    fun `GIVEN no credential provider is configured WHEN a credential is requested THEN the browser answers`() =
        runTest {
            val env = Environment(outcome = CredentialManagerOutcome.NoProviderConfigured)

            val credential = env.provider.requestCredential(nonce = NONCE)

            assertEquals(expected = env.browserAuthFlow.credential, actual = credential)
            assertEquals(expected = 1, actual = env.browserAuthFlow.callCount)
        }

    @Test
    fun `GIVEN no credential is available WHEN a credential is requested THEN the browser answers`() = runTest {
        val env = Environment(outcome = CredentialManagerOutcome.NoCredential)

        val credential = env.provider.requestCredential(nonce = NONCE)

        assertEquals(expected = env.browserAuthFlow.credential, actual = credential)
        assertEquals(expected = 1, actual = env.browserAuthFlow.callCount)
    }

    @Test
    fun `GIVEN the user dismisses the sheet WHEN a credential is requested THEN it is a cancellation`() = runTest {
        val env = Environment(outcome = CredentialManagerOutcome.Cancelled)

        assertFailsWith<LoginCancelledException> { env.provider.requestCredential(nonce = NONCE) }
    }

    @Test
    fun `GIVEN the user dismisses the sheet WHEN a credential is requested THEN the browser never opens`() = runTest {
        val env = Environment(outcome = CredentialManagerOutcome.Cancelled)

        runCatching { env.provider.requestCredential(nonce = NONCE) }

        assertEquals(expected = 0, actual = env.browserAuthFlow.callCount)
    }

    @Test
    fun `GIVEN a credential provider answers WHEN a credential is requested THEN the nonce is submitted`() = runTest {
        val env = Environment(outcome = CredentialManagerOutcome.Success)

        env.provider.requestCredential(nonce = NONCE)

        assertEquals(expected = NONCE, actual = env.credentialRequester.submittedNonce())
    }

    private enum class CredentialManagerOutcome {
        Success,
        NoProviderConfigured,
        NoCredential,
        Cancelled,
    }

    private class StubCredentialRequester(
        private val outcome: CredentialManagerOutcome,
    ) : CredentialRequester {

        private val requests = mutableListOf<GetCredentialRequest>()

        @Suppress("ThrowsCount")
        override suspend fun request(request: GetCredentialRequest): GetCredentialResponse {
            requests += request
            return when (outcome) {
                CredentialManagerOutcome.Success -> GetCredentialResponse(
                    GoogleIdTokenCredential.Builder()
                        .setId(GOOGLE_ACCOUNT_ID)
                        .setIdToken(ID_TOKEN)
                        .build(),
                )

                CredentialManagerOutcome.NoProviderConfigured ->
                    throw GetCredentialProviderConfigurationException()

                CredentialManagerOutcome.NoCredential -> throw NoCredentialException()

                CredentialManagerOutcome.Cancelled -> throw GetCredentialCancellationException()
            }
        }

        fun submittedNonce(): String? = GetSignInWithGoogleOption
            .createFrom(requests.single().credentialOptions.single().requestData)
            .nonce
    }

    private class StubGoogleBrowserAuthFlow : GoogleBrowserAuthFlow {

        val credential = GoogleCredential.AuthorizationCode(
            code = "browser-code",
            codeVerifier = "browser-verifier",
            redirectUri = "com.googleusercontent.apps.android:/oauth2redirect",
        )

        var callCount: Int = 0
            private set

        override suspend fun requestAuthorizationCode(): GoogleCredential.AuthorizationCode {
            callCount += 1
            return credential
        }
    }

    private class Environment(
        outcome: CredentialManagerOutcome,
    ) {

        val browserAuthFlow = StubGoogleBrowserAuthFlow()
        val credentialRequester = StubCredentialRequester(outcome = outcome)
        val provider = AndroidGoogleCredentialProvider(
            credentialRequester = credentialRequester,
            googleBrowserAuthFlow = browserAuthFlow,
            googleServerClientId = SERVER_CLIENT_ID,
        )
    }

    private companion object {
        const val GOOGLE_ACCOUNT_ID = "learner@example.com"
        const val ID_TOKEN =
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ1c2VyLTEifQ.signature"
        const val NONCE = "nonce-1"
        const val SERVER_CLIENT_ID = "web-client.apps.googleusercontent.com"
    }
}
