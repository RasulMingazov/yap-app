package app.yap.feature.auth.data

import app.yap.contract.auth.RefreshCredentialsDto
import app.yap.contract.auth.SessionDto
import app.yap.core.network.ApiClient
import app.yap.core.network.ApiError
import app.yap.core.network.ApiResult
import app.yap.core.network.createNetworkClient
import app.yap.feature.auth.api.entity.AuthSessionState
import app.yap.feature.auth.api.entity.UserId
import app.yap.feature.auth.data.local.SessionLocal
import app.yap.feature.auth.data.local.StubSession
import app.yap.feature.auth.data.local.StubSessionStorage
import app.yap.feature.auth.data.remote.AuthRemoteDataSource
import app.yap.feature.auth.data.remote.DefaultAuthRemoteDataSource
import app.yap.feature.auth.data.remote.StubAuthRemoteDataSource
import app.yap.feature.auth.data.remote.StubSessionDto
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

internal class DefaultAccessTokenProviderTest {

    @Test
    fun `GIVEN no stored session WHEN a token is requested THEN none is returned and none is fetched`() = runTest {
        val env = Environment(storedSession = null)

        val token = env.provider.getAccessToken(rejectedAccessToken = null)

        assertNull(actual = token)
        env.remoteDataSource.refreshCall.notCalled()
    }

    @Test
    fun `GIVEN a stored session WHEN a token is requested THEN the stored one is returned unchanged`() = runTest {
        val env = Environment(storedSession = StubSession.stubSessionLocal())

        val token = env.provider.getAccessToken(rejectedAccessToken = null)

        assertEquals(expected = StubSession.ACCESS_TOKEN, actual = token)
        env.remoteDataSource.refreshCall.notCalled()
    }

    @Test
    fun `GIVEN a rejected token WHEN the rotation succeeds THEN the new session is stored and published`() = runTest {
        val env = Environment(storedSession = StubSession.stubSessionLocal())

        val token = env.provider.getAccessToken(rejectedAccessToken = StubSession.ACCESS_TOKEN)

        assertEquals(expected = StubSession.ROTATED_ACCESS_TOKEN, actual = token)
        env.remoteDataSource.refreshCall.calledWith(RefreshCredentialsDto(refreshToken = StubSession.REFRESH_TOKEN))
        env.sessionStorage.writeCall.calledWith(
            StubSession.stubSessionLocal(
                accessToken = StubSession.ROTATED_ACCESS_TOKEN,
                refreshToken = StubSession.ROTATED_REFRESH_TOKEN,
            ),
        )
        assertEquals(
            expected = AuthSessionState.LoggedIn(UserId(StubSession.USER_ID)),
            actual = env.sessionStore.sessionState.value,
        )
    }

    @Test
    fun `GIVEN the server refuses the session WHEN a rotation runs THEN storage is cleared and logged out follows`() =
        runTest {
            val env = Environment(storedSession = StubSession.stubSessionLocal())
            env.remoteDataSource.refreshCall.returns(ApiResult.Failure(ApiError.Rejected(code = null)))

            val token = env.provider.getAccessToken(rejectedAccessToken = StubSession.ACCESS_TOKEN)

            assertNull(actual = token)
            env.sessionStorage.clearCall.called(times = 1)
            assertEquals(
                expected = AuthSessionState.LoggedOut,
                actual = env.sessionStore.sessionState.value,
            )
        }

    @Test
    fun `GIVEN the server gives no answer WHEN a rotation runs THEN the stored session survives`() = runTest {
        val env = Environment(storedSession = StubSession.stubSessionLocal())
        env.remoteDataSource.refreshCall.returns(ApiResult.Failure(ApiError.Unavailable))

        val token = env.provider.getAccessToken(rejectedAccessToken = StubSession.ACCESS_TOKEN)

        assertNull(actual = token)
        env.sessionStorage.clearCall.notCalled()
        assertEquals(expected = AuthSessionState.Unknown, actual = env.sessionStore.sessionState.value)
    }

    @Test
    fun `GIVEN the caller is rate limited WHEN a rotation runs THEN the user stays logged in`() = runTest {
        val sessionStorage = StubSessionStorage(session = StubSession.stubSessionLocal())
        val sessionStore = SessionStore(
            currentTime = CurrentTime { StubSession.NOW_EPOCH_SECONDS },
            sessionStorage = sessionStorage,
        )
        val provider = DefaultAccessTokenProvider(
            authRemoteDataSource = lazy { rateLimitedDataSource() },
            sessionStore = sessionStore,
        )

        val token = provider.getAccessToken(rejectedAccessToken = StubSession.ACCESS_TOKEN)

        assertNull(actual = token)
        sessionStorage.clearCall.notCalled()
        assertEquals(expected = AuthSessionState.Unknown, actual = sessionStore.sessionState.value)
    }

    @Test
    fun `GIVEN a rotation is in flight WHEN a second caller arrives THEN it waits rather than rotating too`() =
        runTest {
            val env = Environment(storedSession = StubSession.stubSessionLocal(), isRotationGated = true)

            val first = async { env.provider.getAccessToken(rejectedAccessToken = StubSession.ACCESS_TOKEN) }
            runCurrent()
            val second = async { env.provider.getAccessToken(rejectedAccessToken = StubSession.ACCESS_TOKEN) }
            runCurrent()
            env.releaseRotation()
            advanceUntilIdle()

            first.await()
            second.await()
            env.remoteDataSource.refreshCall.called(times = 1)
        }

    @Test
    fun `GIVEN a token arrived while a caller waited WHEN it resumes THEN that token is returned`() = runTest {
        val env = Environment(storedSession = StubSession.stubSessionLocal(), isRotationGated = true)

        val first = async { env.provider.getAccessToken(rejectedAccessToken = StubSession.ACCESS_TOKEN) }
        runCurrent()
        val second = async { env.provider.getAccessToken(rejectedAccessToken = StubSession.ACCESS_TOKEN) }
        runCurrent()
        env.releaseRotation()
        advanceUntilIdle()

        assertEquals(expected = StubSession.ROTATED_ACCESS_TOKEN, actual = first.await())
        assertEquals(expected = StubSession.ROTATED_ACCESS_TOKEN, actual = second.await())
    }

    private fun rateLimitedDataSource(): AuthRemoteDataSource = DefaultAuthRemoteDataSource(
        apiClient = ApiClient(
            createNetworkClient(
                baseUrl = "https://yap.test",
                engine = MockEngine { respond(content = "", status = HttpStatusCode.TooManyRequests) },
                timeouts = null,
            ),
        ),
    )

    private class GatedAuthRemoteDataSource(
        private val delegate: AuthRemoteDataSource,
        private val gate: CompletableDeferred<Unit>,
    ) : AuthRemoteDataSource by delegate {

        override suspend fun refresh(credentials: RefreshCredentialsDto): ApiResult<SessionDto> {
            gate.await()
            return delegate.refresh(credentials)
        }
    }

    private class Environment(
        storedSession: SessionLocal?,
        isRotationGated: Boolean = false,
    ) {

        val sessionStorage = StubSessionStorage(session = storedSession)
        val sessionStore = SessionStore(
            currentTime = CurrentTime { StubSession.NOW_EPOCH_SECONDS },
            sessionStorage = sessionStorage,
        )
        val remoteDataSource = StubAuthRemoteDataSource().apply {
            refreshCall.returns(
                ApiResult.Success(
                    StubSessionDto.stubSessionDto(
                        accessToken = StubSession.ROTATED_ACCESS_TOKEN,
                        refreshToken = StubSession.ROTATED_REFRESH_TOKEN,
                    ),
                ),
            )
        }

        private val gate = CompletableDeferred<Unit>().apply { if (!isRotationGated) complete(Unit) }

        val provider = DefaultAccessTokenProvider(
            authRemoteDataSource = lazy {
                if (isRotationGated) {
                    GatedAuthRemoteDataSource(delegate = remoteDataSource, gate = gate)
                } else {
                    remoteDataSource
                }
            },
            sessionStore = sessionStore,
        )

        fun releaseRotation() = gate.complete(Unit)
    }
}
