package app.yap.server.feature.auth.api

import app.yap.contract.auth.LoginChallengeRequestDto
import app.yap.contract.auth.LoginRequestDto
import app.yap.server.feature.auth.identity.CodeChallenge
import app.yap.server.feature.auth.model.LoginCredential

/** The credential shapes a login request may name. */
private const val AUTHORIZATION_CODE_TYPE = "authorization_code"
private const val IDENTITY_TOKEN_TYPE = "identity_token"

/**
 * A code challenge and its method are accepted only together, and only for the single method this
 * server supports. Whether the resolved provider accepts a code challenge at all is the feature's
 * decision, not the wire format's.
 */
internal fun LoginChallengeRequestDto.hasAcceptableCodeChallenge(): Boolean = when (codeChallenge) {
    null -> codeChallengeMethod == null
    else -> codeChallengeMethod == CodeChallenge.METHOD
}

/**
 * Translates the wire shape into the credential it carries, or `null` when the fields do not form
 * an accepted combination: each credential type requires its own fields and tolerates no field
 * belonging to the other one.
 *
 * The redirect URI is only checked for presence here. Comparing it verbatim with the registered
 * per-provider value belongs to the provider's verifier, which owns that configuration and rejects
 * a mismatch before any token exchange.
 */
internal fun LoginRequestDto.toLoginCredential(): LoginCredential? = when (credentialType) {
    AUTHORIZATION_CODE_TYPE -> toAuthorizationCode()
    IDENTITY_TOKEN_TYPE -> toIdentityToken()
    else -> null
}

private fun LoginRequestDto.toAuthorizationCode(): LoginCredential.AuthorizationCode? {
    val isAuthorizationCode = idToken == null &&
        authorizationCode != null &&
        codeVerifier != null &&
        redirectUri != null
    if (!isAuthorizationCode) return null

    return LoginCredential.AuthorizationCode(
        code = checkNotNull(authorizationCode),
        codeVerifier = checkNotNull(codeVerifier),
        redirectUri = checkNotNull(redirectUri),
    )
}

private fun LoginRequestDto.toIdentityToken(): LoginCredential.IdentityToken? {
    val isIdentityToken = idToken != null &&
        authorizationCode == null &&
        codeVerifier == null &&
        redirectUri == null
    if (!isIdentityToken) return null

    return LoginCredential.IdentityToken(idToken = checkNotNull(idToken))
}
