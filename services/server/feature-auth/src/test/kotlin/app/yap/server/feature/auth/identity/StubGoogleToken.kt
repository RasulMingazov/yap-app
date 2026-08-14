package app.yap.server.feature.auth.identity

import com.auth0.jwk.Jwk
import com.auth0.jwk.JwkProvider
import com.auth0.jwk.NetworkException
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import java.math.BigInteger
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.time.Instant
import java.util.Base64
import java.util.Date

internal object StubGoogleToken {

    const val KEY_ID = "test-key"
    const val OTHER_KEY_ID = "other-key"
    const val ISSUER = "https://accounts.google.com"
    const val WEB_CLIENT_ID = "web-client.apps.googleusercontent.com"
    const val ANDROID_CLIENT_ID = "android-client.apps.googleusercontent.com"
    const val IOS_CLIENT_ID = "ios-client.apps.googleusercontent.com"
    const val SUBJECT = "google-sub-1"
    const val EMAIL = "learner@example.com"
    const val DISPLAY_NAME = "Learner"
    const val AVATAR_URL = "https://example.com/avatar.png"
    const val NONCE = "nonce-1"

    private const val KEY_SIZE = 2048

    val keyPair: KeyPair by lazy {
        KeyPairGenerator.getInstance("RSA").apply { initialize(KEY_SIZE) }.generateKeyPair()
    }

    val otherKeyPair: KeyPair by lazy {
        KeyPairGenerator.getInstance("RSA").apply { initialize(KEY_SIZE) }.generateKeyPair()
    }

    fun stubGoogleAuthConfig(
        androidClientId: String = ANDROID_CLIENT_ID,
        iosClientId: String = IOS_CLIENT_ID,
        webClientId: String = WEB_CLIENT_ID,
    ): GoogleAuthConfig = GoogleAuthConfig(
        androidClientId = androidClientId,
        iosClientId = iosClientId,
        webClientId = webClientId,
    )

    @Suppress("LongParameterList")
    fun stubIdToken(
        audience: String = WEB_CLIENT_ID,
        avatarUrl: String? = AVATAR_URL,
        displayName: String? = DISPLAY_NAME,
        email: String? = EMAIL,
        expiresAt: Instant = Instant.now().plusSeconds(600),
        issuer: String = ISSUER,
        keyId: String = KEY_ID,
        nonce: String? = NONCE,
        signingKeyPair: KeyPair = keyPair,
        subject: String? = SUBJECT,
    ): String = JWT.create()
        .withKeyId(keyId)
        .withIssuer(issuer)
        .withAudience(audience)
        .withSubject(subject)
        .withClaim("email", email)
        .withClaim("name", displayName)
        .withClaim("picture", avatarUrl)
        .withClaim("nonce", nonce)
        .withIssuedAt(Date.from(Instant.now().minusSeconds(60)))
        .withExpiresAt(Date.from(expiresAt))
        .sign(
            Algorithm.RSA256(
                signingKeyPair.public as RSAPublicKey,
                signingKeyPair.private as RSAPrivateKey,
            ),
        )

    fun stubJwkProvider(): JwkProvider = JwkProvider { keyId ->
        require(keyId == KEY_ID) { "unknown key $keyId" }
        keyPair.public.toJwk(keyId)
    }

    fun stubIdentityVerifier(): GoogleIdentityVerifier = GoogleIdentityVerifier(
        googleAuthConfig = stubGoogleAuthConfig(),
        jwkProvider = stubJwkProvider(),
    )

    fun stubUnusedCodeExchanger(): GoogleCodeExchanger = GoogleCodeExchanger(
        googleAuthConfig = stubGoogleAuthConfig(),
        googleIdentityVerifier = GoogleIdentityVerifier(
            googleAuthConfig = stubGoogleAuthConfig(),
            jwkProvider = stubJwkProvider(),
        ),
        httpClient = HttpClient(
            MockEngine { error("the authorization code door was not expected to be used") },
        ),
    )

    fun stubUnreachableJwkProvider(): JwkProvider = JwkProvider {
        throw NetworkException("key set unreachable", java.io.IOException("connect timed out"))
    }

    private fun java.security.PublicKey.toJwk(keyId: String): Jwk {
        val rsaKey = this as RSAPublicKey
        return Jwk.fromValues(
            mapOf(
                "kty" to "RSA",
                "kid" to keyId,
                "alg" to "RS256",
                "use" to "sig",
                "n" to rsaKey.modulus.toBase64Url(),
                "e" to rsaKey.publicExponent.toBase64Url(),
            ),
        )
    }

    private fun BigInteger.toBase64Url(): String = Base64.getUrlEncoder()
        .withoutPadding()
        .encodeToString(toByteArray().dropWhile { it == 0.toByte() }.toByteArray())
}
