package app.yap.server.feature.auth.identity

import app.yap.server.feature.auth.model.AuthFailure
import com.auth0.jwk.JwkProvider
import com.auth0.jwk.JwkProviderBuilder
import com.auth0.jwk.NetworkException
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.DecodedJWT
import java.net.URI
import java.security.interfaces.RSAPublicKey
import java.util.concurrent.TimeUnit

private const val GOOGLE_CERTS_URL = "https://www.googleapis.com/oauth2/v3/certs"
private const val JWK_CACHE_SIZE = 10L
private const val JWK_CACHE_HOURS = 6L
private const val JWK_BUCKET_SIZE = 10L
private const val JWK_REFILL_RATE = 1L

private val ACCEPTED_ISSUERS = arrayOf("accounts.google.com", "https://accounts.google.com")

private const val EMAIL_CLAIM = "email"
private const val NAME_CLAIM = "name"
private const val NONCE_CLAIM = "nonce"
private const val PICTURE_CLAIM = "picture"

internal class GoogleIdentityVerifier(
    private val googleAuthConfig: GoogleAuthConfig,
    private val jwkProvider: JwkProvider,
) {

    fun verify(idToken: String, expectedNonce: String?): GoogleIdentity {
        val decoded = decode(idToken)
        val verified = verifySignatureAndClaims(decoded)

        verifyNonce(verified = verified, expectedNonce = expectedNonce)

        val subject = verified.subject?.takeIf(String::isNotBlank)
            ?: throw AuthFailure.UnverifiableConfirmation("Verified token carries no subject")

        return GoogleIdentity(
            subject = subject,
            email = verified.stringClaim(EMAIL_CLAIM),
            displayName = verified.stringClaim(NAME_CLAIM),
            avatarUrl = verified.stringClaim(PICTURE_CLAIM),
        )
    }

    private fun decode(idToken: String): DecodedJWT = runCatching { JWT.decode(idToken) }
        .getOrElse { throw AuthFailure.UnverifiableConfirmation("Token could not be read") }

    private fun verifySignatureAndClaims(decoded: DecodedJWT): DecodedJWT {
        val publicKey = publicKeyFor(decoded.keyId)
        return runCatching {
            JWT.require(Algorithm.RSA256(publicKey, null))
                .withIssuer(*ACCEPTED_ISSUERS)
                .withAnyOfAudience(*googleAuthConfig.acceptedAudiences.toTypedArray())
                .build()
                .verify(decoded)
        }.getOrElse { throw AuthFailure.UnverifiableConfirmation("Token failed verification") }
    }

    private fun publicKeyFor(keyId: String?): RSAPublicKey = runCatching {
        jwkProvider.get(keyId).publicKey as RSAPublicKey
    }.getOrElse { error ->
        if (error is NetworkException) {
            throw AuthFailure.ProviderUnavailable("Key set unreachable")
        }
        throw AuthFailure.UnverifiableConfirmation("Signing key is unknown")
    }

    private fun verifyNonce(verified: DecodedJWT, expectedNonce: String?) {
        if (expectedNonce == null) return
        if (verified.stringClaim(NONCE_CLAIM) != expectedNonce) {
            throw AuthFailure.UnverifiableConfirmation("Nonce does not match")
        }
    }

    private fun DecodedJWT.stringClaim(name: String): String? =
        getClaim(name).asString()?.takeIf(String::isNotBlank)
}

internal fun googleJwkProvider(): JwkProvider = JwkProviderBuilder(URI(GOOGLE_CERTS_URL).toURL())
    .cached(JWK_CACHE_SIZE, JWK_CACHE_HOURS, TimeUnit.HOURS)
    .rateLimited(JWK_BUCKET_SIZE, JWK_REFILL_RATE, TimeUnit.MINUTES)
    .build()
