package app.yap.feature.scenario.data.repository

import app.yap.contract.scenario.ScenarioStateDto
import app.yap.core.network.ApiError
import app.yap.core.network.ApiResult
import app.yap.feature.auth.api.entity.AuthSessionState
import app.yap.feature.scenario.STUB_ACCOUNT_ID
import app.yap.feature.scenario.StubObserveAuthSessionStateUseCase
import app.yap.feature.scenario.api.entity.ScenarioId
import app.yap.feature.scenario.api.entity.ScenarioStatus
import app.yap.feature.scenario.data.StubScenarioState
import app.yap.feature.scenario.data.local.StubScenarioDao
import app.yap.feature.scenario.data.mapper.toDb
import app.yap.feature.scenario.data.remote.StubScenarioRemoteDataSource
import app.yap.feature.scenario.domain.repository.ActivationResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

internal class DefaultScenarioRepositoryTest {

    @Test
    fun `GIVEN rows of this account WHEN the overview is read from cache THEN it is served without network`() =
        runTest {
            val env = Environment(this, stored = StubScenarioState.stubStateDto())

            val overview = env.repository.get(forceUpdate = false)

            assertEquals(expected = StubScenarioState.FREE_TITLE, actual = overview?.scenarios?.first()?.title)
            env.remoteDataSource.stateCall.notCalled()
            env.cleanUp()
        }

    @Test
    fun `GIVEN an empty database WHEN the overview is read from cache THEN there is nothing and no network`() =
        runTest {
            val env = Environment(this)

            val overview = env.repository.get(forceUpdate = false)

            assertNull(overview)
            env.remoteDataSource.stateCall.notCalled()
            env.cleanUp()
        }

    @Test
    fun `GIVEN a forced update WHEN the server answers THEN the overview comes back and lands in the database`() =
        runTest {
            val env = Environment(this)

            val overview = env.repository.get(forceUpdate = true)

            assertEquals(expected = StubScenarioState.FREE_TITLE, actual = overview?.scenarios?.first()?.title)
            assertEquals(expected = STUB_ACCOUNT_ID, actual = env.scenarioDao.overview.value?.accountId)
            assertEquals(expected = overview, actual = env.repository.observe().first())
            env.cleanUp()
        }

    @Test
    fun `GIVEN stored rows WHEN a forced update fails THEN the cached overview stands and is returned`() = runTest {
        val env = Environment(this, stored = StubScenarioState.stubStateDto())
        env.remoteDataSource.stateCall.returns(ApiResult.Failure(ApiError.Unavailable))

        val overview = env.repository.get(forceUpdate = true)

        assertNotNull(overview)
        assertEquals(expected = STUB_ACCOUNT_ID, actual = env.scenarioDao.overview.value?.accountId)
        env.cleanUp()
    }

    @Test
    fun `GIVEN an empty database WHEN a forced update fails THEN there is nothing to show`() = runTest {
        val env = Environment(this)
        env.remoteDataSource.stateCall.returns(ApiResult.Failure(ApiError.Unavailable))

        val overview = env.repository.get(forceUpdate = true)

        assertNull(overview)
        env.cleanUp()
    }

    @Test
    fun `GIVEN an activation WHEN the server answers with the aggregate THEN the rows are replaced`() = runTest {
        val env = Environment(this)
        val activated = StubScenarioState.stubStateDto(
            scenarios = listOf(StubScenarioState.stubActiveScenarioDto(), StubScenarioState.stubLockedScenarioDto()),
            slotsUsed = 1,
        )
        env.remoteDataSource.activateCall.returns(ApiResult.Success(activated))

        val result = env.repository.activate(ScenarioId(StubScenarioState.FREE_ID))

        assertIs<ActivationResult.Opened>(result)
        assertIs<ScenarioStatus.Active>(env.repository.observe().first()?.scenarios?.first()?.status)
        assertEquals(expected = 1, actual = env.scenarioDao.overview.value?.slotsUsed)
        env.cleanUp()
    }

    @Test
    fun `GIVEN another account's rows WHEN the overview is observed THEN it is not shown`() = runTest {
        val env = Environment(this, stored = StubScenarioState.stubStateDto(), storedAccountId = "someone-else")

        assertNull(env.repository.observe().first())
        env.cleanUp()
    }

    @Test
    fun `GIVEN stored rows WHEN the user logs out THEN the offline copy is gone`() = runTest {
        val env = Environment(this, stored = StubScenarioState.stubStateDto())

        env.observeAuthSessionStateUseCase.authSessionStates.value = AuthSessionState.LoggedOut
        runCurrent()

        assertNull(env.scenarioDao.overview.value)
        assertEquals(expected = emptyList(), actual = env.scenarioDao.scenarios.value)
        env.cleanUp()
    }

    @Test
    fun `GIVEN a slot limit refusal WHEN activating THEN the result names the limit`() = runTest {
        val env = Environment(this)
        env.remoteDataSource.activateCall.returns(
            ApiResult.Failure(ApiError.Rejected(code = "slot_limit_reached")),
        )

        val result = env.repository.activate(ScenarioId(StubScenarioState.LOCKED_ID))

        assertIs<ActivationResult.SlotLimitReached>(result)
        env.cleanUp()
    }

    @Test
    fun `GIVEN an access refusal WHEN activating THEN the result demands access`() = runTest {
        val env = Environment(this)
        env.remoteDataSource.activateCall.returns(
            ApiResult.Failure(ApiError.Rejected(code = "access_required")),
        )

        val result = env.repository.activate(ScenarioId(StubScenarioState.LOCKED_ID))

        assertIs<ActivationResult.AccessRequired>(result)
        env.cleanUp()
    }

    private class Environment(
        testScope: TestScope,
        stored: ScenarioStateDto? = null,
        storedAccountId: String = STUB_ACCOUNT_ID,
    ) {

        val observeAuthSessionStateUseCase = StubObserveAuthSessionStateUseCase()
        val remoteDataSource = StubScenarioRemoteDataSource()
        val scenarioDao = StubScenarioDao().apply {
            if (stored != null) {
                seed(
                    overview = stored.toDb(storedAccountId),
                    scenarios = stored.scenarios.map { scenario -> scenario.toDb() },
                    objectives = stored.scenarios.flatMap { scenario -> scenario.objectives.map { it.toDb(scenario.id) } },
                )
            }
        }
        private val scope = CoroutineScope(StandardTestDispatcher(testScope.testScheduler))
        val repository = DefaultScenarioRepository(
            observeAuthSessionStateUseCase = observeAuthSessionStateUseCase,
            remoteDataSource = remoteDataSource,
            scenarioDao = scenarioDao,
            scope = scope,
        )

        fun cleanUp() {
            scope.cancel()
        }
    }
}
