package app.yap.server.feature.auth

import app.yap.server.core.security.InvalidTokenException
import app.yap.server.core.security.RefreshToken
import app.yap.server.core.security.SessionIdentity
import app.yap.server.core.security.TokenService
import app.yap.server.feature.auth.identity.GoogleCodeExchanger
import app.yap.server.feature.auth.identity.GoogleIdentity
import app.yap.server.feature.auth.identity.GoogleIdentityVerifier
import app.yap.server.feature.auth.model.AuthFailure
import app.yap.server.feature.auth.model.AuthenticatedSession
import app.yap.server.feature.auth.persistence.AuthPersistence
import app.yap.server.feature.auth.persistence.PersistedSession
import app.yap.server.feature.auth.persistence.SessionRotation
import java.time.Clock
import java.time.Instant

internal class AuthService(
    private val authPersistence: AuthPersistence,
    private val clock: Clock,
    private val googleCodeExchanger: GoogleCodeExchanger,
    private val googleIdentityVerifier: GoogleIdentityVerifier,
    private val refreshTokenTtlSeconds: Long,
    private val tokenService: TokenService,
) {

    fun loginWithGoogleIdToken(idToken: String, nonce: String): AuthenticatedSession {
        val identity = googleIdentityVerifier.verify(idToken = idToken, expectedNonce = nonce)
        return establishSession(identity)
    }

    suspend fun loginWithGoogleAuthorizationCode(
        code: String,
        codeVerifier: String,
        redirectUri: String,
    ): AuthenticatedSession {
        val identity = googleCodeExchanger.exchange(
            code = code,
            codeVerifier = codeVerifier,
            redirectUri = redirectUri,
        )
        return establishSession(identity)
    }

    fun rotate(refreshToken: String): AuthenticatedSession {
        val presented = try {
            tokenService.parseRefreshToken(refreshToken)
        } catch (_: InvalidTokenException) {
            throw AuthFailure.MalformedInput()
        }

        val rotated = tokenService.rotateRefreshToken(presented)
        val expiresAt = refreshTokenExpiry()
        val userId = authPersistence.rotateSession(
            SessionRotation(
                sessionId = presented.sessionId,
                presentedRefreshTokenHash = tokenService.hash(presented.value),
                refreshTokenHash = tokenService.hash(rotated.value),
                expiresAt = expiresAt,
            ),
        ) ?: throw AuthFailure.UnverifiableConfirmation()

        return issueSession(refreshToken = rotated, refreshTokenExpiresAt = expiresAt, userId = userId)
    }

    private fun establishSession(identity: GoogleIdentity): AuthenticatedSession {
        val userId = authPersistence.resolveOrCreateUserId(identity)
        val refreshToken = tokenService.createRefreshToken()
        val expiresAt = refreshTokenExpiry()

        authPersistence.createSession(
            PersistedSession(
                sessionId = refreshToken.sessionId,
                userId = userId,
                refreshTokenHash = tokenService.hash(refreshToken.value),
                expiresAt = expiresAt,
            ),
        )

        return issueSession(refreshToken = refreshToken, refreshTokenExpiresAt = expiresAt, userId = userId)
    }

    private fun refreshTokenExpiry(): Instant = Instant.now(clock).plusSeconds(refreshTokenTtlSeconds)

    private fun issueSession(
        refreshToken: RefreshToken,
        refreshTokenExpiresAt: Instant,
        userId: String,
    ): AuthenticatedSession {
        val issued = tokenService.issueTokens(
            session = SessionIdentity(userId = userId, sessionId = refreshToken.sessionId),
            refreshToken = refreshToken,
        )
        return AuthenticatedSession(
            userId = userId,
            accessToken = issued.accessToken,
            refreshToken = issued.refreshToken,
            accessTokenExpiresAtEpochSeconds = issued.accessTokenExpiresAtEpochSeconds,
            refreshTokenExpiresAtEpochSeconds = refreshTokenExpiresAt.epochSecond,
        )
    }
}
