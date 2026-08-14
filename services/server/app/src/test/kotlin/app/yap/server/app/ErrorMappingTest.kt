package app.yap.server.app

import app.yap.server.feature.auth.model.AuthFailure
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

internal class ErrorMappingTest {

    @Test
    fun `GIVEN a route raising malformed input WHEN it is called THEN the response is 400`() = testApplication {
        probeApplication()

        val response = client.get("/probe/malformed")

        assertEquals(expected = HttpStatusCode.BadRequest, actual = response.status)
        assertEquals(expected = """{"error":"invalid_request"}""", actual = response.bodyAsText())
    }

    @Test
    fun `GIVEN a route raising an unverifiable confirmation WHEN it is called THEN the response is 401`() =
        testApplication {
            probeApplication()

            val response = client.get("/probe/unverifiable")

            assertEquals(expected = HttpStatusCode.Unauthorized, actual = response.status)
            assertEquals(expected = """{"error":"unauthorized"}""", actual = response.bodyAsText())
        }

    @Test
    fun `GIVEN a route raising provider unavailable WHEN it is called THEN the response is 503`() = testApplication {
        probeApplication()

        val response = client.get("/probe/unavailable")

        assertEquals(expected = HttpStatusCode.ServiceUnavailable, actual = response.status)
        assertEquals(expected = """{"error":"provider_unavailable"}""", actual = response.bodyAsText())
    }

    @Test
    fun `GIVEN a failure carrying provider detail WHEN it is mapped THEN the response exposes none of it`() =
        testApplication {
            probeApplication()

            val body = client.get("/probe/unavailable").bodyAsText()

            assertFalse(actual = body.contains(PROVIDER_DETAIL))
            assertFalse(actual = body.contains("google", ignoreCase = true))
        }

    private fun ApplicationTestBuilder.probeApplication() = application {
        install(ContentNegotiation) { json() }
        installErrorMapping()

        routing {
            get("/probe/malformed") { throw AuthFailure.MalformedInput(PROVIDER_DETAIL) }
            get("/probe/unverifiable") { throw AuthFailure.UnverifiableConfirmation(PROVIDER_DETAIL) }
            get("/probe/unavailable") { throw AuthFailure.ProviderUnavailable(PROVIDER_DETAIL) }
        }
    }

    private companion object {
        const val PROVIDER_DETAIL =
            "google jwks endpoint https://www.googleapis.com/oauth2/v3/certs timed out"
    }
}
