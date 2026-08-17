package app.yap.feature.auth.data.identity

import app.yap.feature.auth.data.identity.GoogleCredential
import app.yap.feature.auth.data.identity.GoogleCredentialProvider
import io.github.rasulmingazov.stubcall.StubCall1

internal class StubGoogleCredentialProvider(
    credential: GoogleCredential = GoogleCredential.IdToken(value = "google-id-token"),
) : GoogleCredentialProvider {

    val requestCredentialCall = StubCall1.returns<String, GoogleCredential>(credential)

    override suspend fun requestCredential(nonce: String): GoogleCredential =
        requestCredentialCall.invoke(nonce)
}
