package app.yap.server.feature.auth.identity

import app.yap.server.feature.auth.StubAuthChallenge
import app.yap.server.feature.auth.StubLoginCredential
import app.yap.server.feature.auth.model.AuthFailure
import app.yap.server.feature.auth.model.AuthFailureException
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.net.http.HttpClient
import java.time.Duration
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.test.runTest
import kotlin.time.Duration as KotlinDuration

class HttpGoogleTokenExchangeTest {

    @Test
    fun `GIVEN the endpoint returns an identity token WHEN exchanging THEN it is returned`() = runTest {
        Environment().use { env ->
            val result = env.exchange()

            assertEquals(expected = StubLoginCredential.ID_TOKEN, actual = result)
        }
    }

    @Test
    fun `GIVEN an authorization code WHEN exchanging THEN the request carries the PKCE form fields`() = runTest {
        Environment().use { env ->
            env.exchange()

            assertEquals(
                expected = "client_id=${StubGoogleIdToken.ANDROID_CLIENT_ID}" +
                    "&code=${StubLoginCredential.AUTHORIZATION_CODE}" +
                    "&code_verifier=${StubAuthChallenge.CODE_VERIFIER}" +
                    "&grant_type=authorization_code" +
                    "&redirect_uri=app.yap.oauth%3A%2Fredirect",
                actual = env.requestBody,
            )
        }
    }

    @Test
    fun `GIVEN values that need escaping WHEN exchanging THEN they are url encoded`() = runTest {
        Environment().use { env ->
            env.exchange(code = "code/with+reserved=characters")

            assertEquals(
                expected = "client_id=${StubGoogleIdToken.ANDROID_CLIENT_ID}" +
                    "&code=code%2Fwith%2Breserved%3Dcharacters" +
                    "&code_verifier=${StubAuthChallenge.CODE_VERIFIER}" +
                    "&grant_type=authorization_code" +
                    "&redirect_uri=app.yap.oauth%3A%2Fredirect",
                actual = env.requestBody,
            )
        }
    }

    @Test
    fun `GIVEN the endpoint rejects the grant WHEN exchanging THEN it fails as challenge invalid`() = runTest {
        Environment(body = INVALID_GRANT_BODY, statusCode = HTTP_BAD_REQUEST).use { env ->
            val failure = assertFailsWith<AuthFailureException> { env.exchange() }

            assertEquals(expected = AuthFailure.ChallengeInvalid, actual = failure.failure)
        }
    }

    @Test
    fun `GIVEN the endpoint rejects our client WHEN exchanging THEN it fails as provider unavailable`() = runTest {
        Environment(body = INVALID_CLIENT_BODY, statusCode = HTTP_BAD_REQUEST).use { env ->
            val failure = assertFailsWith<AuthFailureException> { env.exchange() }

            assertEquals(expected = AuthFailure.ProviderUnavailable, actual = failure.failure)
        }
    }

    @Test
    fun `GIVEN the endpoint rate limits the request WHEN exchanging THEN it fails as provider unavailable`() =
        runTest {
            Environment(body = RATE_LIMIT_BODY, statusCode = HTTP_TOO_MANY_REQUESTS).use { env ->
                val failure = assertFailsWith<AuthFailureException> { env.exchange() }

                assertEquals(expected = AuthFailure.ProviderUnavailable, actual = failure.failure)
            }
        }

    @Test
    fun `GIVEN the endpoint is failing WHEN exchanging THEN it fails as provider unavailable`() = runTest {
        Environment(body = SERVER_ERROR_BODY, statusCode = HTTP_SERVICE_UNAVAILABLE).use { env ->
            val failure = assertFailsWith<AuthFailureException> { env.exchange() }

            assertEquals(expected = AuthFailure.ProviderUnavailable, actual = failure.failure)
        }
    }

    @Test
    fun `GIVEN the endpoint is unreachable WHEN exchanging THEN it fails as provider unavailable`() = runTest {
        val closedEndpoint = Environment().also(Environment::close)

        val failure = assertFailsWith<AuthFailureException> { closedEndpoint.exchange() }

        assertEquals(expected = AuthFailure.ProviderUnavailable, actual = failure.failure)
    }

    @Test
    fun `GIVEN a malformed success response WHEN exchanging THEN it fails as provider unavailable`() = runTest {
        Environment(body = MALFORMED_BODY).use { env ->
            val failure = assertFailsWith<AuthFailureException> { env.exchange() }

            assertEquals(expected = AuthFailure.ProviderUnavailable, actual = failure.failure)
        }
    }

    @Test
    fun `GIVEN a success response without an identity token WHEN exchanging THEN it is provider unavailable`() =
        runTest {
            Environment(body = NO_IDENTITY_TOKEN_BODY).use { env ->
                val failure = assertFailsWith<AuthFailureException> { env.exchange() }

                assertEquals(expected = AuthFailure.ProviderUnavailable, actual = failure.failure)
            }
        }

    @Test
    fun `GIVEN a request in flight WHEN the caller is cancelled THEN the exchange stops with it`() =
        runTest(timeout = CANCELLATION_TIMEOUT) {
            Environment(isHanging = true).use { env ->
                val exchange = async(Dispatchers.IO) { env.exchange() }
                env.awaitRequestReceived()

                exchange.cancelAndJoin()

                assertFalse(env.isResponseSent)
            }
        }

    private companion object {
        const val HTTP_OK = 200
        const val HTTP_BAD_REQUEST = 400
        const val HTTP_TOO_MANY_REQUESTS = 429
        const val HTTP_SERVICE_UNAVAILABLE = 503

        const val INVALID_CLIENT_BODY = """{"error":"invalid_client"}"""
        const val INVALID_GRANT_BODY = """{"error":"invalid_grant","error_description":"Bad Request"}"""
        const val MALFORMED_BODY = "<html>not json</html>"
        const val NO_IDENTITY_TOKEN_BODY = """{"access_token":"provider-access-token"}"""
        const val RATE_LIMIT_BODY = """{"error":"rate_limit_exceeded"}"""
        const val SERVER_ERROR_BODY = "<html>service unavailable</html>"
        const val SUCCESS_BODY =
            """{"id_token":"${StubLoginCredential.ID_TOKEN}","access_token":"provider-access-token"}"""

        val CANCELLATION_TIMEOUT: KotlinDuration = 10.seconds
        val CONNECT_TIMEOUT: Duration = Duration.ofSeconds(5)
    }

    private class Environment(
        body: String = SUCCESS_BODY,
        isHanging: Boolean = false,
        statusCode: Int = HTTP_OK,
    ) : AutoCloseable {

        private val endpoint = FakeTokenEndpoint(
            body = body,
            isHanging = isHanging,
            statusCode = statusCode,
        )

        private val tokenExchange = HttpGoogleTokenExchange(
            clientId = StubGoogleIdToken.ANDROID_CLIENT_ID,
            httpClient = HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build(),
            tokenEndpoint = endpoint.url,
        )

        val isResponseSent: Boolean get() = endpoint.isResponseSent

        val requestBody: String get() = endpoint.requestBody

        fun awaitRequestReceived() = endpoint.awaitRequestReceived()

        suspend fun exchange(
            code: String = StubLoginCredential.AUTHORIZATION_CODE,
            codeVerifier: String = StubAuthChallenge.CODE_VERIFIER,
            redirectUri: String = StubLoginCredential.REDIRECT_URI,
        ): String = tokenExchange.exchange(
            code = code,
            codeVerifier = codeVerifier,
            redirectUri = redirectUri,
        )

        override fun close() = endpoint.close()
    }

    /**
     * A local stand-in for the provider token endpoint, so the real HTTP client is exercised. When
     * [isHanging] is set it accepts the request and never answers until it is closed, which is how
     * a caller can be cancelled while its request is genuinely in flight.
     */
    private class FakeTokenEndpoint(
        body: String,
        private val isHanging: Boolean,
        statusCode: Int,
    ) : AutoCloseable {

        private val server: HttpServer = HttpServer.create(InetSocketAddress(LOOPBACK, ANY_PORT), 0)
        private val requestReceived = CountDownLatch(1)
        private val released = CountDownLatch(1)

        var isResponseSent: Boolean = false
            private set

        var requestBody: String = ""
            private set

        val url: String get() = "http://$LOOPBACK:${server.address.port}$PATH"

        init {
            server.createContext(PATH) { exchange ->
                requestBody = exchange.requestBody.readBytes().decodeToString()
                requestReceived.countDown()
                if (isHanging) released.await()

                val payload = body.toByteArray()
                exchange.sendResponseHeaders(statusCode, payload.size.toLong())
                exchange.responseBody.use { stream -> stream.write(payload) }
                isResponseSent = true
            }
            server.start()
        }

        fun awaitRequestReceived() {
            check(requestReceived.await(AWAIT_SECONDS, TimeUnit.SECONDS)) { "No request arrived" }
        }

        override fun close() {
            released.countDown()
            server.stop(0)
        }

        private companion object {
            const val ANY_PORT = 0
            const val AWAIT_SECONDS = 10L
            const val LOOPBACK = "127.0.0.1"
            const val PATH = "/token"
        }
    }
}
