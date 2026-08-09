package app.yap.feature.auth.data.remote

import app.yap.contract.auth.LoginChallengeDto
import app.yap.contract.auth.SessionDto

internal object StubAuthDto {

    const val ACCESS_TOKEN = "remote-access-token"
    const val ACCESS_TOKEN_EXPIRES_AT_EPOCH_SECONDS = 3_600L
    const val ACCOUNT_ID = "remote-account-id"
    const val CHALLENGE_ID = "challenge-id"
    const val CHALLENGE_EXPIRES_AT_EPOCH_SECONDS = 300L
    const val NONCE = "nonce"
    const val REFRESH_TOKEN = "remote-refresh-token"

    fun stubLoginChallengeDto(
        challengeId: String = CHALLENGE_ID,
        expiresAtEpochSeconds: Long = CHALLENGE_EXPIRES_AT_EPOCH_SECONDS,
        nonce: String? = NONCE,
    ): LoginChallengeDto = LoginChallengeDto(
        challengeId = challengeId,
        nonce = nonce,
        expiresAtEpochSeconds = expiresAtEpochSeconds,
    )

    fun stubSessionDto(
        accessToken: String = ACCESS_TOKEN,
        accessTokenExpiresAtEpochSeconds: Long = ACCESS_TOKEN_EXPIRES_AT_EPOCH_SECONDS,
        accountId: String = ACCOUNT_ID,
        refreshToken: String = REFRESH_TOKEN,
    ): SessionDto = SessionDto(
        accessToken = accessToken,
        refreshToken = refreshToken,
        accessTokenExpiresAtEpochSeconds = accessTokenExpiresAtEpochSeconds,
        accountId = accountId,
    )
}
