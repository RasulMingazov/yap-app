package app.yap.server.feature.auth.api

import app.yap.contract.auth.LoginChallengeRequestDto
import app.yap.contract.auth.LoginRequestDto
import app.yap.contract.auth.RefreshRequestDto
import app.yap.server.feature.auth.StubAuth
import app.yap.server.feature.auth.StubAuthChallenge
import app.yap.server.feature.auth.StubAuthSession
import app.yap.server.feature.auth.StubLoginCredential

/** The wire requests the authentication routes accept, exactly as a client sends them. */
internal object StubAuthRequest {

    const val AUTHORIZATION_CODE_TYPE = "authorization_code"
    const val IDENTITY_TOKEN_TYPE = "identity_token"

    /** The only code-challenge method the API accepts. */
    const val S256_METHOD = "S256"

    fun stubLoginChallengeRequest(
        codeChallenge: String? = null,
        codeChallengeMethod: String? = null,
        provider: String = StubAuth.PROVIDER,
    ): LoginChallengeRequestDto = LoginChallengeRequestDto(
        provider = provider,
        codeChallenge = codeChallenge,
        codeChallengeMethod = codeChallengeMethod,
    )

    /** The Credential Manager and iOS shape: an identity token and nothing else. */
    fun stubLoginRequest(
        authorizationCode: String? = null,
        challengeId: String = StubAuthChallenge.CHALLENGE_ID,
        codeVerifier: String? = null,
        credentialType: String = IDENTITY_TOKEN_TYPE,
        idToken: String? = StubLoginCredential.ID_TOKEN,
        provider: String = StubAuth.PROVIDER,
        redirectUri: String? = null,
    ): LoginRequestDto = LoginRequestDto(
        challengeId = challengeId,
        provider = provider,
        credentialType = credentialType,
        idToken = idToken,
        authorizationCode = authorizationCode,
        codeVerifier = codeVerifier,
        redirectUri = redirectUri,
    )

    /** The Android browser fallback shape: an authorization code bound by PKCE. */
    fun stubAuthorizationCodeLoginRequest(
        authorizationCode: String? = StubLoginCredential.AUTHORIZATION_CODE,
        codeVerifier: String? = StubAuthChallenge.CODE_VERIFIER,
        idToken: String? = null,
        redirectUri: String? = StubLoginCredential.REDIRECT_URI,
    ): LoginRequestDto = stubLoginRequest(
        authorizationCode = authorizationCode,
        codeVerifier = codeVerifier,
        credentialType = AUTHORIZATION_CODE_TYPE,
        idToken = idToken,
        redirectUri = redirectUri,
    )

    fun stubRefreshRequest(
        refreshToken: String = StubAuthSession.REFRESH_TOKEN,
    ): RefreshRequestDto = RefreshRequestDto(refreshToken = refreshToken)
}
