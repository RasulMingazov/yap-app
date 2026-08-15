package app.yap.core.network

import app.yap.contract.common.ApiErrorCode
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.request.HttpRequestData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable

internal class ApiClientTest {

    @Test
    fun `GIVEN the server answers WHEN a payload is requested THEN it is returned as a success`() = runTest {
        val env = Environment(outcome = ServerOutcome.Payload)

        val result = env.apiClient.get<PayloadDto>(PATH)

        assertEquals(expected = ApiResult.Success(PayloadDto(value = VALUE)), actual = result)
    }

    @Test
    fun `GIVEN a path WHEN a request is sent THEN it is appended to the base url`() = runTest {
        val env = Environment(outcome = ServerOutcome.Payload)

        env.apiClient.get<PayloadDto>(PATH)

        assertEquals(expected = PATH, actual = env.requested().url.encodedPath)
    }

    @Test
    fun `GIVEN a refusal carrying an error code WHEN it arrives THEN the code reaches the caller`() = runTest {
        val env = Environment(outcome = ServerOutcome.BadRequest)

        val result = env.apiClient.get<PayloadDto>(PATH)

        assertEquals(
            expected = ApiResult.Failure(ApiError.Rejected(code = ApiErrorCode.INVALID_REQUEST)),
            actual = result,
        )
    }

    @Test
    fun `GIVEN a refusal without a readable body WHEN it arrives THEN the failure carries no code`() = runTest {
        val env = Environment(outcome = ServerOutcome.BadRequestWithoutBody)

        val result = env.apiClient.get<PayloadDto>(PATH)

        assertEquals(expected = ApiResult.Failure(ApiError.Rejected(code = null)), actual = result)
    }

    @Test
    fun `GIVEN the credentials are refused WHEN a request is sent THEN it is reported as unauthorized`() = runTest {
        val env = Environment(outcome = ServerOutcome.Unauthorized)

        val result = env.apiClient.get<PayloadDto>(PATH)

        assertEquals(expected = ApiResult.Failure(ApiError.Unauthorized), actual = result)
    }

    @Test
    fun `GIVEN the server fails on its own side WHEN a request is sent THEN it is reported as unavailable`() =
        runTest {
            val env = Environment(outcome = ServerOutcome.ServerError)

            val result = env.apiClient.get<PayloadDto>(PATH)

            assertEquals(expected = ApiResult.Failure(ApiError.Unavailable), actual = result)
        }

    @Test
    fun `GIVEN there is no network WHEN a request is sent THEN it is reported as unavailable`() = runTest {
        val env = Environment(outcome = ServerOutcome.NoNetwork)

        val result = env.apiClient.get<PayloadDto>(PATH)

        assertEquals(expected = ApiResult.Failure(ApiError.Unavailable), actual = result)
    }

    @Test
    fun `GIVEN a success the caller cannot read WHEN it arrives THEN it is reported as malformed`() = runTest {
        val env = Environment(outcome = ServerOutcome.UnreadablePayload)

        val result = env.apiClient.get<PayloadDto>(PATH)

        assertEquals(expected = ApiResult.Failure(ApiError.Malformed), actual = result)
    }

    @Test
    fun `GIVEN an authenticated request WHEN it is sent THEN it carries the bearer token`() = runTest {
        val env = Environment(outcome = ServerOutcome.Payload, accessToken = ACCESS_TOKEN)

        env.apiClient.get<PayloadDto>(PATH, authenticated = true)

        assertEquals(
            expected = "Bearer $ACCESS_TOKEN",
            actual = env.requested().headers[HttpHeaders.Authorization],
        )
    }

    @Test
    fun `GIVEN an unauthenticated request WHEN it is sent THEN it carries no token`() = runTest {
        val env = Environment(outcome = ServerOutcome.Payload, accessToken = ACCESS_TOKEN)

        env.apiClient.get<PayloadDto>(PATH, authenticated = false)

        assertNull(actual = env.requested().headers[HttpHeaders.Authorization])
    }

    @Test
    fun `GIVEN an endpoint answering without a payload WHEN it is called THEN the success carries no value`() =
        runTest {
            val env = Environment(outcome = ServerOutcome.NoContent)

            val result = env.apiClient.send(HttpMethod.Delete, PATH)

            assertEquals(expected = ApiResult.Success(Unit), actual = result)
        }

    private enum class ServerOutcome {
        Payload,
        UnreadablePayload,
        NoContent,
        BadRequest,
        BadRequestWithoutBody,
        Unauthorized,
        ServerError,
        NoNetwork,
    }

    private class Environment(
        outcome: ServerOutcome,
        accessToken: String? = null,
    ) {

        private val requests = mutableListOf<HttpRequestData>()

        private val engine = MockEngine { request ->
            requests += request
            when (outcome) {
                ServerOutcome.Payload -> respond(content = PAYLOAD_JSON, headers = jsonHeaders())
                ServerOutcome.UnreadablePayload -> respond(content = "not json", headers = jsonHeaders())
                ServerOutcome.NoContent -> respond(content = "", status = HttpStatusCode.NoContent)
                ServerOutcome.BadRequest -> respond(
                    content = ERROR_JSON,
                    status = HttpStatusCode.BadRequest,
                    headers = jsonHeaders(),
                )

                ServerOutcome.BadRequestWithoutBody -> respondError(HttpStatusCode.BadRequest)
                ServerOutcome.Unauthorized -> respondError(HttpStatusCode.Unauthorized)
                ServerOutcome.ServerError -> respondError(HttpStatusCode.InternalServerError)
                ServerOutcome.NoNetwork -> error("no network")
            }
        }

        val apiClient: ApiClient = ApiClient(
            createNetworkClient(baseUrl = BASE_URL, engine = engine, timeouts = null).apply {
                if (accessToken != null) installAccessTokenModifier { accessToken }
            },
        )

        fun requested(): HttpRequestData = requests.single()

        private fun jsonHeaders() =
            headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
    }

    @Serializable
    private data class PayloadDto(val value: String)

    private companion object {
        const val BASE_URL = "https://api.example.com"
        const val PATH = "/v1/probe"
        const val VALUE = "payload"
        const val ACCESS_TOKEN = "access-token"
        val PAYLOAD_JSON = """{"value":"$VALUE"}"""
        val ERROR_JSON = """{"error":"${ApiErrorCode.INVALID_REQUEST}"}"""
    }
}
