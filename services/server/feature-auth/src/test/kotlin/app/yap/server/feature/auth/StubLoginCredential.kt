package app.yap.server.feature.auth

import app.yap.server.feature.auth.model.LoginCredential

internal object StubLoginCredential {

    const val AUTHORIZATION_CODE = "authorization-code"
    const val ID_TOKEN = "identity-token"

    /** The single redirect URI registered for the Android browser fallback. */
    const val REDIRECT_URI = "app.yap.oauth:/redirect"

    fun stubAuthorizationCode(
        code: String = AUTHORIZATION_CODE,
        codeVerifier: String = StubAuthChallenge.CODE_VERIFIER,
        redirectUri: String = REDIRECT_URI,
    ): LoginCredential.AuthorizationCode = LoginCredential.AuthorizationCode(
        code = code,
        codeVerifier = codeVerifier,
        redirectUri = redirectUri,
    )

    fun stubIdentityToken(
        idToken: String = ID_TOKEN,
    ): LoginCredential.IdentityToken = LoginCredential.IdentityToken(idToken = idToken)
}
