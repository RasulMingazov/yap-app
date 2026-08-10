package app.yap.server

import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals

class SharedPluginsTest {

    @Test
    fun `GIVEN a route failing unexpectedly WHEN it is called THEN the response is a bare server error`() =
        testApplication {
            application {
                installSharedPlugins()
                routing { get(FAILING_PATH) { error("rejected credential $CREDENTIAL") } }
            }

            val response = client.get(FAILING_PATH)

            assertEquals(expected = HttpStatusCode.InternalServerError, actual = response.status)
            assertEquals(expected = "Internal server error", actual = response.bodyAsText())
        }

    private companion object {
        const val CREDENTIAL = "ysr_33333333-3333-3333-3333-333333333333.refresh-secret"
        const val FAILING_PATH = "/failing"
    }
}
