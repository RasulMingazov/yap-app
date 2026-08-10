package app.yap.server

import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals

class HealthRouteTest {

    @Test
    fun `GIVEN a reachable database WHEN requesting health THEN the instance reports itself healthy`() =
        testApplication {
            application { routing { healthRoute(isDatabaseReady = { true }) } }

            val response = client.get(HEALTH_PATH)

            assertEquals(expected = HttpStatusCode.OK, actual = response.status)
        }

    @Test
    fun `GIVEN an unreachable database WHEN requesting health THEN the instance reports itself unavailable`() =
        testApplication {
            application { routing { healthRoute(isDatabaseReady = { false }) } }

            val response = client.get(HEALTH_PATH)

            assertEquals(expected = HttpStatusCode.ServiceUnavailable, actual = response.status)
        }

    private companion object {
        const val HEALTH_PATH = "/health"
    }
}
