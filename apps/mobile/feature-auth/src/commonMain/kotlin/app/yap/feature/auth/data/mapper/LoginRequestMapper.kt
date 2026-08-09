package app.yap.feature.auth.data.mapper

import app.yap.contract.auth.LoginChallengeRequestDto
import app.yap.contract.auth.LoginRequestDto
import app.yap.feature.auth.data.identity.PreparedAttempt
import app.yap.feature.auth.data.identity.ProviderCredential
import app.yap.feature.auth.domain.entity.LoginProviderId

private const val AUTHORIZATION_CODE = "authorization_code"
private const val CODE_CHALLENGE_METHOD_S256 = "S256"
private const val IDENTITY_TOKEN = "identity_token"

/** Only the public S256 code challenge leaves the device here (R-096, AC-062). */
internal fun PreparedAttempt.toChallengeRequest(
    providerId: LoginProviderId,
): LoginChallengeRequestDto = LoginChallengeRequestDto(
    provider = providerId.id,
    codeChallenge = codeChallenge,
    codeChallengeMethod = codeChallenge?.let { CODE_CHALLENGE_METHOD_S256 },
)

/** The `codeVerifier` reaches the backend only here, together with the authorization code (R-098). */
internal fun ProviderCredential.toLoginRequest(
    challengeId: String,
    providerId: LoginProviderId,
): LoginRequestDto = when (this) {
    is ProviderCredential.AuthorizationCode -> LoginRequestDto(
        challengeId = challengeId,
        provider = providerId.id,
        credentialType = AUTHORIZATION_CODE,
        authorizationCode = code,
        codeVerifier = codeVerifier,
        redirectUri = redirectUri,
    )

    is ProviderCredential.IdentityToken -> LoginRequestDto(
        challengeId = challengeId,
        provider = providerId.id,
        credentialType = IDENTITY_TOKEN,
        idToken = idToken,
    )
}
