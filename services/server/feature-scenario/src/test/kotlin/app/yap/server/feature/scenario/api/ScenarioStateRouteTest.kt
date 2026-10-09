package app.yap.server.feature.scenario.api

import app.yap.contract.scenario.ScenarioStateDto
import app.yap.server.feature.scenario.access.AccessPolicy
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

internal class ScenarioStateRouteTest {

    @Test
    fun `GIVEN a fresh user WHEN state is requested THEN 20 scenarios with one free available and empty progress return`() =
        withScenarioApplication { source ->
            val userId = source.newUser()

            val response = client.get("/v1/scenarios/state?today=2026-10-09") {
                header(HttpHeaders.Authorization, ScenarioTestTokens.bearer(userId))
            }

            assertEquals(expected = HttpStatusCode.OK, actual = response.status)
            val state = json.decodeFromString<ScenarioStateDto>(response.bodyAsText())
            assertEquals(expected = 20, actual = state.scenarios.size)
            assertEquals(expected = 1, actual = state.scenarios.count { it.free && !it.locked && it.status == "available" })
            assertEquals(expected = 19, actual = state.scenarios.count { it.locked })
            assertEquals(expected = 0, actual = state.slotsUsed)
            assertEquals(expected = 5, actual = state.slotCapacity)
            assertEquals(expected = 0, actual = state.streakDays)
            assertEquals(expected = 0L, actual = state.practiceSeconds)
        }

    @Test
    fun `GIVEN a malformed today parameter WHEN state is requested THEN the request is rejected as invalid`() =
        withScenarioApplication { source ->
            val userId = source.newUser()

            val response = client.get("/v1/scenarios/state?today=not-a-date") {
                header(HttpHeaders.Authorization, ScenarioTestTokens.bearer(userId))
            }

            assertEquals(expected = HttpStatusCode.BadRequest, actual = response.status)
        }

    @Test
    fun `GIVEN access lapsed WHEN state is requested THEN active paid scenarios stay active and locked with their slot`() {
        var hasAccess = true
        withScenarioApplication(accessPolicy = AccessPolicy { hasAccess }) { source ->
            val userId = source.newUser()
            client.post("/v1/scenarios/job-interview/activate?today=2026-10-09") {
                header(HttpHeaders.Authorization, ScenarioTestTokens.bearer(userId))
            }

            hasAccess = false
            val response = client.get("/v1/scenarios/state?today=2026-10-09") {
                header(HttpHeaders.Authorization, ScenarioTestTokens.bearer(userId))
            }

            val state = json.decodeFromString<ScenarioStateDto>(response.bodyAsText())
            val scenario = state.scenarios.single { it.id == "job-interview" }
            assertEquals(expected = "active", actual = scenario.status)
            assertEquals(expected = true, actual = scenario.locked)
            assertEquals(expected = 1, actual = state.slotsUsed)
        }
    }

    @Test
    fun `GIVEN no bearer token WHEN state is requested THEN the request is unauthorized`() =
        withScenarioApplication {
            val response = client.get("/v1/scenarios/state?today=2026-10-09")

            assertEquals(expected = HttpStatusCode.Unauthorized, actual = response.status)
        }
}
