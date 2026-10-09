package app.yap.feature.scenario.data.repository

import app.yap.contract.common.ApiErrorCode
import app.yap.contract.scenario.ScenarioStateDto
import app.yap.core.network.ApiError
import app.yap.core.network.ApiResult
import app.yap.feature.auth.api.entity.AuthSessionState
import app.yap.feature.auth.api.usecase.ObserveAuthSessionStateUseCase
import app.yap.feature.scenario.api.entity.ScenarioId
import app.yap.feature.scenario.api.entity.ScenarioOverview
import app.yap.feature.scenario.data.local.ScenarioDao
import app.yap.feature.scenario.data.mapper.toDb
import app.yap.feature.scenario.data.mapper.toDomain
import app.yap.feature.scenario.data.remote.ScenarioRemoteDataSource
import app.yap.feature.scenario.domain.repository.ActivationResult
import app.yap.feature.scenario.domain.repository.ScenarioRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

internal class DefaultScenarioRepository(
    private val observeAuthSessionStateUseCase: ObserveAuthSessionStateUseCase,
    private val remoteDataSource: ScenarioRemoteDataSource,
    private val scenarioDao: ScenarioDao,
    scope: CoroutineScope,
) : ScenarioRepository {

    init {
        scope.launch {
            observeAuthSessionStateUseCase()
                .filterIsInstance<AuthSessionState.LoggedOut>()
                .collect { scenarioDao.clear() }
        }
    }

    override fun observe(): Flow<ScenarioOverview?> =
        combine(scenarioDao.observeOverview(), scenarioDao.observeScenarios()) { overview, scenarios ->
            overview?.takeIf { stored -> stored.accountId == currentAccountId() }?.toDomain(scenarios)
        }

    override suspend fun get(forceUpdate: Boolean): ScenarioOverview? {
        if (forceUpdate) {
            when (val result = remoteDataSource.state()) {
                is ApiResult.Success -> save(result.value)
                is ApiResult.Failure -> Unit
            }
        }
        return observe().first()
    }

    override suspend fun activate(id: ScenarioId): ActivationResult = mutate { remoteDataSource.activate(id.value) }

    override suspend fun repeat(id: ScenarioId): ActivationResult = mutate { remoteDataSource.repeat(id.value) }

    private suspend fun mutate(call: suspend () -> ApiResult<ScenarioStateDto>): ActivationResult = when (val result = call()) {
        is ApiResult.Success -> {
            save(result.value)
            ActivationResult.Opened
        }
        is ApiResult.Failure -> result.error.toActivationResult()
    }

    private suspend fun save(state: ScenarioStateDto) {
        val accountId = currentAccountId() ?: return
        scenarioDao.replace(
            overview = state.toDb(accountId),
            scenarios = state.scenarios.map { scenario -> scenario.toDb() },
            objectives = state.scenarios.flatMap { scenario -> scenario.objectives.map { it.toDb(scenario.id) } },
        )
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
