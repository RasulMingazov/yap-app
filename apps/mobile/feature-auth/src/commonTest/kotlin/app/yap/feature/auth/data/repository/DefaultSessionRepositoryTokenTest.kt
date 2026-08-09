package app.yap.feature.auth.data.repository

import app.yap.feature.auth.data.identity.StubLoginProviderAdapter
import app.yap.feature.auth.data.local.SessionLocal
import app.yap.feature.auth.data.local.StubSessionLocal
import app.yap.feature.auth.data.local.StubSessionStorage
import app.yap.feature.auth.data.remote.AuthApiFailureKind
import app.yap.feature.auth.data.remote.AuthApiResult
import app.yap.feature.auth.data.remote.StubAuthApi
import app.yap.feature.auth.data.remote.StubAuthDto
import app.yap.feature.auth.domain.entity.LoginProviderId
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

private const val EXPIRED_NOW_EPOCH_SECONDS = 2_000L

internal class DefaultSessionRepositoryTokenTest {

    @Test
    fun `GIVEN a stored session WHEN a token is requested THEN the stored access token is used`() = runTest {
        val env = Environment(storedSession = StubSessionLocal.stubSessionLocal())

        val accessToken = env.repository.getAccessToken(rejectedAccessToken = null)

        assertEquals(expected = StubSessionLocal.ACCESS_TOKEN, actual = accessToken)
    }

    @Test
    fun `GIVEN a rejected access token WHEN a token is requested THEN one refresh returns the rotated token`() = runTest {
        val env = Environment(storedSession = StubSessionLocal.stubSessionLocal())

        val accessToken = env.repository.getAccessToken(
            rejectedAccessToken = StubSessionLocal.ACCESS_TOKEN,
        )

        assertEquals(expected = StubAuthDto.ACCESS_TOKEN, actual = accessToken)
        env.authApi.refreshCall.called(1)
    }

    @Test
    fun `GIVEN a rotated token WHEN the previous token is rejected again THEN no second refresh is performed`() = runTest {
        val env = Environment(storedSession = StubSessionLocal.stubSessionLocal())
        env.repository.getAccessToken(rejectedAccessToken = StubSessionLocal.ACCESS_TOKEN)

        env.repository.getAccessToken(rejectedAccessToken = StubSessionLocal.ACCESS_TOKEN)

        env.authApi.refreshCall.called(1)
    }

    @Test
    fun `GIVEN a definitively rejected refresh WHEN a token is requested THEN no token is returned`() = runTest {
        val env = Environment(storedSession = StubSessionLocal.stubSessionLocal())
        env.authApi.refreshCall.returns(AuthApiResult.Failure(kind = AuthApiFailureKind.Rejected))

        val accessToken = env.repository.getAccessToken(
            rejectedAccessToken = StubSessionLocal.ACCESS_TOKEN,
        )

        assertNull(accessToken)
    }

    private class Environment(storedSession: SessionLocal?) {

        val authApi = StubAuthApi()
        val storage = StubSessionStorage(stored = storedSession)
        val repository = DefaultSessionRepository(
            adapters = mapOf(LoginProviderId.Google to StubLoginProviderAdapter()),
            authApi = authApi,
            currentTime = { EXPIRED_NOW_EPOCH_SECONDS },
            sessionStorage = storage,
        )
    }
}
