package app.yap.server.feature.auth.identity

import app.yap.server.feature.auth.StubAuth
import app.yap.server.feature.auth.StubAuthChallenge
import app.yap.server.feature.auth.StubLoginCredential
import app.yap.server.feature.auth.StubVerifiedIdentity
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.time.Instant

internal object StubGoogleIdToken {

    const val ANDROID_CLIENT_ID = "android-client-id"
    const val FALLBACK_REDIRECT_URI = StubLoginCredential.REDIRECT_URI
    const val IOS_CLIENT_ID = "ios-client-id"
    const val ISSUER = "https://accounts.google.com"
    const val KEY_ID = "google-key-id"
    const val SERVER_CLIENT_ID = "server-client-id"

    /** The nonce hash the challenge stored, as produced by [stubNonceHasher]. */
    const val NONCE_HASH = "hashed:${StubAuthChallenge.NONCE}"

    val EXPIRES_AT: Instant = StubAuth.NOW.plusSeconds(600)

    val googleKeyPair: KeyPair = generateKeyPair()
    val otherKeyPair: KeyPair = generateKeyPair()

    fun stubGoogleProviderConfig(
        androidClientId: String? = ANDROID_CLIENT_ID,
        fallbackRedirectUri: String? = FALLBACK_REDIRECT_URI,
        iosClientId: String? = IOS_CLIENT_ID,
        serverClientId: String = SERVER_CLIENT_ID,
    ): GoogleProviderConfig = GoogleProviderConfig(
        androidClientId = androidClientId,
        fallbackRedirectUri = fallbackRedirectUri,
        iosClientId = iosClientId,
        serverClientId = serverClientId,
    )

    fun stubNonceHasher(): NonceHasher = NonceHasher { value -> "hashed:$value" }

    @Suppress("LongParameterList")
    fun stubIdToken(
        audience: String = SERVER_CLIENT_ID,
        authorizedParty: String? = ANDROID_CLIENT_ID,
        email: String? = StubVerifiedIdentity.EMAIL,
        expiresAt: Instant = EXPIRES_AT,
        isEmailVerified: Boolean? = true,
        issuer: String = ISSUER,
        keyPair: KeyPair = googleKeyPair,
        nonce: String? = StubAuthChallenge.NONCE,
        subject: String? = StubVerifiedIdentity.SUBJECT,
    ): String = JWT.create()
        .withKeyId(KEY_ID)
        .withIssuer(issuer)
        .withAudience(audience)
        .withSubject(subject)
        .withExpiresAt(expiresAt)
        .withClaim("azp", authorizedParty)
        .withClaim("nonce", nonce)
        .withClaim("email", email)
        .withClaim("email_verified", isEmailVerified)
        .sign(
            Algorithm.RSA256(
                keyPair.public as RSAPublicKey,
                keyPair.private as RSAPrivateKey,
            ),
        )

    private fun generateKeyPair(): KeyPair = KeyPairGenerator.getInstance("RSA")
        .apply { initialize(2048) }
        .generateKeyPair()
}
