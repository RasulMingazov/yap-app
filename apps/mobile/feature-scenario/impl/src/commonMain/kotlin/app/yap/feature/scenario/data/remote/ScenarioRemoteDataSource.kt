package app.yap.feature.scenario.data.remote

import app.yap.contract.scenario.ScenarioStateDto
import app.yap.core.network.ApiClient
import app.yap.core.network.ApiResult
import app.yap.core.network.get
import app.yap.core.network.post
import io.ktor.client.request.parameter

private const val SCENARIOS_PATH = "/v1/scenarios"

internal interface ScenarioRemoteDataSource {

    suspend fun state(todayIsoDate: String): ApiResult<ScenarioStateDto>

    suspend fun activate(scenarioId: String, todayIsoDate: String): ApiResult<ScenarioStateDto>

    suspend fun repeat(scenarioId: String, todayIsoDate: String): ApiResult<ScenarioStateDto>
}

internal class DefaultScenarioRemoteDataSource(private val apiClient: ApiClient) : ScenarioRemoteDataSource {

    override suspend fun state(todayIsoDate: String): ApiResult<ScenarioStateDto> =
        apiClient.get("$SCENARIOS_PATH/state") { parameter("today", todayIsoDate) }

    override suspend fun activate(scenarioId: String, todayIsoDate: String): ApiResult<ScenarioStateDto> =
        apiClient.post("$SCENARIOS_PATH/$scenarioId/activate") { parameter("today", todayIsoDate) }

    override suspend fun repeat(scenarioId: String, todayIsoDate: String): ApiResult<ScenarioStateDto> =
        apiClient.post("$SCENARIOS_PATH/$scenarioId/repeat") { parameter("today", todayIsoDate) }
}
