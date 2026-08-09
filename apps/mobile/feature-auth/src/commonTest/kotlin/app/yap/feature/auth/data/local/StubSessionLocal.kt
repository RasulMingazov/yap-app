package app.yap.feature.auth.data.local

internal object StubSessionLocal {

    const val ACCESS_TOKEN = "access-token"
    const val ACCESS_TOKEN_EXPIRES_AT_EPOCH_SECONDS = 1_800L
    const val ACCOUNT_ID = "account-id"
    const val REFRESH_TOKEN = "refresh-token"

    fun stubSessionLocal(
        accessToken: String = ACCESS_TOKEN,
        accessTokenExpiresAtEpochSeconds: Long = ACCESS_TOKEN_EXPIRES_AT_EPOCH_SECONDS,
        accountId: String = ACCOUNT_ID,
        refreshToken: String = REFRESH_TOKEN,
    ): SessionLocal = SessionLocal(
        accessToken = accessToken,
        accessTokenExpiresAtEpochSeconds = accessTokenExpiresAtEpochSeconds,
        accountId = accountId,
        refreshToken = refreshToken,
    )
}
