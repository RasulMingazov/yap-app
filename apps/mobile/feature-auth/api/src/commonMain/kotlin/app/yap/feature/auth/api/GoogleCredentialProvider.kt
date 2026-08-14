package app.yap.feature.auth.api

interface GoogleCredentialProvider {

    suspend fun requestCredential(nonce: String): GoogleCredential
}

sealed interface GoogleCredential {

    data class IdToken(val value: String) : GoogleCredential

    data class AuthorizationCode(
        val code: String,
        val codeVerifier: String,
        val redirectUri: String,
    ) : GoogleCredential
}
