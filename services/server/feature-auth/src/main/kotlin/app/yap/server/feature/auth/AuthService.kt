package app.yap.server.feature.auth

import app.yap.server.core.security.SessionIdentity
import app.yap.server.core.security.TokenService
import app.yap.server.feature.auth.identity.CodeChallenge
import app.yap.server.feature.auth.identity.IdentityVerifier
import app.yap.server.feature.auth.identity.IdentityVerifiers
import app.yap.server.feature.auth.model.AuthChallenge
import app.yap.server.feature.auth.model.AuthFailure
import app.yap.server.feature.auth.model.AuthFailureException
import app.yap.server.feature.auth.model.IssuedChallenge
import app.yap.server.feature.auth.model.IssuedSession
import app.yap.server.feature.auth.model.LoginCredential
import app.yap.server.feature.auth.model.NewSession
import app.yap.server.feature.auth.model.ProviderId
import app.yap.server.feature.auth.persistence.AuthRepository
import java.time.Clock
import java.time.Duration
import java.time.Instant

/**
 * The authentication scenarios. There is no log-out scenario in this iteration: clearing a session
 * on the device is a client-side reaction to a definitive refresh rejection.
 */
internal class AuthService(
    private val clock: Clock,
    private val identityVerifiers: IdentityVerifiers,
    private val repository: AuthRepository,
    private val tokenService: TokenService,
) {

    /**
     * Issues a fresh, single-use challenge for [provider], storing only the nonce hash and the
     * client-supplied [codeChallenge] verbatim as the challenge proof. The raw nonce leaves the
     * server exactly once, in the returned value.
     */
    suspend fun startChallenge(codeChallenge: String?, provider: String): IssuedChallenge {
        val verifier = verifier(provider)
        if (codeChallenge != null && !verifier.supportsAuthorizationCode) {
            throw AuthFailureException(AuthFailure.InvalidRequest)
        }

        val challenge = tokenService.createChallenge(CHALLENGE_TTL_SECONDS)
        repository.insertChallenge(
            AuthChallenge(
                createdAt = Instant.now(clock),
                expiresAt = challenge.expiresAt,
                id = challenge.id,
                nonceHash = tokenService.hash(challenge.nonce),
                proof = codeChallenge,
                provider = verifier.providerId,
            ),
        )
        return IssuedChallenge(
            challengeId = challenge.id,
            expiresAtEpochSeconds = challenge.expiresAt.epochSecond,
            nonce = challenge.nonce,
        )
    }

    /**
     * Completes a login attempt. The provider result is verified before the transaction opens,
     * because verification may need a network call, and a PKCE proof is compared before any token
     * exchange runs. Since that work takes unbounded time, the challenge is re-checked against the
     * transaction's own clock inside the locked transaction rather than against any time read here.
     * Every challenge problem collapses into [AuthFailure.ChallengeInvalid].
     */
    suspend fun login(
        challengeId: String,
        credential: LoginCredential,
        provider: String,
    ): IssuedSession {
        val verifier = verifier(provider)
        val challenge = readChallenge(challengeId = challengeId, verifier = verifier)
        requireBoundProof(challenge = challenge, credential = credential, verifier = verifier)

        val identity = verifier.verify(credential = credential, nonceHash = challenge.nonceHash)
        val refreshToken = tokenService.createRefreshToken()
        val account = repository.consumeChallengeAndCreateSession(
            challenge = challenge,
            identity = identity,
            session = NewSession(
                absoluteLifetime = SESSION_ABSOLUTE_LIFETIME,
                id = refreshToken.sessionId,
                refreshTokenHash = tokenService.hash(refreshToken.value),
            ),
        ) ?: throw challengeInvalid()

        val tokens = tokenService.issueTokens(
            session = SessionIdentity(userId = account.id, sessionId = refreshToken.sessionId),
            refreshToken = refreshToken,
        )
        return IssuedSession(
            accessToken = tokens.accessToken,
            accessTokenExpiresAtEpochSeconds = tokens.accessTokenExpiresAtEpochSeconds,
            accountId = account.id,
            refreshToken = tokens.refreshToken,
        )
    }

    /**
     * Rejects a challenge that is already unusable before the provider is contacted at all. This is
     * only a pre-check: the authoritative expiry decision is made by the repository once it holds
     * the challenge lock, because verification below may itself outlive the challenge.
     */
    private suspend fun readChallenge(
        challengeId: String,
        verifier: IdentityVerifier,
    ): AuthChallenge {
        val challenge = repository.findChallenge(challengeId)
        val isUsable = challenge != null &&
            challenge.provider == verifier.providerId &&
            challenge.expiresAt.isAfter(Instant.now(clock))
        if (!isUsable) throw challengeInvalid()

        return checkNotNull(challenge)
    }

    /**
     * Rejects an authorization code whose verifier does not hash to the persisted proof, before the
     * verifier is given a chance to exchange it.
     */
    private fun requireBoundProof(
        challenge: AuthChallenge,
        credential: LoginCredential,
        verifier: IdentityVerifier,
    ) {
        if (credential !is LoginCredential.AuthorizationCode) return
        if (!verifier.supportsAuthorizationCode) throw AuthFailureException(AuthFailure.InvalidRequest)

        val proof = challenge.proof
        if (proof == null || CodeChallenge.s256(credential.codeVerifier) != proof) {
            throw challengeInvalid()
        }
    }

    private fun verifier(provider: String): IdentityVerifier =
        identityVerifiers.find(ProviderId(provider))
            ?: throw AuthFailureException(AuthFailure.ProviderUnavailable)

    private fun challengeInvalid(): AuthFailureException =
        AuthFailureException(AuthFailure.ChallengeInvalid)

    private companion object {
        const val CHALLENGE_TTL_SECONDS = 300L
        val SESSION_ABSOLUTE_LIFETIME: Duration = Duration.ofDays(180)
    }
}
