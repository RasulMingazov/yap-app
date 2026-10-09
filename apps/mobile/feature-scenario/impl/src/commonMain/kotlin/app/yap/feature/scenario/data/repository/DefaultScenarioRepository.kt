package app.yap.feature.scenario.data.repository

import app.yap.contract.common.ApiErrorCode
import app.yap.contract.scenario.ScenarioStateDto
import app.yap.core.network.ApiError
import app.yap.core.network.ApiResult
import app.yap.feature.auth.api.entity.AuthSessionState
import app.yap.feature.auth.api.usecase.ObserveAuthSessionStateUseCase
import app.yap.feature.scenario.api.entity.OverviewState
import app.yap.feature.scenario.api.entity.ScenarioId
import app.yap.feature.scenario.data.CurrentDate
import app.yap.feature.scenario.data.local.OverviewSnapshotLocal
import app.yap.feature.scenario.data.local.OverviewSnapshotStore
import app.yap.feature.scenario.data.mapper.toDomain
import app.yap.feature.scenario.data.remote.ScenarioRemoteDataSource
import app.yap.feature.scenario.domain.repository.ActivationResult
import app.yap.feature.scenario.domain.repository.ScenarioRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class RefreshFailedException : RuntimeException("overview refresh failed")

internal class DefaultScenarioRepository(
    private val currentDate: CurrentDate,
    private val observeAuthSessionStateUseCase: ObserveAuthSessionStateUseCase,
    private val remoteDataSource: ScenarioRemoteDataSource,
    scope: CoroutineScope,
    private val snapshotStore: OverviewSnapshotStore,
) : ScenarioRepository {

    private val overviewState = MutableStateFlow<OverviewState>(OverviewState.Loading)
    private val restoreMutex = Mutex()

    private var isRestored = false

    override val state: Flow<OverviewState> = flow {
        restoreOnce()
        emitAll(overviewState)
    }

    init {
        scope.launch {
            observeAuthSessionStateUseCase().collect { authSessionState ->
                if (authSessionState is AuthSessionState.LoggedOut) forget()
            }
        }
    }

    override suspend fun refresh(): Result<Unit> {
        restoreOnce()
        markRefreshing()
        return when (val result = remoteDataSource.state(currentDate.isoDate())) {
            is ApiResult.Success -> {
                publish(result.value)
                Result.success(Unit)
            }
            is ApiResult.Failure -> {
                settleFailedRefresh()
                Result.failure(RefreshFailedException())
            }
        }
    }

    override suspend fun activate(id: ScenarioId): ActivationResult =
        mutate { today -> remoteDataSource.activate(scenarioId = id.value, todayIsoDate = today) }

    override suspend fun repeat(id: ScenarioId): ActivationResult =
        mutate { today -> remoteDataSource.repeat(scenarioId = id.value, todayIsoDate = today) }

    private suspend fun mutate(
        call: suspend (todayIsoDate: String) -> ApiResult<ScenarioStateDto>,
    ): ActivationResult = when (val result = call(currentDate.isoDate())) {
        is ApiResult.Success -> {
            publish(result.value)
            ActivationResult.Opened
        }
        is ApiResult.Failure -> result.error.toActivationResult()
    }

    // Every mutating call answers with the fresh aggregate, so the repository replaces its state
    // and snapshot from the response — no client-side arithmetic on slots, streak, or status.
    private suspend fun publish(state: ScenarioStateDto) {
        val accountId = currentAccountId() ?: return
        val today = currentDate.isoDate()
        overviewState.value = OverviewState.Ready(
            overview = state.toDomain(todayIsoDate = today),
            isRefreshing = false,
        )
        snapshotStore.write(
            OverviewSnapshotLocal(accountId = accountId, state = state, todayIsoDate = today),
        )
    }

    private suspend fun restoreOnce() {
        restoreMutex.withLock {
            if (isRestored) return
            isRestored = true

            val accountId = currentAccountId()
            val snapshot = snapshotStore.read()?.takeIf { stored -> stored.accountId == accountId }
            if (snapshot != null && overviewState.value is OverviewState.Loading) {
                overviewState.value = OverviewState.Ready(
                    overview = snapshot.state.toDomain(todayIsoDate = currentDate.isoDate()),
                    isRefreshing = false,
                )
            }
        }
    }

    private suspend fun forget() {
        snapshotStore.clear()
        restoreMutex.withLock { isRestored = false }
        overviewState.value = OverviewState.Loading
    }

    private suspend fun currentAccountId(): String? {
        val authSessionState = observeAuthSessionStateUseCase()
            .first { sessionState -> sessionState !is AuthSessionState.Unknown }
        return (authSessionState as? AuthSessionState.LoggedIn)?.userId?.value
    }

    private fun markRefreshing() {
        overviewState.update { current ->
            when (current) {
                is OverviewState.Ready -> current.copy(isRefreshing = true)
                is OverviewState.Unavailable -> OverviewState.Loading
                is OverviewState.Loading -> current
            }
        }
    }

    // A failed refresh stays silent once any content is shown (research R3); without content it is
    // the FR-006 full-screen error.
    private fun settleFailedRefresh() {
        overviewState.update { current ->
            when (current) {
                is OverviewState.Ready -> current.copy(isRefreshing = false)
                else -> OverviewState.Unavailable
            }
        }
    }

    private fun ApiError.toActivationResult(): ActivationResult = when {
        this is ApiError.Rejected && code == ApiErrorCode.ACCESS_REQUIRED -> ActivationResult.AccessRequired
        this is ApiError.Rejected && code == ApiErrorCode.SLOT_LIMIT_REACHED -> ActivationResult.SlotLimitReached
        else -> ActivationResult.Failed(this)
    }
}
