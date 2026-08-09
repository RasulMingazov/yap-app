package app.yap.server.feature.auth

import app.yap.server.core.security.IssuedTokens
import app.yap.server.core.security.RefreshToken
import app.yap.server.feature.auth.model.NewSession
import app.yap.server.feature.auth.model.SessionRotation
import java.time.Duration

internal object StubAuthSession {

    const val ACCESS_TOKEN = "access-token"
    const val ACCESS_TOKEN_EXPIRES_AT_EPOCH_SECONDS = 1_786_924_800L
    const val SESSION_ID = "33333333-3333-3333-3333-333333333333"
    const val REFRESH_TOKEN = "ysr_$SESSION_ID.refresh-secret"

    /** The value a rotation issues, so a test can tell it apart from the presented one. */
    const val ROTATED_REFRESH_TOKEN = "ysr_$SESSION_ID.rotated-secret"

    val ABSOLUTE_LIFETIME: Duration = Duration.ofDays(180)

    /** The refresh TTL the server is configured with: thirty days of inactivity. */
    val INACTIVITY_LIMIT: Duration = Duration.ofDays(30)

    fun stubIssuedTokens(
        accessToken: String = ACCESS_TOKEN,
        accessTokenExpiresAtEpochSeconds: Long = ACCESS_TOKEN_EXPIRES_AT_EPOCH_SECONDS,
        refreshToken: String = REFRESH_TOKEN,
    ): IssuedTokens = IssuedTokens(
        accessToken = accessToken,
        refreshToken = refreshToken,
        accessTokenExpiresAtEpochSeconds = accessTokenExpiresAtEpochSeconds,
    )

    fun stubNewSession(
        absoluteLifetime: Duration = ABSOLUTE_LIFETIME,
        id: String = SESSION_ID,
        refreshTokenHash: String = StubAuth.HASH,
    ): NewSession = NewSession(
        absoluteLifetime = absoluteLifetime,
        id = id,
        refreshTokenHash = refreshTokenHash,
    )

    fun stubRefreshToken(
        sessionId: String = SESSION_ID,
        value: String = REFRESH_TOKEN,
    ): RefreshToken = RefreshToken(
        sessionId = sessionId,
        value = value,
    )

    fun stubSessionRotation(
        inactivityLimit: Duration = INACTIVITY_LIMIT,
        presentedTokenHash: String = StubAuth.HASH,
        rotatedTokenHash: String = StubAuth.HASH,
        sessionId: String = SESSION_ID,
    ): SessionRotation = SessionRotation(
        inactivityLimit = inactivityLimit,
        presentedTokenHash = presentedTokenHash,
        rotatedTokenHash = rotatedTokenHash,
        sessionId = sessionId,
    )
}
