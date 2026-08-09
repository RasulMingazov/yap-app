package app.yap.feature.auth.data.identity

internal object StubPreparedAttempt {

    const val ATTEMPT_ID = "attempt-id"
    const val AUTHORIZATION_CODE = "authorization-code"
    const val CODE_CHALLENGE = "code-challenge"
    const val CODE_VERIFIER = "code-verifier"
    const val ID_TOKEN = "id-token"
    const val REDIRECT_URI = "app.yap://oauth"

    fun stubPreparedAttempt(
        attemptId: String = ATTEMPT_ID,
        codeChallenge: String? = CODE_CHALLENGE,
        codeVerifier: String? = CODE_VERIFIER,
    ): PreparedAttempt = PreparedAttempt(
        attemptId = attemptId,
        codeChallenge = codeChallenge,
        codeVerifier = codeVerifier,
    )

    fun stubAuthorizationCodeCredential(
        code: String = AUTHORIZATION_CODE,
        codeVerifier: String = CODE_VERIFIER,
        redirectUri: String = REDIRECT_URI,
    ): ProviderCredential = ProviderCredential.AuthorizationCode(
        code = code,
        codeVerifier = codeVerifier,
        redirectUri = redirectUri,
    )

    fun stubIdentityTokenCredential(
        idToken: String = ID_TOKEN,
    ): ProviderCredential = ProviderCredential.IdentityToken(idToken = idToken)
}
