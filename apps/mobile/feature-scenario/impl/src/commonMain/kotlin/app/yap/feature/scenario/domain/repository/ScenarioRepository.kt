package app.yap.feature.scenario.domain.repository

import app.yap.feature.scenario.api.entity.ScenarioId
import app.yap.feature.scenario.api.entity.ScenarioOverview
import kotlinx.coroutines.flow.Flow

internal interface ScenarioRepository {

    fun observe(): Flow<ScenarioOverview?>

    suspend fun get(forceUpdate: Boolean): ScenarioOverview?

    suspend fun activate(id: ScenarioId): ActivationResult

    suspend fun repeat(id: ScenarioId): ActivationResult
}
