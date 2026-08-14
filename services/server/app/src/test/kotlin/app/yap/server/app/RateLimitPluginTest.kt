package app.yap.server.app

import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.ratelimit.rateLimit
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals

internal class RateLimitPluginTest {

    @Test
    fun `GIVEN a limit of 100 a minute WHEN one address sends 101 requests THEN the last is refused`() =
        testApplication {
            probeApplication()

            val statuses = List(LIMIT + 1) { client.get("/probe") { header(FORWARDED_FOR, FIRST_ADDRESS) }.status }

            assertEquals(expected = HttpStatusCode.OK, actual = statuses[LIMIT - 1])
            assertEquals(expected = HttpStatusCode.TooManyRequests, actual = statuses[LIMIT])
        }

    @Test
    fun `GIVEN one address is over the limit WHEN a second address sends a request THEN it is unaffected`() =
        testApplication {
            probeApplication()
            repeat(LIMIT + 1) { client.get("/probe") { header(FORWARDED_FOR, FIRST_ADDRESS) } }

            val response = client.get("/probe") { header(FORWARDED_FOR, SECOND_ADDRESS) }

            assertEquals(expected = HttpStatusCode.OK, actual = response.status)
        }

    private fun ApplicationTestBuilder.probeApplication() = application {
        installAuthRateLimit(requestsPerMinute = LIMIT, trustProxyHeaders = true)

        routing {
            rateLimit(AUTH_RATE_LIMIT_NAME) {
                get("/probe") { call.respondText("ok") }
            }
        }
    }

    private companion object {
        const val LIMIT = 100
        const val FORWARDED_FOR = "X-Forwarded-For"
        const val FIRST_ADDRESS = "203.0.113.1"
        const val SECOND_ADDRESS = "203.0.113.2"
    }
}
