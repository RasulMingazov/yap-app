package app.yap.server.feature.auth

import app.yap.server.core.security.SessionIdentity
import app.yap.server.feature.auth.identity.IdentityVerifiers
import app.yap.server.feature.auth.model.AuthAccount
import app.yap.server.feature.auth.model.AuthChallenge
import app.yap.server.feature.auth.model.AuthFailure
import app.yap.server.feature.auth.model.AuthFailureException
import app.yap.server.feature.auth.model.IssuedChallenge
import app.yap.server.feature.auth.model.IssuedSession
import app.yap.server.feature.auth.model.ProviderId
import java.time.Clock
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.coroutines.test.runTest

class AuthServiceTest {

    @Test
    fun `GIVEN a registered provider WHEN starting a challenge THEN only the nonce hash is stored`() = runTest {
        val env = Environment()

        env.service.startChallenge(codeChallenge = null, provider = StubAuth.PROVIDER)

        env.repository.insertChallengeCall.calledWith(
            StubAuthChallenge.stubAuthChallenge(
                nonceHash = StubAuth.HASH,
                proof = null,
            ),
        )
    }

    @Test
    fun `GIVEN a PKCE attempt WHEN starting a challenge THEN the supplied code challenge is stored as the proof`() =
        runTest {
            val env = Environment()

            env.service.startChallenge(
                codeChallenge = StubAuthChallenge.CODE_CHALLENGE,
                provider = StubAuth.PROVIDER,
            )

            env.repository.insertChallengeCall.calledWith(
                StubAuthChallenge.stubAuthChallenge(proof = StubAuthChallenge.CODE_CHALLENGE),
            )
        }

    @Test
    fun `GIVEN a registered provider WHEN starting a challenge THEN it expires in five minutes`() = runTest {
        val env = Environment()

        env.service.startChallenge(codeChallenge = null, provider = StubAuth.PROVIDER)

        env.tokenService.createChallengeCall.calledWith(StubAuthChallenge.TTL_SECONDS)
    }

    @Test
    fun `GIVEN a registered provider WHEN starting a challenge THEN the raw nonce is returned once`() = runTest {
        val env = Environment()

        val result = env.service.startChallenge(codeChallenge = null, provider = StubAuth.PROVIDER)

        assertEquals(
            expected = IssuedChallenge(
                challengeId = StubAuthChallenge.CHALLENGE_ID,
                expiresAtEpochSeconds = StubAuthChallenge.EXPIRES_AT.epochSecond,
                nonce = StubAuthChallenge.NONCE,
            ),
            actual = result,
        )
    }

    @Test
    fun `GIVEN an unconfigured provider WHEN starting a challenge THEN it fails as provider unavailable`() = runTest {
        val env = Environment(providerId = OTHER_PROVIDER)

        val failure = assertFailsWith<AuthFailureException> {
            env.service.startChallenge(codeChallenge = null, provider = StubAuth.PROVIDER)
        }

        assertEquals(expected = AuthFailure.ProviderUnavailable, actual = failure.failure)
    }

    @Test
    fun `GIVEN an unconfigured provider WHEN starting a challenge THEN no challenge is stored`() = runTest {
        val env = Environment(providerId = OTHER_PROVIDER)

        assertFailsWith<AuthFailureException> {
            env.service.startChallenge(codeChallenge = null, provider = StubAuth.PROVIDER)
        }

        env.repository.insertChallengeCall.notCalled()
    }

    @Test
    fun `GIVEN a provider without PKCE WHEN starting a challenge with a code challenge THEN it is an invalid request`() =
        runTest {
            val env = Environment(supportsAuthorizationCode = false)

            val failure = assertFailsWith<AuthFailureException> {
                env.service.startChallenge(
                    codeChallenge = StubAuthChallenge.CODE_CHALLENGE,
                    provider = StubAuth.PROVIDER,
                )
            }

            assertEquals(expected = AuthFailure.InvalidRequest, actual = failure.failure)
        }

    @Test
    fun `GIVEN a verified identity token WHEN logging in THEN it returns the issued session`() = runTest {
        val env = Environment()

        val result = env.service.login(
            challengeId = StubAuthChallenge.CHALLENGE_ID,
            credential = StubLoginCredential.stubIdentityToken(),
            provider = StubAuth.PROVIDER,
        )

        assertEquals(
            expected = IssuedSession(
                accessToken = StubAuthSession.ACCESS_TOKEN,
                accessTokenExpiresAtEpochSeconds = StubAuthSession.ACCESS_TOKEN_EXPIRES_AT_EPOCH_SECONDS,
                accountId = StubAuthAccount.ACCOUNT_ID,
                refreshToken = StubAuthSession.REFRESH_TOKEN,
            ),
            actual = result,
        )
    }

    @Test
    fun `GIVEN a verified identity token WHEN logging in THEN the session is stored as a hash`() = runTest {
        val env = Environment()

        env.service.login(
            challengeId = StubAuthChallenge.CHALLENGE_ID,
            credential = StubLoginCredential.stubIdentityToken(),
            provider = StubAuth.PROVIDER,
        )

        env.repository.consumeChallengeAndCreateSessionCall.calledWith(
            StubAuthChallenge.stubAuthChallenge(),
            StubVerifiedIdentity.stubVerifiedIdentity(),
            StubAuthSession.stubNewSession(),
        )
    }

    @Test
    fun `GIVEN a verified identity token WHEN logging in THEN the session is bound to the resolved account`() =
        runTest {
            val env = Environment()

            env.service.login(
                challengeId = StubAuthChallenge.CHALLENGE_ID,
                credential = StubLoginCredential.stubIdentityToken(),
                provider = StubAuth.PROVIDER,
            )

            env.tokenService.issueTokensCall.calledWith(
                SessionIdentity(userId = StubAuthAccount.ACCOUNT_ID, sessionId = StubAuthSession.SESSION_ID),
                StubAuthSession.stubRefreshToken(),
            )
        }

    @Test
    fun `GIVEN one challenge WHEN logging in THEN it is consumed exactly once`() = runTest {
        val env = Environment()

        env.service.login(
            challengeId = StubAuthChallenge.CHALLENGE_ID,
            credential = StubLoginCredential.stubIdentityToken(),
            provider = StubAuth.PROVIDER,
        )

        env.repository.consumeChallengeAndCreateSessionCall.called(times = 1)
    }

    @Test
    fun `GIVEN provider verification fails WHEN logging in THEN the login transaction never opens`() = runTest {
        val env = Environment()
        env.verifier.verifyCall.throws(AuthFailureException(AuthFailure.ChallengeInvalid))

        assertFailsWith<AuthFailureException> {
            env.service.login(
                challengeId = StubAuthChallenge.CHALLENGE_ID,
                credential = StubLoginCredential.stubIdentityToken(),
                provider = StubAuth.PROVIDER,
            )
        }

        env.repository.consumeChallengeAndCreateSessionCall.notCalled()
    }

    @Test
    fun `GIVEN a missing challenge WHEN logging in THEN it fails as challenge invalid`() = runTest {
        val env = Environment(challenge = null)

        val failure = assertFailsWith<AuthFailureException> {
            env.service.login(
                challengeId = StubAuthChallenge.CHALLENGE_ID,
                credential = StubLoginCredential.stubIdentityToken(),
                provider = StubAuth.PROVIDER,
            )
        }

        assertEquals(expected = AuthFailure.ChallengeInvalid, actual = failure.failure)
    }

    @Test
    fun `GIVEN an expired challenge WHEN logging in THEN it fails as challenge invalid`() = runTest {
        val env = Environment(challenge = StubAuthChallenge.stubAuthChallenge(expiresAt = StubAuth.NOW))

        val failure = assertFailsWith<AuthFailureException> {
            env.service.login(
                challengeId = StubAuthChallenge.CHALLENGE_ID,
                credential = StubLoginCredential.stubIdentityToken(),
                provider = StubAuth.PROVIDER,
            )
        }

        assertEquals(expected = AuthFailure.ChallengeInvalid, actual = failure.failure)
    }

    @Test
    fun `GIVEN a challenge issued for another provider WHEN logging in THEN it fails as challenge invalid`() =
        runTest {
            val env = Environment(challenge = StubAuthChallenge.stubAuthChallenge(provider = OTHER_PROVIDER))

            val failure = assertFailsWith<AuthFailureException> {
                env.service.login(
                    challengeId = StubAuthChallenge.CHALLENGE_ID,
                    credential = StubLoginCredential.stubIdentityToken(),
                    provider = StubAuth.PROVIDER,
                )
            }

            assertEquals(expected = AuthFailure.ChallengeInvalid, actual = failure.failure)
        }

    @Test
    fun `GIVEN a challenge consumed by a concurrent attempt WHEN logging in THEN it fails as challenge invalid`() =
        runTest {
            val env = Environment(account = null)

            val failure = assertFailsWith<AuthFailureException> {
                env.service.login(
                    challengeId = StubAuthChallenge.CHALLENGE_ID,
                    credential = StubLoginCredential.stubIdentityToken(),
                    provider = StubAuth.PROVIDER,
                )
            }

            assertEquals(expected = AuthFailure.ChallengeInvalid, actual = failure.failure)
        }

    @Test
    fun `GIVEN the login transaction rejects the challenge WHEN logging in THEN no tokens are issued`() = runTest {
        val env = Environment(account = null)

        assertFailsWith<AuthFailureException> {
            env.service.login(
                challengeId = StubAuthChallenge.CHALLENGE_ID,
                credential = StubLoginCredential.stubIdentityToken(),
                provider = StubAuth.PROVIDER,
            )
        }

        env.tokenService.issueTokensCall.notCalled()
    }

    @Test
    fun `GIVEN a mismatched code verifier WHEN logging in THEN it fails as challenge invalid`() = runTest {
        val env = Environment(challenge = StubAuthChallenge.stubAuthChallenge(proof = StubAuthChallenge.CODE_CHALLENGE))

        val failure = assertFailsWith<AuthFailureException> {
            env.service.login(
                challengeId = StubAuthChallenge.CHALLENGE_ID,
                credential = StubLoginCredential.stubAuthorizationCode(codeVerifier = "another-code-verifier"),
                provider = StubAuth.PROVIDER,
            )
        }

        assertEquals(expected = AuthFailure.ChallengeInvalid, actual = failure.failure)
    }

    @Test
    fun `GIVEN a mismatched code verifier WHEN logging in THEN no token exchange is attempted`() = runTest {
        val env = Environment(challenge = StubAuthChallenge.stubAuthChallenge(proof = StubAuthChallenge.CODE_CHALLENGE))

        assertFailsWith<AuthFailureException> {
            env.service.login(
                challengeId = StubAuthChallenge.CHALLENGE_ID,
                credential = StubLoginCredential.stubAuthorizationCode(codeVerifier = "another-code-verifier"),
                provider = StubAuth.PROVIDER,
            )
        }

        env.verifier.verifyCall.notCalled()
    }

    @Test
    fun `GIVEN a challenge without a stored proof WHEN logging in with a code THEN it fails as challenge invalid`() =
        runTest {
            val env = Environment()

            val failure = assertFailsWith<AuthFailureException> {
                env.service.login(
                    challengeId = StubAuthChallenge.CHALLENGE_ID,
                    credential = StubLoginCredential.stubAuthorizationCode(),
                    provider = StubAuth.PROVIDER,
                )
            }

            assertEquals(expected = AuthFailure.ChallengeInvalid, actual = failure.failure)
        }

    @Test
    fun `GIVEN a matching code verifier WHEN logging in THEN the credential reaches the verifier`() = runTest {
        val env = Environment(challenge = StubAuthChallenge.stubAuthChallenge(proof = StubAuthChallenge.CODE_CHALLENGE))
        val credential = StubLoginCredential.stubAuthorizationCode()

        env.service.login(
            challengeId = StubAuthChallenge.CHALLENGE_ID,
            credential = credential,
            provider = StubAuth.PROVIDER,
        )

        env.verifier.verifyCall.calledWith(credential, StubAuth.HASH)
    }

    @Test
    fun `GIVEN an unconfigured provider WHEN logging in THEN it fails as provider unavailable`() = runTest {
        val env = Environment(providerId = OTHER_PROVIDER)

        val failure = assertFailsWith<AuthFailureException> {
            env.service.login(
                challengeId = StubAuthChallenge.CHALLENGE_ID,
                credential = StubLoginCredential.stubIdentityToken(),
                provider = StubAuth.PROVIDER,
            )
        }

        assertEquals(expected = AuthFailure.ProviderUnavailable, actual = failure.failure)
    }

    @Test
    fun `GIVEN a provider without PKCE WHEN logging in with an authorization code THEN it is an invalid request`() =
        runTest {
            val env = Environment(
                challenge = StubAuthChallenge.stubAuthChallenge(proof = StubAuthChallenge.CODE_CHALLENGE),
                supportsAuthorizationCode = false,
            )

            val failure = assertFailsWith<AuthFailureException> {
                env.service.login(
                    challengeId = StubAuthChallenge.CHALLENGE_ID,
                    credential = StubLoginCredential.stubAuthorizationCode(),
                    provider = StubAuth.PROVIDER,
                )
            }

            assertEquals(expected = AuthFailure.InvalidRequest, actual = failure.failure)
        }

    private companion object {
        val OTHER_PROVIDER = ProviderId("apple")
    }

    private class Environment(
        account: AuthAccount? = StubAuthAccount.stubAuthAccount(),
        challenge: AuthChallenge? = StubAuthChallenge.stubAuthChallenge(),
        providerId: ProviderId = ProviderId.Google,
        supportsAuthorizationCode: Boolean = true,
    ) {

        val repository = StubAuthRepository(account = account, challenge = challenge)
        val tokenService = StubTokenService()
        val verifier = StubIdentityVerifier(
            providerId = providerId,
            supportsAuthorizationCode = supportsAuthorizationCode,
        )
        val service = AuthService(
            clock = Clock.fixed(StubAuth.NOW, ZoneOffset.UTC),
            identityVerifiers = IdentityVerifiers(verifiers = listOf(verifier)),
            repository = repository,
            tokenService = tokenService,
        )
    }
}
