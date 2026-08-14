package app.yap.feature.auth.data.mapper

import app.yap.feature.auth.api.entity.AuthState
import app.yap.feature.auth.api.entity.UserId
import app.yap.feature.auth.data.local.SessionLocal
import app.yap.feature.auth.data.local.StubSession
import app.yap.feature.auth.data.remote.StubSessionDto
import kotlin.test.Test
import kotlin.test.assertEquals

internal class SessionMapperTest {

    @Test
    fun `GIVEN a session from the server WHEN it is stored THEN both instants are copied verbatim`() {
        val dto = StubSessionDto.stubSessionDto()

        val stored = dto.toLocal()

        assertEquals(
            expected = SessionLocal(
                accessToken = StubSession.ACCESS_TOKEN,
                refreshToken = StubSession.REFRESH_TOKEN,
                accessTokenExpiresAtEpochSeconds = StubSession.ACCESS_TOKEN_EXPIRES_AT_EPOCH_SECONDS,
                refreshTokenExpiresAtEpochSeconds = StubSession.REFRESH_TOKEN_EXPIRES_AT_EPOCH_SECONDS,
            ),
            actual = stored,
        )
    }

    @Test
    fun `GIVEN instants the device would find implausible WHEN they are stored THEN they are still copied`() {
        val dto = StubSessionDto.stubSessionDto(
            accessTokenExpiresAtEpochSeconds = 0L,
            refreshTokenExpiresAtEpochSeconds = -1L,
        )

        val stored = dto.toLocal()

        assertEquals(expected = 0L, actual = stored.accessTokenExpiresAtEpochSeconds)
        assertEquals(expected = -1L, actual = stored.refreshTokenExpiresAtEpochSeconds)
    }

    @Test
    fun `GIVEN one session WHEN it is stored twice THEN nothing about it moves`() {
        val dto = StubSessionDto.stubSessionDto()

        assertEquals(expected = dto.toLocal(), actual = dto.toLocal())
    }

    @Test
    fun `GIVEN a stored session WHEN it becomes domain state THEN it names the account the token belongs to`() {
        val stored = StubSession.stubSessionLocal()

        assertEquals(
            expected = AuthState.LoggedIn(userId = UserId(StubSession.USER_ID)),
            actual = stored.toDomain(),
        )
    }

    @Test
    fun `GIVEN a token naming no account WHEN it becomes domain state THEN it counts as no session`() {
        val stored = StubSession.stubSessionLocal(accessToken = StubSession.ACCESS_TOKEN_WITHOUT_SUBJECT)

        assertEquals(expected = AuthState.LoggedOut, actual = stored.toDomain())
    }
}
