package app.yap.server.feature.auth.identity

import app.yap.server.feature.auth.StubAuth
import app.yap.server.feature.auth.StubAuthChallenge
import app.yap.server.feature.auth.StubLoginCredential
import app.yap.server.feature.auth.StubVerifiedIdentity
import app.yap.server.feature.auth.model.AuthFailure
import app.yap.server.feature.auth.model.AuthFailureException
import app.yap.server.feature.auth.model.LoginCredential
import app.yap.server.feature.auth.model.ProviderId
import app.yap.server.feature.auth.model.VerifiedIdentity
import java.security.KeyPair
import java.security.interfaces.RSAPublicKey
import java.time.Clock
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.coroutines.test.runTest

class GoogleIdentityVerifierTest {

    @Test
    fun `GIVEN a correctly signed identity token WHEN verifying THEN it returns the proven identity`() = runTest {
        val env = Environment()

        val result = env.verifier.verify(
            credential = LoginCredential.IdentityToken(idToken = StubGoogleIdToken.stubIdToken()),
            nonceHash = StubGoogleIdToken.NONCE_HASH,
        )

        assertEquals(
            expected = VerifiedIdentity(
                email = StubVerifiedIdentity.EMAIL,
                isEmailVerified = true,
                provider = ProviderId.Google,
                subject = StubVerifiedIdentity.SUBJECT,
            ),
            actual = result,
        )
    }

    @Test
    fun `GIVEN a token signed by another key WHEN verifying THEN it fails as challenge invalid`() = runTest {
        val env = Environment()
        val idToken = StubGoogleIdToken.stubIdToken(keyPair = StubGoogleIdToken.otherKeyPair)

        assertChallengeInvalid { env.verifier.verifyIdentityToken(idToken) }
    }

    @Test
    fun `GIVEN a token from another issuer WHEN verifying THEN it fails as challenge invalid`() = runTest {
        val env = Environment()
        val idToken = StubGoogleIdToken.stubIdToken(issuer = "https://accounts.example.com")

        assertChallengeInvalid { env.verifier.verifyIdentityToken(idToken) }
    }

    @Test
    fun `GIVEN a token for another audience WHEN verifying THEN it fails as challenge invalid`() = runTest {
        val env = Environment()
        val idToken = StubGoogleIdToken.stubIdToken(audience = "another-server-client-id")

        assertChallengeInvalid { env.verifier.verifyIdentityToken(idToken) }
    }

    @Test
    fun `GIVEN a token whose authorized party is unknown WHEN verifying THEN it fails as challenge invalid`() =
        runTest {
            val env = Environment()
            val idToken = StubGoogleIdToken.stubIdToken(authorizedParty = "another-client-id")

            assertChallengeInvalid { env.verifier.verifyIdentityToken(idToken) }
        }

    @Test
    fun `GIVEN a token without an authorized party WHEN verifying THEN it returns the proven identity`() = runTest {
        val env = Environment()
        val idToken = StubGoogleIdToken.stubIdToken(authorizedParty = null)

        val result = env.verifier.verifyIdentityToken(idToken)

        assertEquals(expected = StubVerifiedIdentity.SUBJECT, actual = result.subject)
    }

    @Test
    fun `GIVEN an expired token WHEN verifying THEN it fails as challenge invalid`() = runTest {
        val env = Environment()
        val idToken = StubGoogleIdToken.stubIdToken(expiresAt = StubAuth.NOW)

        assertChallengeInvalid { env.verifier.verifyIdentityToken(idToken) }
    }

    @Test
    fun `GIVEN a token without a subject WHEN verifying THEN it fails as challenge invalid`() = runTest {
        val env = Environment()
        val idToken = StubGoogleIdToken.stubIdToken(subject = null)

        assertChallengeInvalid { env.verifier.verifyIdentityToken(idToken) }
    }

    @Test
    fun `GIVEN a token bound to another nonce WHEN verifying THEN it fails as challenge invalid`() = runTest {
        val env = Environment()
        val idToken = StubGoogleIdToken.stubIdToken(nonce = "another-nonce")

        assertChallengeInvalid { env.verifier.verifyIdentityToken(idToken) }
    }

    @Test
    fun `GIVEN a token without a nonce WHEN verifying THEN it fails as challenge invalid`() = runTest {
        val env = Environment()
        val idToken = StubGoogleIdToken.stubIdToken(nonce = null)

        assertChallengeInvalid { env.verifier.verifyIdentityToken(idToken) }
    }

    @Test
    fun `GIVEN a token verified against no stored nonce WHEN verifying THEN it fails as challenge invalid`() =
        runTest {
            val env = Environment()

            assertChallengeInvalid {
                env.verifier.verify(
                    credential = LoginCredential.IdentityToken(idToken = StubGoogleIdToken.stubIdToken()),
                    nonceHash = null,
                )
            }
        }

    @Test
    fun `GIVEN an authorization code WHEN verifying THEN the exchanged identity token proves the identity`() =
        runTest {
            val env = Environment()

            val result = env.verifier.verify(
                credential = StubLoginCredential.stubAuthorizationCode(),
                nonceHash = StubGoogleIdToken.NONCE_HASH,
            )

            assertEquals(expected = StubVerifiedIdentity.SUBJECT, actual = result.subject)
        }

    @Test
    fun `GIVEN an authorization code WHEN verifying THEN the code is exchanged with its verifier`() = runTest {
        val env = Environment()

        env.verifier.verify(
            credential = StubLoginCredential.stubAuthorizationCode(),
            nonceHash = StubGoogleIdToken.NONCE_HASH,
        )

        env.tokenExchange.exchangeCall.calledWith(
            StubLoginCredential.AUTHORIZATION_CODE,
            StubAuthChallenge.CODE_VERIFIER,
            StubGoogleIdToken.FALLBACK_REDIRECT_URI,
        )
    }

    @Test
    fun `GIVEN an unregistered redirect uri WHEN verifying an authorization code THEN nothing is exchanged`() =
        runTest {
            val env = Environment()

            assertChallengeInvalid {
                env.verifier.verify(
                    credential = StubLoginCredential.stubAuthorizationCode(redirectUri = "https://attacker.example.com/redirect"),
                    nonceHash = StubGoogleIdToken.NONCE_HASH,
                )
            }

            env.tokenExchange.exchangeCall.notCalled()
        }

    @Test
    fun `GIVEN no configured redirect uri WHEN verifying an authorization code THEN it is unavailable`() = runTest {
        val env = Environment(config = StubGoogleIdToken.stubGoogleProviderConfig(fallbackRedirectUri = null))

        val failure = assertFailsWith<AuthFailureException> {
            env.verifier.verify(
                credential = StubLoginCredential.stubAuthorizationCode(),
                nonceHash = StubGoogleIdToken.NONCE_HASH,
            )
        }

        assertEquals(expected = AuthFailure.ProviderUnavailable, actual = failure.failure)
    }

    private suspend fun GoogleIdentityVerifier.verifyIdentityToken(idToken: String): VerifiedIdentity =
        verify(
            credential = LoginCredential.IdentityToken(idToken = idToken),
            nonceHash = StubGoogleIdToken.NONCE_HASH,
        )

    private suspend fun assertChallengeInvalid(block: suspend () -> Unit) {
        val failure = assertFailsWith<AuthFailureException> { block() }

        assertEquals(expected = AuthFailure.ChallengeInvalid, actual = failure.failure)
    }

    private class Environment(
        config: GoogleProviderConfig = StubGoogleIdToken.stubGoogleProviderConfig(),
        signingKeyPair: KeyPair = StubGoogleIdToken.googleKeyPair,
    ) {

        val tokenExchange = StubGoogleTokenExchange()
        val verifier = GoogleIdentityVerifier(
            clock = Clock.fixed(StubAuth.NOW, ZoneOffset.UTC),
            config = config,
            nonceHasher = StubGoogleIdToken.stubNonceHasher(),
            signingKeys = { signingKeyPair.public as RSAPublicKey },
            tokenExchange = tokenExchange,
        )
    }
}
