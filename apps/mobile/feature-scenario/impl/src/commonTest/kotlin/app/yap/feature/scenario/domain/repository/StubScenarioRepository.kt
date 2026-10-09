package app.yap.feature.scenario.domain.repository

import app.yap.feature.scenario.api.entity.OverviewState
import app.yap.feature.scenario.api.entity.ScenarioId
import io.github.rasulmingazov.stubcall.StubCall0
import io.github.rasulmingazov.stubcall.StubCall1
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

internal class StubScenarioRepository(
    state: OverviewState = OverviewState.Loading,
) : ScenarioRepository {

    val states = MutableStateFlow(state)

    val refreshCall = StubCall0.returns<Result<Unit>>(Result.success(Unit))
    val activateCall = StubCall1.returns<ScenarioId, ActivationResult>(ActivationResult.Opened)
    val repeatCall = StubCall1.returns<ScenarioId, ActivationResult>(ActivationResult.Opened)

    override val state: Flow<OverviewState> = states

    override suspend fun refresh(): Result<Unit> = refreshCall.invoke()

    override suspend fun activate(id: ScenarioId): ActivationResult = activateCall.invoke(id)

    override suspend fun repeat(id: ScenarioId): ActivationResult = repeatCall.invoke(id)
}
