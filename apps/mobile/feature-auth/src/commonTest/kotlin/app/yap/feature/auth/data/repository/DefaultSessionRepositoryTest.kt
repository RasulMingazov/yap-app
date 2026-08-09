package app.yap.feature.auth.data.repository

import app.yap.feature.auth.data.identity.StubLoginProviderAdapter
import app.yap.feature.auth.data.local.SessionLocal
import app.yap.feature.auth.data.local.StubSessionLocal
import app.yap.feature.auth.data.local.StubSessionStorage
import app.yap.feature.auth.data.remote.AuthApiFailureKind
import app.yap.feature.auth.data.remote.AuthApiResult
import app.yap.feature.auth.data.remote.StubAuthApi
import app.yap.feature.auth.data.remote.StubAuthDto
import app.yap.feature.auth.domain.entity.AccountId
import app.yap.feature.auth.domain.entity.LoginProviderId
import app.yap.feature.auth.domain.entity.Session
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

private const val EXPIRED_NOW_EPOCH_SECONDS = 2_000L
private const val VALID_NOW_EPOCH_SECONDS = 100L

@OptIn(ExperimentalCoroutinesApi::class)
internal class DefaultSessionRepositoryTest {

    @Test
    fun `GIVEN a stored session with valid access WHEN restoring THEN it is restored without a refresh`() = runTest {
        val env = Environment(
            nowEpochSeconds = VALID_NOW_EPOCH_SECONDS,
            storedSession = StubSessionLocal.stubSessionLocal(),
        )

        val session = env.repository.get(forceUpdate = true)

        assertEquals(
            expected = Session(accountId = AccountId(StubSessionLocal.ACCOUNT_ID)),
            actual = session,
        )
        env.authApi.refreshCall.notCalled()
    }

    @Test
    fun `GIVEN expired access WHEN two callers restore at once THEN one refresh is performed`() = runTest {
        val env = Environment(
            nowEpochSeconds = EXPIRED_NOW_EPOCH_SECONDS,
            storedSession = StubSessionLocal.stubSessionLocal(),
        )

        awaitAll(
            async { env.repository.get(forceUpdate = true) },
            async { env.repository.get(forceUpdate = true) },
        )

        env.authApi.refreshCall.called(1)
    }

    @Test
    fun `GIVEN expired access WHEN the refresh succeeds THEN rotated credentials are stored before they are published`() = runTest {
        val env = Environment(
            nowEpochSeconds = EXPIRED_NOW_EPOCH_SECONDS,
            storedSession = StubSessionLocal.stubSessionLocal(),
        )
        val writesAtEmission = mutableListOf<Int>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            env.repository.observe().collect { writesAtEmission += env.storage.writeCall.callCount }
        }

        env.repository.get(forceUpdate = true)

        assertEquals(expected = listOf(0, 0, 1), actual = writesAtEmission)
    }

    @Test
    fun `GIVEN a definitively rejected refresh WHEN restoring THEN the stored session is cleared`() = runTest {
        val env = Environment(
            nowEpochSeconds = EXPIRED_NOW_EPOCH_SECONDS,
            storedSession = StubSessionLocal.stubSessionLocal(),
        )
        env.authApi.refreshCall.returns(AuthApiResult.Failure(kind = AuthApiFailureKind.Rejected))

        val session = env.repository.get(forceUpdate = true)

        assertNull(session)
        env.storage.clearCall.called(1)
    }

    @Test
    fun `GIVEN a transient refresh failure WHEN restoring THEN the stored session is preserved`() = runTest {
        val env = Environment(
            nowEpochSeconds = EXPIRED_NOW_EPOCH_SECONDS,
            storedSession = StubSessionLocal.stubSessionLocal(),
        )
        env.authApi.refreshCall.returns(AuthApiResult.Failure(kind = AuthApiFailureKind.Unavailable))

        env.repository.get(forceUpdate = true)

        env.storage.clearCall.notCalled()
    }

    @Test
    fun `GIVEN an offline start WHEN restoring THEN the stored session opens provisionally`() = runTest {
        val env = Environment(
            nowEpochSeconds = EXPIRED_NOW_EPOCH_SECONDS,
            storedSession = StubSessionLocal.stubSessionLocal(),
        )
        env.authApi.refreshCall.returns(AuthApiResult.Failure(kind = AuthApiFailureKind.Unavailable))

        val session = env.repository.get(forceUpdate = true)

        assertEquals(
            expected = Session(accountId = AccountId(StubSessionLocal.ACCOUNT_ID)),
            actual = session,
        )
    }

    @Test
    fun `GIVEN a stored session WHEN only observing THEN no storage read is started`() = runTest {
        val env = Environment(storedSession = StubSessionLocal.stubSessionLocal())

        val session = env.repository.observe().first()

        assertNull(session)
    }

    @Test
    fun `GIVEN a successful login WHEN observing THEN the new session is published`() = runTest {
        val env = Environment()

        env.repository.logIn(LoginProviderId.Google)

        assertEquals(
            expected = Session(accountId = AccountId(StubAuthDto.ACCOUNT_ID)),
            actual = env.repository.observe().first(),
        )
    }

    private class Environment(
        nowEpochSeconds: Long = VALID_NOW_EPOCH_SECONDS,
        storedSession: SessionLocal? = null,
    ) {

        val adapter = StubLoginProviderAdapter()
        val authApi = StubAuthApi()
        val storage = StubSessionStorage(stored = storedSession)
        val repository = DefaultSessionRepository(
            adapters = mapOf(LoginProviderId.Google to adapter),
            authApi = authApi,
            currentTime = { nowEpochSeconds },
            sessionStorage = storage,
        )
    }
}
