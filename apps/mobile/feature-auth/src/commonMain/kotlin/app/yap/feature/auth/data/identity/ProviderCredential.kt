package app.yap.feature.auth.data.identity

internal sealed interface ProviderCredential {

    data class AuthorizationCode(
        val code: String,
        val codeVerifier: String,
        val redirectUri: String,
    ) : ProviderCredential

    data class IdentityToken(val idToken: String) : ProviderCredential
}
