package app.yap.feature.auth.data.remote

import app.yap.feature.auth.data.identity.StubLoginProviderAdapter
import app.yap.feature.auth.data.local.SessionDb
import app.yap.feature.auth.data.local.StubSessionDb
import app.yap.feature.auth.data.local.StubSessionStorage
import app.yap.feature.auth.data.repository.DefaultSessionRepository
import app.yap.feature.auth.domain.entity.LoginProviderId
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

private const val EXPIRED_NOW_EPOCH_SECONDS = 2_000L

internal class DefaultAccessTokenProviderTest {

    @Test
    fun `GIVEN a stored session WHEN a token is requested THEN the stored access token is used`() = runTest {
        val env = Environment(storedSession = StubSessionDb.stubSessionDb())

        val accessToken = env.accessTokenProvider.getAccessToken(rejectedAccessToken = null)

        assertEquals(expected = StubSessionDb.ACCESS_TOKEN, actual = accessToken)
    }

    @Test
    fun `GIVEN a rejected access token WHEN a token is requested THEN one refresh returns the rotated token`() = runTest {
        val env = Environment(storedSession = StubSessionDb.stubSessionDb())

        val accessToken = env.accessTokenProvider.getAccessToken(
            rejectedAccessToken = StubSessionDb.ACCESS_TOKEN,
        )

        assertEquals(expected = StubAuthDto.ACCESS_TOKEN, actual = accessToken)
        env.authApi.refreshCall.called(1)
    }

    @Test
    fun `GIVEN a rotated token WHEN the previous token is rejected again THEN no second refresh is performed`() = runTest {
        val env = Environment(storedSession = StubSessionDb.stubSessionDb())
        env.accessTokenProvider.getAccessToken(rejectedAccessToken = StubSessionDb.ACCESS_TOKEN)

        env.accessTokenProvider.getAccessToken(rejectedAccessToken = StubSessionDb.ACCESS_TOKEN)

        env.authApi.refreshCall.called(1)
    }

    @Test
    fun `GIVEN a definitively rejected refresh WHEN a token is requested THEN no token is returned`() = runTest {
        val env = Environment(storedSession = StubSessionDb.stubSessionDb())
        env.authApi.refreshCall.returns(AuthApiResult.Failure(kind = AuthApiFailureKind.Rejected))

        val accessToken = env.accessTokenProvider.getAccessToken(
            rejectedAccessToken = StubSessionDb.ACCESS_TOKEN,
        )

        assertNull(accessToken)
    }

    private class Environment(storedSession: SessionDb?) {

        val authApi = StubAuthApi()
        val storage = StubSessionStorage(stored = storedSession)
        val repository = DefaultSessionRepository(
            adapters = mapOf(LoginProviderId.Google to StubLoginProviderAdapter()),
            authApi = authApi,
            currentTime = { EXPIRED_NOW_EPOCH_SECONDS },
            sessionStorage = storage,
        )
        val accessTokenProvider = DefaultAccessTokenProvider(sessionCredentials = repository)
    }
}
