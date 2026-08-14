package app.yap.server.feature.auth.identity

import app.yap.server.feature.auth.model.AuthFailure
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

internal class GoogleIdentityVerifierTest {

    @Test
    fun `GIVEN a correctly signed token WHEN it is verified THEN the identity claims are read`() {
        val env = Environment()

        val identity = env.verifier.verify(
            idToken = StubGoogleToken.stubIdToken(),
            expectedNonce = StubGoogleToken.NONCE,
        )

        assertEquals(
            expected = GoogleIdentity(
                subject = StubGoogleToken.SUBJECT,
                email = StubGoogleToken.EMAIL,
                displayName = StubGoogleToken.DISPLAY_NAME,
                avatarUrl = StubGoogleToken.AVATAR_URL,
            ),
            actual = identity,
        )
    }

    @Test
    fun `GIVEN a token signed by another key WHEN it is verified THEN it is refused`() {
        val env = Environment()

        assertFailsWith<AuthFailure.UnverifiableConfirmation> {
            env.verifier.verify(
                idToken = StubGoogleToken.stubIdToken(
                    keyId = StubGoogleToken.OTHER_KEY_ID,
                    signingKeyPair = StubGoogleToken.otherKeyPair,
                ),
                expectedNonce = StubGoogleToken.NONCE,
            )
        }
    }

    @Test
    fun `GIVEN a token for another audience WHEN it is verified THEN it is refused`() {
        val env = Environment()

        assertFailsWith<AuthFailure.UnverifiableConfirmation> {
            env.verifier.verify(
                idToken = StubGoogleToken.stubIdToken(audience = "someone-else.apps.googleusercontent.com"),
                expectedNonce = StubGoogleToken.NONCE,
            )
        }
    }

    @Test
    fun `GIVEN a token from another issuer WHEN it is verified THEN it is refused`() {
        val env = Environment()

        assertFailsWith<AuthFailure.UnverifiableConfirmation> {
            env.verifier.verify(
                idToken = StubGoogleToken.stubIdToken(issuer = "https://accounts.example.com"),
                expectedNonce = StubGoogleToken.NONCE,
            )
        }
    }

    @Test
    fun `GIVEN an expired token WHEN it is verified THEN it is refused`() {
        val env = Environment()

        assertFailsWith<AuthFailure.UnverifiableConfirmation> {
            env.verifier.verify(
                idToken = StubGoogleToken.stubIdToken(expiresAt = Instant.now().minusSeconds(600)),
                expectedNonce = StubGoogleToken.NONCE,
            )
        }
    }

    @Test
    fun `GIVEN a token carrying another nonce WHEN it is verified THEN it is refused`() {
        val env = Environment()

        assertFailsWith<AuthFailure.UnverifiableConfirmation> {
            env.verifier.verify(
                idToken = StubGoogleToken.stubIdToken(nonce = "nonce-2"),
                expectedNonce = StubGoogleToken.NONCE,
            )
        }
    }

    @Test
    fun `GIVEN a token carrying no subject WHEN it is verified THEN it is refused`() {
        val env = Environment()

        assertFailsWith<AuthFailure.UnverifiableConfirmation> {
            env.verifier.verify(
                idToken = StubGoogleToken.stubIdToken(subject = null),
                expectedNonce = StubGoogleToken.NONCE,
            )
        }
    }

    @Test
    fun `GIVEN a token omitting the display name and avatar WHEN it is verified THEN both are absent`() {
        val env = Environment()

        val identity = env.verifier.verify(
            idToken = StubGoogleToken.stubIdToken(avatarUrl = null, displayName = null),
            expectedNonce = StubGoogleToken.NONCE,
        )

        assertEquals(
            expected = GoogleIdentity(
                subject = StubGoogleToken.SUBJECT,
                email = StubGoogleToken.EMAIL,
                displayName = null,
                avatarUrl = null,
            ),
            actual = identity,
        )
    }

    @Test
    fun `GIVEN a token minted for the Android client WHEN it is verified THEN it is accepted`() {
        val env = Environment()

        val identity = env.verifier.verify(
            idToken = StubGoogleToken.stubIdToken(audience = StubGoogleToken.ANDROID_CLIENT_ID),
            expectedNonce = StubGoogleToken.NONCE,
        )

        assertEquals(expected = StubGoogleToken.SUBJECT, actual = identity.subject)
    }

    private class Environment {

        val verifier = GoogleIdentityVerifier(
            googleAuthConfig = StubGoogleToken.stubGoogleAuthConfig(),
            jwkProvider = StubGoogleToken.stubJwkProvider(),
        )
    }
}
