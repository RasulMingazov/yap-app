package app.yap.feature.scenario.data.remote

import app.yap.contract.scenario.ScenarioStateDto
import app.yap.core.network.ApiResult
import app.yap.feature.scenario.data.StubScenarioState
import io.github.rasulmingazov.stubcall.StubCall0
import io.github.rasulmingazov.stubcall.StubCall1

internal class StubScenarioRemoteDataSource(
    state: ScenarioStateDto = StubScenarioState.stubStateDto(),
) : ScenarioRemoteDataSource {

    private val result: ApiResult<ScenarioStateDto> = ApiResult.Success(state)

    val stateCall = StubCall0.returns<ApiResult<ScenarioStateDto>>(result)
    val activateCall = StubCall1.returns<String, ApiResult<ScenarioStateDto>>(result)
    val repeatCall = StubCall1.returns<String, ApiResult<ScenarioStateDto>>(result)

    override suspend fun state(): ApiResult<ScenarioStateDto> = stateCall.invoke()

    override suspend fun activate(scenarioId: String): ApiResult<ScenarioStateDto> = activateCall.invoke(scenarioId)

    override suspend fun repeat(scenarioId: String): ApiResult<ScenarioStateDto> = repeatCall.invoke(scenarioId)
}
