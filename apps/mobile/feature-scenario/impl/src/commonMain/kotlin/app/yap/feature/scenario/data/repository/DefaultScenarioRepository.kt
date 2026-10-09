package app.yap.feature.scenario.data.repository

import app.yap.contract.common.ApiErrorCode
import app.yap.contract.scenario.ScenarioStateDto
import app.yap.core.network.ApiError
import app.yap.core.network.ApiResult
import app.yap.feature.auth.api.entity.AuthSessionState
import app.yap.feature.auth.api.usecase.ObserveAuthSessionStateUseCase
import app.yap.feature.scenario.api.entity.ScenarioId
import app.yap.feature.scenario.api.entity.ScenarioOverview
import app.yap.feature.scenario.data.CurrentDate
import app.yap.feature.scenario.data.local.OverviewSnapshotLocal
import app.yap.feature.scenario.data.local.OverviewSnapshotStore
import app.yap.feature.scenario.data.mapper.toDomain
import app.yap.feature.scenario.data.remote.ScenarioRemoteDataSource
import app.yap.feature.scenario.domain.repository.ActivationResult
import app.yap.feature.scenario.domain.repository.ScenarioRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

internal class DefaultScenarioRepository(
    private val currentDate: CurrentDate,
    private val observeAuthSessionStateUseCase: ObserveAuthSessionStateUseCase,
    private val remoteDataSource: ScenarioRemoteDataSource,
    scope: CoroutineScope,
    private val snapshotStore: OverviewSnapshotStore,
) : ScenarioRepository {

    init {
        scope.launch {
            observeAuthSessionStateUseCase()
                .filterIsInstance<AuthSessionState.LoggedOut>()
                .collect { snapshotStore.clear() }
        }
    }

    override fun observe(): Flow<ScenarioOverview?> = snapshotStore.observe().map { snapshot -> snapshot.toOverview() }

    override suspend fun get(forceUpdate: Boolean): ScenarioOverview? {
        if (!forceUpdate) return snapshotStore.observe().first().toOverview()

        return when (val result = remoteDataSource.state(currentDate.isoDate())) {
            is ApiResult.Success -> save(result.value)
            is ApiResult.Failure -> snapshotStore.observe().first().toOverview()
        }
    }

    override suspend fun activate(id: ScenarioId): ActivationResult =
        mutate { today -> remoteDataSource.activate(scenarioId = id.value, todayIsoDate = today) }

    override suspend fun repeat(id: ScenarioId): ActivationResult =
        mutate { today -> remoteDataSource.repeat(scenarioId = id.value, todayIsoDate = today) }

    private suspend fun OverviewSnapshotLocal?.toOverview(): ScenarioOverview? {
        val snapshot = this ?: return null
        if (snapshot.accountId != currentAccountId()) return null
        return snapshot.state.toDomain(todayIsoDate = currentDate.isoDate())
    }

    private suspend fun mutate(
        call: suspend (todayIsoDate: String) -> ApiResult<ScenarioStateDto>,
    ): ActivationResult = when (val result = call(currentDate.isoDate())) {
        is ApiResult.Success -> {
            save(result.value)
            ActivationResult.Opened
        }
        is ApiResult.Failure -> result.error.toActivationResult()
    }

    private suspend fun save(state: ScenarioStateDto): ScenarioOverview? {
        val accountId = currentAccountId() ?: return null
        snapshotStore.write(OverviewSnapshotLocal(accountId = accountId, state = state))
        return state.toDomain(todayIsoDate = currentDate.isoDate())
    }

    private suspend fun currentAccountId(): String? {
        val authSessionState = observeAuthSessionStateUseCase()
            .first { sessionState -> sessionState !is AuthSessionState.Unknown }
        return (authSessionState as? AuthSessionState.LoggedIn)?.userId?.value
    }

    private fun ApiError.toActivationResult(): ActivationResult = when {
        this is ApiError.Rejected && code == ApiErrorCode.ACCESS_REQUIRED -> ActivationResult.AccessRequired
        this is ApiError.Rejected && code == ApiErrorCode.SLOT_LIMIT_REACHED -> ActivationResult.SlotLimitReached
        else -> ActivationResult.Failed(this)
    }
}
