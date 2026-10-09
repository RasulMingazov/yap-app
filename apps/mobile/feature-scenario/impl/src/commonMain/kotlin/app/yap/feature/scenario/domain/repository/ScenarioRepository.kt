package app.yap.feature.scenario.domain.repository

import app.yap.feature.scenario.api.entity.OverviewState
import app.yap.feature.scenario.api.entity.ScenarioId
import kotlinx.coroutines.flow.Flow

internal interface ScenarioRepository {

    val state: Flow<OverviewState>

    suspend fun refresh(): Result<Unit>

    suspend fun activate(id: ScenarioId): ActivationResult

    suspend fun repeat(id: ScenarioId): ActivationResult
}
