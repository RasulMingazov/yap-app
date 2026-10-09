package app.yap.feature.auth.data.identity

import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException
import androidx.credentials.exceptions.NoCredentialException
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
        } catch (error: GetCredentialException) {
            error.browserFallbackOrThrow()
        }

        return response?.toIdToken() ?: googleBrowserAuthFlow.requestAuthorizationCode()
    }

    /**
     * `null` when the browser flow should take over, because no provider can answer on this device.
     *
     * Play services report every aborted Sign in with Google flow as a cancellation, including the
     * ones the user never got to finish — an app whose signing certificate is not registered in the
     * Google Cloud console comes back as "[16] Account reauth failed." under that very type. Only a
     * message that names the user as the one who cancelled stays silent; anything else propagates
     * as a failure so the screen can say so.
     */
    private fun GetCredentialException.browserFallbackOrThrow(): GetCredentialResponse? = when {
        this is GetCredentialCancellationException && isUserDismissal -> throw LoginCancelledException()
        this is GetCredentialProviderConfigurationException || this is NoCredentialException -> null
        else -> throw this
    }

    private val GetCredentialCancellationException.isUserDismissal: Boolean
        get() = message.isNullOrBlank() || USER_DISMISSAL.containsMatchIn(message.orEmpty())

    private fun GetCredentialResponse.toIdToken(): GoogleCredential.IdToken {
        val isGoogleIdToken =
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        require(isGoogleIdToken) { "Unexpected credential type ${credential.type}" }

        return GoogleCredential.IdToken(
            value = GoogleIdTokenCredential.createFrom(credential.data).idToken,
        )
    }

    private companion object {
        val USER_DISMISSAL = Regex("cancel{1,2}ed by (the )?user", RegexOption.IGNORE_CASE)
    }
}
