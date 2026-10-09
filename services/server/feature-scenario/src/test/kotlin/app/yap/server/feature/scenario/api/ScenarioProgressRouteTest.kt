package app.yap.server.feature.scenario.api

import app.yap.contract.scenario.ReportProgressRequestDto
import app.yap.contract.scenario.ScenarioStateDto
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
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }
private const val FREE_SCENARIO = "cafe-visit"
private const val FREE_OBJECTIVES = 6

internal class ScenarioProgressRouteTest {

    @Test
    fun `GIVEN an applied report WHEN the same reportId is replayed THEN nothing is applied twice`() =
        withScenarioApplication { source ->
            val userId = source.newUser()
            val bearer = ScenarioTestTokens.bearer(userId)
            client.activate(bearer)
            val report = report(reportId = UUID.randomUUID().toString(), achievedObjective = 1)
            client.progress(bearer, report)

            val replay = client.progress(bearer, report)

            assertEquals(expected = HttpStatusCode.OK, actual = replay.status)
            val state = json.decodeFromString<ScenarioStateDto>(replay.bodyAsText())
            assertEquals(expected = 180L, actual = state.practiceSeconds)
            assertEquals(
                expected = 2,
                actual = state.scenarios.single { it.id == FREE_SCENARIO }.currentObjective,
            )
        }

    @Test
    fun `GIVEN a time-only report WHEN it is applied THEN minutes grow and progress stands`() =
        withScenarioApplication { source ->
            val userId = source.newUser()
            val bearer = ScenarioTestTokens.bearer(userId)
            client.activate(bearer)

            val response = client.progress(bearer, report(achievedObjective = null))

            assertEquals(expected = HttpStatusCode.OK, actual = response.status)
            val state = json.decodeFromString<ScenarioStateDto>(response.bodyAsText())
            assertEquals(expected = 180L, actual = state.practiceSeconds)
            assertEquals(
                expected = 1,
                actual = state.scenarios.single { it.id == FREE_SCENARIO }.currentObjective,
            )
        }

    @Test
    fun `GIVEN a repeat elsewhere WHEN a report carries the previous attempt THEN it is rejected`() =
        withScenarioApplication { source ->
            val userId = source.newUser()
            val bearer = ScenarioTestTokens.bearer(userId)
            client.activate(bearer)

            val response = client.progress(bearer, report(attempt = 2, achievedObjective = 1))

            assertEquals(expected = HttpStatusCode.BadRequest, actual = response.status)
        }

    @Test
    fun `GIVEN objective one is current WHEN a later objective is reported THEN progress cannot skip ahead`() =
        withScenarioApplication { source ->
            val userId = source.newUser()
            val bearer = ScenarioTestTokens.bearer(userId)
            client.activate(bearer)

            val response = client.progress(bearer, report(achievedObjective = 3))

            assertEquals(expected = HttpStatusCode.BadRequest, actual = response.status)
        }

    @Test
    fun `GIVEN sequential achievements WHEN they are reported THEN the current objective only moves forward`() =
        withScenarioApplication { source ->
            val userId = source.newUser()
            val bearer = ScenarioTestTokens.bearer(userId)
            client.activate(bearer)
            client.progress(bearer, report(achievedObjective = 1))
            client.progress(bearer, report(achievedObjective = 2))

            val replayFirst = client.progress(bearer, report(achievedObjective = 1))

            assertEquals(expected = HttpStatusCode.OK, actual = replayFirst.status)
            val state = json.decodeFromString<ScenarioStateDto>(replayFirst.bodyAsText())
            assertEquals(
                expected = 3,
                actual = state.scenarios.single { it.id == FREE_SCENARIO }.currentObjective,
            )
        }

    @Test
    fun `GIVEN the last objective WHEN it is achieved THEN the scenario completes and its slot frees at once`() =
        withScenarioApplication { source ->
            val userId = source.newUser()
            val bearer = ScenarioTestTokens.bearer(userId)
            client.activate(bearer)
            (1 until FREE_OBJECTIVES).forEach { objective ->
                client.progress(bearer, report(achievedObjective = objective))
            }

            val response = client.progress(bearer, report(achievedObjective = FREE_OBJECTIVES))

            assertEquals(expected = HttpStatusCode.OK, actual = response.status)
            val state = json.decodeFromString<ScenarioStateDto>(response.bodyAsText())
            val scenario = state.scenarios.single { it.id == FREE_SCENARIO }
            assertEquals(expected = "completed", actual = scenario.status)
            assertEquals(expected = 0, actual = state.slotsUsed)
        }

    private suspend fun HttpClient.activate(bearer: String): HttpResponse =
        post("/v1/scenarios/$FREE_SCENARIO/activate") {
            header(HttpHeaders.Authorization, bearer)
        }

    private suspend fun HttpClient.progress(bearer: String, body: ReportProgressRequestDto): HttpResponse =
        post("/v1/scenarios/$FREE_SCENARIO/progress") {
            header(HttpHeaders.Authorization, bearer)
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(ReportProgressRequestDto.serializer(), body))
        }

    private fun report(
        reportId: String = UUID.randomUUID().toString(),
        attempt: Int = 1,
        achievedObjective: Int?,
        elapsedSeconds: Long = 180L,
    ): ReportProgressRequestDto = ReportProgressRequestDto(
        reportId = reportId,
        attempt = attempt,
        achievedObjective = achievedObjective,
        elapsedSeconds = elapsedSeconds,
    )
}
