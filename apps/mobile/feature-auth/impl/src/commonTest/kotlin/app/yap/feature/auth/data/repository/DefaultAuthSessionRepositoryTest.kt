package app.yap.feature.auth.data.repository

import app.yap.feature.auth.api.entity.AuthSessionState
import app.yap.feature.auth.api.entity.UserId
import app.yap.feature.auth.data.CurrentTime
import app.yap.feature.auth.data.SessionStore
import app.yap.feature.auth.data.StubAccessTokenProvider
import app.yap.feature.auth.data.local.SessionLocal
import app.yap.feature.auth.data.local.StubSession
import app.yap.feature.auth.data.local.StubSessionStorage
import app.yap.feature.auth.domain.repository.AuthSessionRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest

internal class DefaultAuthSessionRepositoryTest {

    @Test
    fun `GIVEN a stored session WHEN auth state is observed THEN it starts unknown and then logged in`() = runTest {
        val env = Environment(storedSession = StubSession.stubSessionLocal())

        val states = env.repository.observe().take(2).toList()

        assertEquals(
            expected = listOf(AuthSessionState.Unknown, AuthSessionState.LoggedIn(UserId(StubSession.USER_ID))),
            actual = states,
        )
    }

    @Test
    fun `GIVEN no stored session WHEN auth state is observed THEN it starts unknown and then logged out`() = runTest {
        val env = Environment(storedSession = null)

        val states = env.repository.observe().take(2).toList()

        assertEquals(
            expected = listOf(AuthSessionState.Unknown, AuthSessionState.LoggedOut),
            actual = states,
        )
    }

    @Test
    fun `GIVEN a stored session WHEN auth state is observed twice THEN storage is read once`() = runTest {
        val env = Environment(storedSession = StubSession.stubSessionLocal())

        env.repository.observe().take(2).toList()
        env.repository.observe().take(1).toList()

        env.sessionStorage.readCall.called(times = 1)
    }

    @Test
    fun `GIVEN a stored session past its own expiry WHEN the launch decision is made THEN it is forgotten`() =
        runTest {
            val env = Environment(
                storedSession = StubSession.stubSessionLocal(),
                nowEpochSeconds = StubSession.REFRESH_TOKEN_EXPIRES_AT_EPOCH_SECONDS + 1,
            )

            val state = env.repository.observe().first { it !is AuthSessionState.Unknown }

            assertEquals(expected = AuthSessionState.LoggedOut, actual = state)
            env.sessionStorage.clearCall.called(times = 1)
        }

    @Test
    fun `GIVEN a device clock moved backwards WHEN the launch decision is made THEN it is still made locally`() =
        runTest {
            val env = Environment(storedSession = StubSession.stubSessionLocal(), nowEpochSeconds = 0L)

            val state = env.repository.observe().first { it !is AuthSessionState.Unknown }

            assertEquals(expected = AuthSessionState.LoggedIn(UserId(StubSession.USER_ID)), actual = state)
        }

    @Test
    fun `GIVEN a device clock moved forwards WHEN the launch decision is made THEN the session reads as expired`() =
        runTest {
            val env = Environment(
                storedSession = StubSession.stubSessionLocal(),
                nowEpochSeconds = StubSession.REFRESH_TOKEN_EXPIRES_AT_EPOCH_SECONDS * 2,
            )

            val state = env.repository.observe().first { it !is AuthSessionState.Unknown }

            assertEquals(expected = AuthSessionState.LoggedOut, actual = state)
        }

    @Test
    fun `GIVEN the access token has already expired WHEN a refresh runs THEN the stored token is presented as rejected`() =
        runTest {
            val env = Environment(
                storedSession = StubSession.stubSessionLocal(),
                nowEpochSeconds = StubSession.ACCESS_TOKEN_EXPIRES_AT_EPOCH_SECONDS + 1,
            )

            env.repository.refresh()

            env.accessTokenProvider.getAccessTokenCall.calledWith(StubSession.ACCESS_TOKEN)
        }

    @Test
    fun `GIVEN the access token expires inside the margin WHEN a refresh runs THEN exactly one is made`() = runTest {
        val env = Environment(
            storedSession = StubSession.stubSessionLocal(),
            nowEpochSeconds = StubSession.ACCESS_TOKEN_EXPIRES_AT_EPOCH_SECONDS - MARGIN_SECONDS,
        )

        env.repository.refresh()

        env.accessTokenProvider.getAccessTokenCall.called(times = 1)
    }

    @Test
    fun `GIVEN the access token expires beyond the margin WHEN a refresh runs THEN none is made`() = runTest {
        val env = Environment(
            storedSession = StubSession.stubSessionLocal(),
            nowEpochSeconds = StubSession.ACCESS_TOKEN_EXPIRES_AT_EPOCH_SECONDS - MARGIN_SECONDS - 1,
        )

        env.repository.refresh()

        env.accessTokenProvider.getAccessTokenCall.notCalled()
    }

    @Test
    fun `GIVEN a healthy access token WHEN a refresh runs THEN none is made`() = runTest {
        val env = Environment(storedSession = StubSession.stubSessionLocal())

        env.repository.refresh()

        env.accessTokenProvider.getAccessTokenCall.notCalled()
    }

    @Test
    fun `GIVEN no stored session WHEN a refresh runs THEN nothing is asked of the server`() = runTest {
        val env = Environment(storedSession = null)

        env.repository.refresh()

        env.accessTokenProvider.getAccessTokenCall.notCalled()
    }

    private class Environment(
        storedSession: SessionLocal?,
        nowEpochSeconds: Long = StubSession.NOW_EPOCH_SECONDS,
    ) {

        val sessionStorage = StubSessionStorage(session = storedSession)
        val accessTokenProvider = StubAccessTokenProvider()
        private val currentTime = CurrentTime { nowEpochSeconds }
        val repository: AuthSessionRepository = DefaultAuthSessionRepository(
            accessTokenProvider = accessTokenProvider,
            currentTime = currentTime,
            sessionStore = SessionStore(currentTime = currentTime, sessionStorage = sessionStorage),
        )
    }

    private companion object {
        const val MARGIN_SECONDS = 300L
    }
}
