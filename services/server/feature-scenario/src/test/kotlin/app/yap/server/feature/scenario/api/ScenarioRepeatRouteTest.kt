package app.yap.server.feature.scenario.api

import app.yap.contract.scenario.ReportProgressRequestDto
import app.yap.contract.scenario.ScenarioStateDto
import app.yap.server.feature.scenario.access.AccessPolicy
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }
private const val TODAY = "2026-10-09"
private const val FREE_SCENARIO = "cafe-visit"
private const val FREE_OBJECTIVES = 6
private val OTHER_SCENARIOS = listOf("small-talk", "taxi-transport", "pharmacy-doctor", "hotel-checkin", "job-interview")

internal class ScenarioRepeatRouteTest {

    @Test
    fun `GIVEN a completed scenario WHEN a repeat is confirmed THEN a fresh attempt starts and history is kept`() =
        withScenarioApplication { source ->
            val userId = source.newUser()
            val bearer = ScenarioTestTokens.bearer(userId)
            client.complete(bearer)

            val response = client.post("/v1/scenarios/$FREE_SCENARIO/repeat?today=$TODAY") {
                header(HttpHeaders.Authorization, bearer)
            }

            assertEquals(expected = HttpStatusCode.OK, actual = response.status)
            val state = json.decodeFromString<ScenarioStateDto>(response.bodyAsText())
            val scenario = state.scenarios.single { it.id == FREE_SCENARIO }
            assertEquals(expected = "active", actual = scenario.status)
            assertEquals(expected = 2, actual = scenario.attempt)
            assertEquals(expected = 1, actual = scenario.currentObjective)
            assertEquals(expected = FREE_OBJECTIVES * 60L, actual = state.practiceSeconds)
            assertEquals(expected = 1, actual = state.streakDays)
            assertEquals(expected = listOf(TODAY), actual = state.practisedDates)
        }

    @Test
    fun `GIVEN a scenario that is not completed WHEN a repeat is asked for THEN it is refused`() =
        withScenarioApplication { source ->
            val userId = source.newUser()
            val bearer = ScenarioTestTokens.bearer(userId)
            client.post("/v1/scenarios/$FREE_SCENARIO/activate") {
                header(HttpHeaders.Authorization, bearer)
            }

            val response = client.post("/v1/scenarios/$FREE_SCENARIO/repeat") {
                header(HttpHeaders.Authorization, bearer)
            }

            assertEquals(expected = HttpStatusCode.Conflict, actual = response.status)
            assertTrue(response.bodyAsText().contains("invalid_request"))
        }

    @Test
    fun `GIVEN five occupied slots WHEN a completed scenario is repeated THEN the slot limit refuses it`() =
        withScenarioApplication(accessPolicy = AccessPolicy { true }) { source ->
            val userId = source.newUser()
            val bearer = ScenarioTestTokens.bearer(userId)
            client.complete(bearer)
            OTHER_SCENARIOS.forEach { scenarioId ->
                client.post("/v1/scenarios/$scenarioId/activate") {
                    header(HttpHeaders.Authorization, bearer)
                }
            }

            val response = client.post("/v1/scenarios/$FREE_SCENARIO/repeat") {
                header(HttpHeaders.Authorization, bearer)
            }

            assertEquals(expected = HttpStatusCode.Conflict, actual = response.status)
            assertTrue(response.bodyAsText().contains("slot_limit_reached"))
        }

    @Test
    fun `GIVEN access lapsed WHEN a completed paid scenario is repeated THEN access is required`() {
        var hasAccess = true
        withScenarioApplication(accessPolicy = AccessPolicy { hasAccess }) { source ->
            val userId = source.newUser()
            val bearer = ScenarioTestTokens.bearer(userId)
            val scenarioId = "small-talk"
            client.post("/v1/scenarios/$scenarioId/activate") {
                header(HttpHeaders.Authorization, bearer)
            }
            (1..5).forEach { objective ->
                client.progress(bearer, scenarioId, objective)
            }

            hasAccess = false
            val response = client.post("/v1/scenarios/$scenarioId/repeat") {
                header(HttpHeaders.Authorization, bearer)
            }

            assertEquals(expected = HttpStatusCode.Forbidden, actual = response.status)
            assertTrue(response.bodyAsText().contains("access_required"))
        }
    }

    private suspend fun HttpClient.complete(bearer: String) {
        post("/v1/scenarios/$FREE_SCENARIO/activate?today=$TODAY") {
            header(HttpHeaders.Authorization, bearer)
        }
        (1..FREE_OBJECTIVES).forEach { objective ->
            progress(bearer, FREE_SCENARIO, objective)
        }
    }

    private suspend fun HttpClient.progress(bearer: String, scenarioId: String, objective: Int): HttpResponse =
        post("/v1/scenarios/$scenarioId/progress") {
            header(HttpHeaders.Authorization, bearer)
            contentType(ContentType.Application.Json)
            setBody(
                json.encodeToString(
                    ReportProgressRequestDto.serializer(),
                    ReportProgressRequestDto(
                        reportId = UUID.randomUUID().toString(),
                        attempt = 1,
                        achievedObjective = objective,
                        localDate = TODAY,
                        elapsedSeconds = 60L,
                    ),
                ),
            )
        }
}
