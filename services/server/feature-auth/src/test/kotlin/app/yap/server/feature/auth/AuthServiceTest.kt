package app.yap.server.feature.auth

import app.yap.server.core.security.JwtTokenService
import app.yap.server.feature.auth.identity.GoogleIdentityVerifier
import app.yap.server.feature.auth.identity.StubGoogleToken
import app.yap.server.feature.auth.model.AuthFailure
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

internal class AuthServiceTest {

    @Test
    fun `GIVEN a verified confirmation WHEN logging in THEN an account is resolved and a session issued`() {
        val env = Environment()

        val session = env.authService.loginWithGoogleIdToken(
            idToken = StubGoogleToken.stubIdToken(),
            nonce = StubGoogleToken.NONCE,
        )

        assertEquals(expected = USER_ID, actual = session.userId)
        assertEquals(expected = 1, actual = env.persistence.resolvedIdentities.size)
        assertEquals(expected = 1, actual = env.persistence.createdSessions.size)
    }

    @Test
    fun `GIVEN a verified confirmation WHEN logging in THEN the session expiry is the configured window`() {
        val env = Environment()

        val session = env.authService.loginWithGoogleIdToken(
            idToken = StubGoogleToken.stubIdToken(),
            nonce = StubGoogleToken.NONCE,
        )

        assertEquals(
            expected = NOW.epochSecond + REFRESH_TOKEN_TTL_SECONDS,
            actual = session.refreshTokenExpiresAtEpochSeconds,
        )
    }

    @Test
    fun `GIVEN a verified confirmation WHEN logging in THEN only the refresh token hash is persisted`() {
        val env = Environment()

        val session = env.authService.loginWithGoogleIdToken(
            idToken = StubGoogleToken.stubIdToken(),
            nonce = StubGoogleToken.NONCE,
        )

        val persisted = env.persistence.createdSessions.single()
        assertEquals(
            expected = env.tokenService.hash(session.refreshToken),
            actual = persisted.refreshTokenHash,
        )
    }

    @Test
    fun `GIVEN an unverifiable confirmation WHEN logging in THEN nothing is created and it is refused`() {
        val env = Environment()

        assertFailsWith<AuthFailure.UnverifiableConfirmation> {
            env.authService.loginWithGoogleIdToken(
                idToken = StubGoogleToken.stubIdToken(
                    keyId = StubGoogleToken.OTHER_KEY_ID,
                    signingKeyPair = StubGoogleToken.otherKeyPair,
                ),
                nonce = StubGoogleToken.NONCE,
            )
        }

        assertEquals(expected = emptyList(), actual = env.persistence.resolvedIdentities)
        assertEquals(expected = emptyList(), actual = env.persistence.createdSessions)
    }

    private class Environment {

        val persistence = StubAuthPersistence(userId = USER_ID)
        val tokenService = JwtTokenService(
            jwtSecret = JWT_SECRET,
            jwtIssuer = "yap-backend",
            jwtAudience = "yap-mobile",
            accessTokenTtlSeconds = 900,
            clock = Clock.fixed(NOW, ZoneOffset.UTC),
        )
        val authService = AuthService(
            authPersistence = persistence,
            clock = Clock.fixed(NOW, ZoneOffset.UTC),
            googleCodeExchanger = StubGoogleToken.stubUnusedCodeExchanger(),
            googleIdentityVerifier = GoogleIdentityVerifier(
                googleAuthConfig = StubGoogleToken.stubGoogleAuthConfig(),
                jwkProvider = StubGoogleToken.stubJwkProvider(),
            ),
            refreshTokenTtlSeconds = REFRESH_TOKEN_TTL_SECONDS,
            tokenService = tokenService,
        )
    }

    private companion object {
        const val USER_ID = "user-1"
        const val REFRESH_TOKEN_TTL_SECONDS = 7_776_000L
        const val JWT_SECRET = "a-test-secret-that-is-at-least-forty-three-characters"
        val NOW: Instant = Instant.parse("2026-08-13T10:00:00Z")
    }
}
