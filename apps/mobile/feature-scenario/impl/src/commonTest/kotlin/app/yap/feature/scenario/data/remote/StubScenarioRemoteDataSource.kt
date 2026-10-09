package app.yap.feature.scenario.data.remote

import app.yap.contract.scenario.ScenarioStateDto
import app.yap.core.network.ApiResult
import app.yap.feature.scenario.data.StubScenarioState
import io.github.rasulmingazov.stubcall.StubCall1
import io.github.rasulmingazov.stubcall.StubCall2

internal class StubScenarioRemoteDataSource(
    state: ScenarioStateDto = StubScenarioState.stubStateDto(),
) : ScenarioRemoteDataSource {

    private val result: ApiResult<ScenarioStateDto> = ApiResult.Success(state)

    val stateCall = StubCall1.returns<String, ApiResult<ScenarioStateDto>>(result)
    val activateCall = StubCall2.returns<String, String, ApiResult<ScenarioStateDto>>(result)
    val repeatCall = StubCall2.returns<String, String, ApiResult<ScenarioStateDto>>(result)

    override suspend fun state(todayIsoDate: String): ApiResult<ScenarioStateDto> =
        stateCall.invoke(todayIsoDate)

    override suspend fun activate(scenarioId: String, todayIsoDate: String): ApiResult<ScenarioStateDto> =
        activateCall.invoke(scenarioId, todayIsoDate)

    override suspend fun repeat(scenarioId: String, todayIsoDate: String): ApiResult<ScenarioStateDto> =
        repeatCall.invoke(scenarioId, todayIsoDate)
}
