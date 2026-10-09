package app.yap.feature.scenario.data.repository

import app.yap.core.network.ApiError
import app.yap.core.network.ApiResult
import app.yap.feature.auth.api.entity.AuthSessionState
import app.yap.feature.scenario.STUB_ACCOUNT_ID
import app.yap.feature.scenario.StubObserveAuthSessionStateUseCase
import app.yap.feature.scenario.api.entity.ScenarioId
import app.yap.feature.scenario.api.entity.ScenarioStatus
import app.yap.feature.scenario.data.StubScenarioState
import app.yap.feature.scenario.data.local.OverviewSnapshotLocal
import app.yap.feature.scenario.data.local.StubOverviewSnapshotStore
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
    fun `GIVEN a snapshot of this account WHEN the overview is read from cache THEN it is served without network`() =
        runTest {
            val env = Environment(this, snapshot = snapshotOf(accountId = STUB_ACCOUNT_ID))

            val overview = env.repository.get(forceUpdate = false)

            assertEquals(expected = StubScenarioState.FREE_TITLE, actual = overview?.scenarios?.first()?.title)
            env.remoteDataSource.stateCall.notCalled()
            env.cleanUp()
        }

    @Test
    fun `GIVEN no snapshot WHEN the overview is read from cache THEN there is nothing and no network`() = runTest {
        val env = Environment(this)

        val overview = env.repository.get(forceUpdate = false)

        assertNull(overview)
        env.remoteDataSource.stateCall.notCalled()
        env.cleanUp()
    }

    @Test
    fun `GIVEN a forced update WHEN the server answers THEN the overview comes back and lands in the snapshot`() =
        runTest {
            val env = Environment(this)

            val overview = env.repository.get(forceUpdate = true)

            assertEquals(expected = StubScenarioState.FREE_TITLE, actual = overview?.scenarios?.first()?.title)
            assertEquals(expected = STUB_ACCOUNT_ID, actual = env.snapshotStore.snapshots.value?.accountId)
            assertEquals(expected = overview, actual = env.repository.observe().first())
            env.cleanUp()
        }

    @Test
    fun `GIVEN a snapshot WHEN a forced update fails THEN the cached overview stands and is returned`() = runTest {
        val env = Environment(this, snapshot = snapshotOf(accountId = STUB_ACCOUNT_ID))
        env.remoteDataSource.stateCall.returns(ApiResult.Failure(ApiError.Unavailable))

        val overview = env.repository.get(forceUpdate = true)

        assertNotNull(overview)
        assertEquals(expected = STUB_ACCOUNT_ID, actual = env.snapshotStore.snapshots.value?.accountId)
        env.cleanUp()
    }

    @Test
    fun `GIVEN no snapshot WHEN a forced update fails THEN there is nothing to show`() = runTest {
        val env = Environment(this)
        env.remoteDataSource.stateCall.returns(ApiResult.Failure(ApiError.Unavailable))

        val overview = env.repository.get(forceUpdate = true)

        assertNull(overview)
        env.cleanUp()
    }

    @Test
    fun `GIVEN an activation WHEN the server answers with the aggregate THEN the snapshot is replaced`() = runTest {
        val env = Environment(this)
        val activated = StubScenarioState.stubStateDto(
            scenarios = listOf(StubScenarioState.stubActiveScenarioDto(), StubScenarioState.stubLockedScenarioDto()),
            slotsUsed = 1,
        )
        env.remoteDataSource.activateCall.returns(ApiResult.Success(activated))

        val result = env.repository.activate(ScenarioId(StubScenarioState.FREE_ID))

        assertIs<ActivationResult.Opened>(result)
        assertIs<ScenarioStatus.Active>(env.repository.observe().first()?.scenarios?.first()?.status)
        assertEquals(expected = 1, actual = env.snapshotStore.snapshots.value?.state?.slotsUsed)
        env.cleanUp()
    }

    @Test
    fun `GIVEN another account's snapshot WHEN the overview is observed THEN it is not shown`() = runTest {
        val env = Environment(this, snapshot = snapshotOf(accountId = "someone-else"))

        assertNull(env.repository.observe().first())
        env.cleanUp()
    }

    @Test
    fun `GIVEN a stored snapshot WHEN the user logs out THEN the offline copy is gone`() = runTest {
        val env = Environment(this, snapshot = snapshotOf(accountId = STUB_ACCOUNT_ID))

        env.observeAuthSessionStateUseCase.authSessionStates.value = AuthSessionState.LoggedOut
        runCurrent()

        assertNull(env.snapshotStore.snapshots.value)
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
