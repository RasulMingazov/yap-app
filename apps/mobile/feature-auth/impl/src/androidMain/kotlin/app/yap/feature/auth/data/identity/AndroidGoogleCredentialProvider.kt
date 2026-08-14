package app.yap.feature.auth.data.identity

import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException
import androidx.credentials.exceptions.NoCredentialException
import app.yap.feature.auth.api.GoogleCredential
import app.yap.feature.auth.api.GoogleCredentialProvider
import app.yap.feature.auth.api.LoginCancelledException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

internal class AndroidGoogleCredentialProvider(
    private val credentialRequester: CredentialRequester,
    private val googleBrowserAuthFlow: GoogleBrowserAuthFlow,
    private val googleServerClientId: String,
) : GoogleCredentialProvider {

    override suspend fun requestCredential(nonce: String): GoogleCredential {
        val response = try {
            credentialRequester.request(
                GetCredentialRequest.Builder()
                    .addCredentialOption(
                        GetSignInWithGoogleOption.Builder(googleServerClientId)
                            .setNonce(nonce)
                            .build(),
                    )
                    .build(),
            )
        } catch (_: GetCredentialCancellationException) {
            throw LoginCancelledException()
        } catch (error: GetCredentialException) {
            val hasNoProvider = error is GetCredentialProviderConfigurationException ||
                error is NoCredentialException
            if (!hasNoProvider) throw error
            null
        }

        return response?.toIdToken() ?: googleBrowserAuthFlow.requestAuthorizationCode()
    }

    private fun GetCredentialResponse.toIdToken(): GoogleCredential.IdToken {
        val isGoogleIdToken =
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        require(isGoogleIdToken) { "Unexpected credential type ${credential.type}" }

        return GoogleCredential.IdToken(
            value = GoogleIdTokenCredential.createFrom(credential.data).idToken,
        )
    }
}
