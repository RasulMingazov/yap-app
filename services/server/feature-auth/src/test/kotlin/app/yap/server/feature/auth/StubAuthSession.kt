package app.yap.server.feature.auth

import app.yap.server.core.security.IssuedTokens
import app.yap.server.core.security.RefreshToken
import app.yap.server.feature.auth.model.NewSession
import java.time.Duration

internal object StubAuthSession {

    const val ACCESS_TOKEN = "access-token"
    const val ACCESS_TOKEN_EXPIRES_AT_EPOCH_SECONDS = 1_786_924_800L
    const val SESSION_ID = "33333333-3333-3333-3333-333333333333"
    const val REFRESH_TOKEN = "ysr_$SESSION_ID.refresh-secret"

    val ABSOLUTE_LIFETIME: Duration = Duration.ofDays(180)

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
}
