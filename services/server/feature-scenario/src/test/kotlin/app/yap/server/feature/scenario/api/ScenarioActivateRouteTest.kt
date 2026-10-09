package app.yap.server.feature.scenario.api

import app.yap.contract.scenario.ScenarioStateDto
import app.yap.server.feature.scenario.access.AccessPolicy
import app.yap.server.feature.scenario.model.ScenarioFailure
import app.yap.server.feature.scenario.persistence.PostgresTestSupport
import app.yap.server.feature.scenario.persistence.PostgresTestSupport.insertUser
import app.yap.server.feature.scenario.persistence.ScenarioRepository
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import java.time.Instant
import java.util.concurrent.CountDownLatch
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.Database
import org.junit.jupiter.api.Assumptions.assumeTrue

private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

private val FIVE_SCENARIOS = listOf("small-talk", "taxi-transport", "pharmacy-doctor", "hotel-checkin", "job-interview")

internal class ScenarioActivateRouteTest {

    @Test
    fun `GIVEN a free slot WHEN the free scenario is activated THEN it occupies a slot and the aggregate returns`() =
        withScenarioApplication { source ->
            val userId = source.newUser()

            val response = client.post("/v1/scenarios/cafe-visit/activate") {
                header(HttpHeaders.Authorization, ScenarioTestTokens.bearer(userId))
            }

            assertEquals(expected = HttpStatusCode.OK, actual = response.status)
            val state = json.decodeFromString<ScenarioStateDto>(response.bodyAsText())
            assertEquals(expected = 1, actual = state.slotsUsed)
            assertEquals(
                expected = "active",
                actual = state.scenarios.single { it.id == "cafe-visit" }.status,
            )
        }

    @Test
    fun `GIVEN an already active scenario WHEN it is activated again THEN nothing changes and the call succeeds`() =
        withScenarioApplication { source ->
            val userId = source.newUser()
            client.post("/v1/scenarios/cafe-visit/activate") {
                header(HttpHeaders.Authorization, ScenarioTestTokens.bearer(userId))
            }

            val response = client.post("/v1/scenarios/cafe-visit/activate") {
                header(HttpHeaders.Authorization, ScenarioTestTokens.bearer(userId))
            }

            assertEquals(expected = HttpStatusCode.OK, actual = response.status)
            val state = json.decodeFromString<ScenarioStateDto>(response.bodyAsText())
            assertEquals(expected = 1, actual = state.slotsUsed)
        }

    @Test
    fun `GIVEN an unknown scenario WHEN it is activated THEN it is not found`() =
        withScenarioApplication { source ->
            val userId = source.newUser()

            val response = client.post("/v1/scenarios/nope/activate") {
                header(HttpHeaders.Authorization, ScenarioTestTokens.bearer(userId))
            }

            assertEquals(expected = HttpStatusCode.NotFound, actual = response.status)
        }

    @Test
    fun `GIVEN no access WHEN a locked scenario is activated THEN access is required and nothing is written`() =
        withScenarioApplication { source ->
            val userId = source.newUser()

            val response = client.post("/v1/scenarios/job-interview/activate") {
                header(HttpHeaders.Authorization, ScenarioTestTokens.bearer(userId))
            }

            assertEquals(expected = HttpStatusCode.Forbidden, actual = response.status)
            assertTrue(response.bodyAsText().contains("access_required"))
        }

    @Test
    fun `GIVEN five active scenarios WHEN a sixth is activated THEN the slot limit refuses it`() =
        withScenarioApplication(accessPolicy = AccessPolicy { true }) { source ->
            val userId = source.newUser()
            client.post("/v1/scenarios/cafe-visit/activate") {
                header(HttpHeaders.Authorization, ScenarioTestTokens.bearer(userId))
            }
            FIVE_SCENARIOS.dropLast(1).forEach { scenarioId ->
                client.post("/v1/scenarios/$scenarioId/activate") {
                    header(HttpHeaders.Authorization, ScenarioTestTokens.bearer(userId))
                }
            }

            val response = client.post("/v1/scenarios/${FIVE_SCENARIOS.last()}/activate") {
                header(HttpHeaders.Authorization, ScenarioTestTokens.bearer(userId))
            }

            assertEquals(expected = HttpStatusCode.Conflict, actual = response.status)
            assertTrue(response.bodyAsText().contains("slot_limit_reached"))
        }

    @Test
    fun `GIVEN four active scenarios WHEN two activations race for the last slot THEN exactly one is refused`() {
        assumeTrue(PostgresTestSupport.isDockerAvailable, "Docker is unavailable — integration suite not run")

        PostgresTestSupport.withDatabase { source ->
            PostgresTestSupport.migrate(source)
            Database.connect(source)
            val repository = ScenarioRepository()
            val userId = source.insertUser()
            val now = Instant.now()
            repository.activate(userId = userId, scenarioId = "cafe-visit", hasAccess = true, now = now)
            FIVE_SCENARIOS.take(3).forEach { scenarioId ->
                repository.activate(userId = userId, scenarioId = scenarioId, hasAccess = true, now = now)
            }

            val barrier = CyclicBarrier(2)
            val refused = AtomicInteger()
            val done = CountDownLatch(2)
            listOf("hotel-checkin", "job-interview").forEach { scenarioId ->
                thread {
                    barrier.await()
                    try {
                        repository.activate(userId = userId, scenarioId = scenarioId, hasAccess = true, now = now)
                    } catch (_: ScenarioFailure.SlotLimitReached) {
                        refused.incrementAndGet()
                    } finally {
                        done.countDown()
                    }
                }
            }
            done.await()

            assertEquals(expected = 1, actual = refused.get())
        }
    }
}
