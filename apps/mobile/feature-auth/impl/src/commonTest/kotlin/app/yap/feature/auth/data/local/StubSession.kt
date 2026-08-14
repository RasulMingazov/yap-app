package app.yap.feature.auth.data.local

internal object StubSession {

    const val USER_ID = "user-1"

    const val ACCESS_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ1c2VyLTEifQ.signature"

    const val ACCESS_TOKEN_WITHOUT_SUBJECT =
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJ5YXAtYmFja2VuZCJ9.signature"

    const val ROTATED_ACCESS_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ1c2VyLTEifQ.rotated"

    const val ROTATED_REFRESH_TOKEN = "ysr_11111111-1111-1111-1111-111111111111.rotated"

    const val REFRESH_TOKEN = "ysr_11111111-1111-1111-1111-111111111111.secret"
    const val ACCESS_TOKEN_EXPIRES_AT_EPOCH_SECONDS = 1_800_000_900L
    const val REFRESH_TOKEN_EXPIRES_AT_EPOCH_SECONDS = 1_807_776_000L

    const val NOW_EPOCH_SECONDS = 1_800_000_000L

    fun stubSessionLocal(
        accessToken: String = ACCESS_TOKEN,
        refreshToken: String = REFRESH_TOKEN,
        accessTokenExpiresAtEpochSeconds: Long = ACCESS_TOKEN_EXPIRES_AT_EPOCH_SECONDS,
        refreshTokenExpiresAtEpochSeconds: Long = REFRESH_TOKEN_EXPIRES_AT_EPOCH_SECONDS,
    ): SessionLocal = SessionLocal(
        accessToken = accessToken,
        refreshToken = refreshToken,
        accessTokenExpiresAtEpochSeconds = accessTokenExpiresAtEpochSeconds,
        refreshTokenExpiresAtEpochSeconds = refreshTokenExpiresAtEpochSeconds,
    )
}
