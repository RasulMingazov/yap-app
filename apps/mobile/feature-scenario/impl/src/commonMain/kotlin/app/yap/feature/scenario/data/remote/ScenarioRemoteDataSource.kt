package app.yap.feature.scenario.data.remote

import app.yap.contract.scenario.ScenarioStateDto
import app.yap.core.network.ApiClient
import app.yap.core.network.ApiResult
import app.yap.core.network.get
import app.yap.core.network.post

private const val SCENARIOS_PATH = "/v1/scenarios"

internal interface ScenarioRemoteDataSource {

    suspend fun state(): ApiResult<ScenarioStateDto>

    suspend fun activate(scenarioId: String): ApiResult<ScenarioStateDto>

    suspend fun repeat(scenarioId: String): ApiResult<ScenarioStateDto>
}

internal class DefaultScenarioRemoteDataSource(private val apiClient: ApiClient) : ScenarioRemoteDataSource {

    override suspend fun state(): ApiResult<ScenarioStateDto> = apiClient.get("$SCENARIOS_PATH/state")

    override suspend fun activate(scenarioId: String): ApiResult<ScenarioStateDto> =
        apiClient.post("$SCENARIOS_PATH/$scenarioId/activate")

    override suspend fun repeat(scenarioId: String): ApiResult<ScenarioStateDto> =
        apiClient.post("$SCENARIOS_PATH/$scenarioId/repeat")
}
