package app.yap.feature.scenario.data.repository

import app.yap.core.network.ApiError
import app.yap.core.network.ApiResult
import app.yap.feature.auth.api.entity.AuthSessionState
import app.yap.feature.scenario.STUB_ACCOUNT_ID
import app.yap.feature.scenario.StubObserveAuthSessionStateUseCase
import app.yap.feature.scenario.api.entity.OverviewState
import app.yap.feature.scenario.api.entity.ScenarioId
import app.yap.feature.scenario.api.entity.ScenarioStatus
import app.yap.feature.scenario.data.CurrentDate
import app.yap.feature.scenario.data.StubScenarioState
import app.yap.feature.scenario.data.local.OverviewSnapshotLocal
import app.yap.feature.scenario.data.local.StubOverviewSnapshotStore
import app.yap.feature.scenario.data.remote.StubScenarioRemoteDataSource
import app.yap.feature.scenario.domain.repository.ActivationResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

internal class DefaultScenarioRepositoryTest {

    @Test
    fun `GIVEN a snapshot of this account WHEN the state is observed THEN content is ready without network`() =
        runTest {
            val env = Environment(this, snapshot = snapshotOf(accountId = STUB_ACCOUNT_ID))

            val state = env.repository.state.first()

            assertIs<OverviewState.Ready>(state)
            assertEquals(expected = StubScenarioState.FREE_TITLE, actual = state.overview.scenarios.first().title)
            env.remoteDataSource.stateCall.notCalled()
            env.cleanUp()
        }

    @Test
    fun `GIVEN no snapshot WHEN a refresh is in flight THEN the state is loading`() = runTest {
        val env = Environment(this)

        assertIs<OverviewState.Loading>(env.repository.state.first())
        env.cleanUp()
    }

    @Test
    fun `GIVEN no snapshot WHEN the refresh fails THEN the state is unavailable`() = runTest {
        val env = Environment(this)
        env.remoteDataSource.stateCall.returns(ApiResult.Failure(ApiError.Unavailable))

        val result = env.repository.refresh()

        assertTrue(result.isFailure)
        assertIs<OverviewState.Unavailable>(env.repository.state.first())
        env.cleanUp()
    }

    @Test
    fun `GIVEN shown content WHEN a refresh fails THEN the failure stays silent and content stands`() = runTest {
        val env = Environment(this, snapshot = snapshotOf(accountId = STUB_ACCOUNT_ID))
        env.remoteDataSource.stateCall.returns(ApiResult.Failure(ApiError.Unavailable))

        env.repository.refresh()

        val state = env.repository.state.first()
        assertIs<OverviewState.Ready>(state)
        assertEquals(expected = false, actual = state.isRefreshing)
        env.cleanUp()
    }

    @Test
    fun `GIVEN a successful refresh WHEN it lands THEN state and snapshot hold the fresh aggregate`() = runTest {
        val env = Environment(this)

        val result = env.repository.refresh()

        assertTrue(result.isSuccess)
        assertIs<OverviewState.Ready>(env.repository.state.first())
        assertEquals(expected = STUB_ACCOUNT_ID, actual = env.snapshotStore.stored?.accountId)
        env.cleanUp()
    }

    @Test
    fun `GIVEN an activation WHEN the server answers with the aggregate THEN state and snapshot are replaced`() =
        runTest {
            val env = Environment(this)
            val activated = StubScenarioState.stubStateDto(
                scenarios = listOf(StubScenarioState.stubActiveScenarioDto(), StubScenarioState.stubLockedScenarioDto()),
                slotsUsed = 1,
            )
            env.remoteDataSource.activateCall.returns(ApiResult.Success(activated))

            val result = env.repository.activate(ScenarioId(StubScenarioState.FREE_ID))

            assertIs<ActivationResult.Opened>(result)
            val state = env.repository.state.first()
            assertIs<OverviewState.Ready>(state)
            assertIs<ScenarioStatus.Active>(state.overview.scenarios.first().status)
            assertEquals(expected = 1, actual = env.snapshotStore.stored?.state?.slotsUsed)
            env.cleanUp()
        }

    @Test
    fun `GIVEN another account's snapshot WHEN the state is observed THEN it is not shown`() = runTest {
        val env = Environment(this, snapshot = snapshotOf(accountId = "someone-else"))

        assertIs<OverviewState.Loading>(env.repository.state.first())
        env.cleanUp()
    }

    @Test
    fun `GIVEN a stored snapshot WHEN the user logs out THEN the offline copy is gone and state resets`() =
        runTest {
            val env = Environment(this, snapshot = snapshotOf(accountId = STUB_ACCOUNT_ID))
            assertIs<OverviewState.Ready>(env.repository.state.first())

            env.observeAuthSessionStateUseCase.authSessionStates.value = AuthSessionState.LoggedOut
            runCurrent()

            assertEquals(expected = null, actual = env.snapshotStore.stored)
            assertIs<OverviewState.Loading>(env.repository.state.first())
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

    private fun snapshotOf(accountId: String): OverviewSnapshotLocal = OverviewSnapshotLocal(
        accountId = accountId,
        state = StubScenarioState.stubStateDto(),
        todayIsoDate = StubScenarioState.TODAY,
    )

    private class Environment(
        testScope: TestScope,
        snapshot: OverviewSnapshotLocal? = null,
    ) {

        val observeAuthSessionStateUseCase = StubObserveAuthSessionStateUseCase()
        val remoteDataSource = StubScenarioRemoteDataSource()
        val snapshotStore = StubOverviewSnapshotStore(snapshot = snapshot)
        private val scope = CoroutineScope(StandardTestDispatcher(testScope.testScheduler))
        val repository = DefaultScenarioRepository(
            currentDate = CurrentDate { StubScenarioState.TODAY },
            observeAuthSessionStateUseCase = observeAuthSessionStateUseCase,
            remoteDataSource = remoteDataSource,
            scope = scope,
            snapshotStore = snapshotStore,
        )

        fun cleanUp() {
            scope.cancel()
        }
    }
}
