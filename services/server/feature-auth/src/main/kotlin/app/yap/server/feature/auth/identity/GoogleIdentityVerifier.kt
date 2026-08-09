package app.yap.server.feature.auth.identity

import app.yap.server.feature.auth.model.AuthFailure
import app.yap.server.feature.auth.model.AuthFailureException
import app.yap.server.feature.auth.model.LoginCredential
import app.yap.server.feature.auth.model.ProviderId
import app.yap.server.feature.auth.model.VerifiedIdentity
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.DecodedJWT
import java.time.Clock
import java.time.Instant

/**
 * Verifies Google identity tokens against Google's published signing keys. Nothing is derived from
 * a locally decoded token: signature, issuer, audience, authorized party, expiry, subject, and the
 * challenge nonce are all checked before an identity is returned.
 *
 * Every rejection collapses into [AuthFailure.ChallengeInvalid], so a client learns that the
 * attempt failed and nothing about which check rejected it.
 */
internal class GoogleIdentityVerifier(
    private val clock: Clock,
    private val config: GoogleProviderConfig,
    private val nonceHasher: NonceHasher,
    private val signingKeys: SigningKeyProvider,
    private val tokenExchange: GoogleTokenExchange,
) : IdentityVerifier {

    override val providerId: ProviderId = ProviderId.Google

    override val supportsAuthorizationCode: Boolean = true

    override suspend fun verify(credential: LoginCredential, nonceHash: String?): VerifiedIdentity {
        val idToken = when (credential) {
            is LoginCredential.AuthorizationCode -> exchange(credential)
            is LoginCredential.IdentityToken -> credential.idToken
        }

        return identityOf(token = verifiedToken(idToken), nonceHash = nonceHash)
    }

    private suspend fun exchange(credential: LoginCredential.AuthorizationCode): String {
        val redirectUri = config.fallbackRedirectUri
            ?: throw AuthFailureException(AuthFailure.ProviderUnavailable)
        if (credential.redirectUri != redirectUri) throw challengeInvalid()

        return tokenExchange.exchange(
            code = credential.code,
            codeVerifier = credential.codeVerifier,
            redirectUri = redirectUri,
        )
    }

    private fun verifiedToken(idToken: String): DecodedJWT {
        val token = signatureVerifiedToken(idToken)
        if (!isCorrectlyScoped(token)) throw challengeInvalid()

        return token
    }

    private fun signatureVerifiedToken(idToken: String): DecodedJWT = runCatching {
        val token = JWT.decode(idToken)
        Algorithm.RSA256(signingKeys.publicKey(token.keyId), null).verify(token)
        token
    }.getOrElse { throw challengeInvalid() }

    private fun isCorrectlyScoped(token: DecodedJWT): Boolean {
        val authorizedParty = token.getClaim(AUTHORIZED_PARTY_CLAIM).asString()
        val expiresAt = token.expiresAtAsInstant

        return token.issuer in ISSUERS &&
            config.serverClientId in token.audience.orEmpty() &&
            (authorizedParty == null || authorizedParty in config.allowedAuthorizedParties) &&
            expiresAt != null &&
            expiresAt.isAfter(Instant.now(clock))
    }

    private fun identityOf(token: DecodedJWT, nonceHash: String?): VerifiedIdentity {
        val nonce = token.getClaim(NONCE_CLAIM).asString()
        val subject = token.subject
        if (subject.isNullOrBlank() || nonce == null || nonceHash == null) throw challengeInvalid()
        if (nonceHasher.hash(nonce) != nonceHash) throw challengeInvalid()

        return VerifiedIdentity(
            email = token.getClaim(EMAIL_CLAIM).asString(),
            isEmailVerified = token.getClaim(EMAIL_VERIFIED_CLAIM).asBoolean(),
            provider = providerId,
            subject = subject,
        )
    }

    private fun challengeInvalid(): AuthFailureException =
        AuthFailureException(AuthFailure.ChallengeInvalid)

    private companion object {
        const val AUTHORIZED_PARTY_CLAIM = "azp"
        const val EMAIL_CLAIM = "email"
        const val EMAIL_VERIFIED_CLAIM = "email_verified"
        const val NONCE_CLAIM = "nonce"
        val ISSUERS = setOf("https://accounts.google.com", "accounts.google.com")
    }
}
