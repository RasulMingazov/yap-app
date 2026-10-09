package app.yap.feature.scenario.domain.repository

import app.yap.feature.scenario.api.entity.ScenarioId
import app.yap.feature.scenario.api.entity.ScenarioOverview
import io.github.rasulmingazov.stubcall.StubCall1
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

internal class StubScenarioRepository(
    overview: ScenarioOverview? = null,
) : ScenarioRepository {

    val overviews = MutableStateFlow(overview)

    val getCall = StubCall1.returns<Boolean, ScenarioOverview?>(overview)
    val activateCall = StubCall1.returns<ScenarioId, ActivationResult>(ActivationResult.Opened)
    val repeatCall = StubCall1.returns<ScenarioId, ActivationResult>(ActivationResult.Opened)

    override fun observe(): Flow<ScenarioOverview?> = overviews

    override suspend fun get(forceUpdate: Boolean): ScenarioOverview? = getCall.invoke(forceUpdate)

    override suspend fun activate(id: ScenarioId): ActivationResult = activateCall.invoke(id)

    override suspend fun repeat(id: ScenarioId): ActivationResult = repeatCall.invoke(id)
}
