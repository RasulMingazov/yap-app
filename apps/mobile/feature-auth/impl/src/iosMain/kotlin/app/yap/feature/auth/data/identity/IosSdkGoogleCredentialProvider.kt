package app.yap.feature.auth.data.identity

internal class IosSdkGoogleCredentialProvider(
    private val requestIdToken: suspend (nonce: String) -> String?,
) : GoogleCredentialProvider {

    override suspend fun requestCredential(nonce: String): GoogleCredential {
        val idToken = requestIdToken(nonce) ?: throw LoginCancelledException()

        return GoogleCredential.IdToken(
            value = requireNotNull(idToken.takeIf(String::isNotBlank)) {
                "GoogleSignIn returned an empty ID token"
            },
        )
    }
}
