package app.yap.server.app

import app.yap.contract.auth.GoogleAuthorizationCodeDto
import app.yap.contract.auth.GoogleCredentialsDto
import app.yap.contract.auth.RefreshCredentialsDto
import app.yap.server.core.config.AppConfig
import app.yap.server.core.config.AuthConfig
import app.yap.server.core.config.DatabaseConfig
import app.yap.server.core.security.JwtTokenService
import app.yap.server.feature.auth.AuthFeature
import app.yap.server.feature.auth.identity.GoogleAuthConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assumptions.assumeTrue

internal class AuthEndpointsWiringTest {

    @Test
    fun `GIVEN the production graph WHEN a confirmation cannot be verified THEN the native door answers 401`() =
        testApplication {
            assumeDocker()
            serverApplication()

            val response = client.postGoogle(address = "203.0.113.10")

            assertEquals(expected = HttpStatusCode.Unauthorized, actual = response)
        }

    @Test
    fun `GIVEN the production graph WHEN a code cannot be exchanged THEN the fallback door answers 401`() =
        testApplication {
            assumeDocker()
            serverApplication()

            val response = client.postGoogleCode(address = "203.0.113.11")

            assertEquals(expected = HttpStatusCode.Unauthorized, actual = response)
        }

    @Test
    fun `GIVEN the production graph WHEN an unknown session is presented THEN the refresh door answers 401`() =
        testApplication {
            assumeDocker()
            serverApplication()

            val response = client.postRefresh(address = "203.0.113.12")

            assertEquals(expected = HttpStatusCode.Unauthorized, actual = response)
        }

    @Test
    fun `GIVEN the production graph WHEN the native door is flooded THEN it answers 429`() = testApplication {
        assumeDocker()
        serverApplication()

        val statuses = List(LIMIT + 1) { client.postGoogle(address = "203.0.113.20") }

        assertEquals(expected = HttpStatusCode.TooManyRequests, actual = statuses.last())
    }

    @Test
    fun `GIVEN the production graph WHEN the fallback door is flooded THEN it answers 429`() = testApplication {
        assumeDocker()
        serverApplication()

        val statuses = List(LIMIT + 1) { client.postGoogleCode(address = "203.0.113.21") }

        assertEquals(expected = HttpStatusCode.TooManyRequests, actual = statuses.last())
    }

    @Test
    fun `GIVEN the production graph WHEN the refresh door is flooded THEN it answers 429`() = testApplication {
        assumeDocker()
        serverApplication()

        val statuses = List(LIMIT + 1) { client.postRefresh(address = "203.0.113.22") }

        assertEquals(expected = HttpStatusCode.TooManyRequests, actual = statuses.last())
    }

    private suspend fun HttpClient.postGoogle(address: String): HttpStatusCode = post("/v1/auth/google") {
        contentType(ContentType.Application.Json)
        header(FORWARDED_FOR, address)
        setBody(Json.encodeToString(GoogleCredentialsDto(idToken = "not-a-token", nonce = "nonce-1")))
    }.status

    private suspend fun HttpClient.postGoogleCode(address: String): HttpStatusCode = post("/v1/auth/google/code") {
        contentType(ContentType.Application.Json)
        header(FORWARDED_FOR, address)
        setBody(
            Json.encodeToString(
                GoogleAuthorizationCodeDto(
                    code = "used-code",
                    codeVerifier = "verifier",
                    redirectUri = "com.googleusercontent.apps.android:/oauth2redirect",
                ),
            ),
        )
    }.status

    private suspend fun HttpClient.postRefresh(address: String): HttpStatusCode = post("/v1/auth/refresh") {
        contentType(ContentType.Application.Json)
        header(FORWARDED_FOR, address)
        setBody(Json.encodeToString(RefreshCredentialsDto(refreshToken = unknownRefreshToken())))
    }.status

    private fun ApplicationTestBuilder.serverApplication() {
        PostgresTestSupport.connect()
        application { serverModule(config = appConfig(), authFeature = offlineAuthFeature()) }
    }

    private fun assumeDocker() = assumeTrue(
        PostgresTestSupport.isDockerAvailable,
        "Docker is not available: the endpoint wiring guard did not run",
    )

    private fun tokenService(): JwtTokenService = JwtTokenService(
        jwtSecret = JWT_SECRET,
        jwtIssuer = "yap-backend",
        jwtAudience = "yap-mobile",
        accessTokenTtlSeconds = ACCESS_TOKEN_TTL_SECONDS,
    )

    private fun unknownRefreshToken(): String = tokenService().createRefreshToken().value

    private fun offlineAuthFeature(): AuthFeature = AuthFeature(
        googleAuthConfig = GoogleAuthConfig(
            androidClientId = ANDROID_CLIENT_ID,
            iosClientId = IOS_CLIENT_ID,
            webClientId = WEB_CLIENT_ID,
        ),
        refreshTokenTtlSeconds = REFRESH_TOKEN_TTL_SECONDS,
        tokenService = tokenService(),
        httpClient = HttpClient(
            MockEngine {
                respond(
                    content = """{"error":"invalid_grant"}""",
                    status = HttpStatusCode.BadRequest,
                    headers = headersOf("Content-Type", ContentType.Application.Json.toString()),
                )
            },
        ) {
            expectSuccess = false
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        },
    )

    private fun appConfig(): AppConfig = AppConfig(
        port = 8080,
        trustProxyHeaders = true,
        authRateLimitRequestsPerMinute = LIMIT,
        googleAndroidClientId = ANDROID_CLIENT_ID,
        googleIosClientId = IOS_CLIENT_ID,
        googleWebClientId = WEB_CLIENT_ID,
        database = DatabaseConfig(
            url = "jdbc:postgresql://localhost:5432/unused",
            user = "postgres",
            password = "postgres",
            poolSize = 1,
        ),
        auth = AuthConfig(
            jwtSecret = JWT_SECRET,
            jwtIssuer = "yap-backend",
            jwtAudience = "yap-mobile",
            accessTokenTtlSeconds = ACCESS_TOKEN_TTL_SECONDS,
            refreshTokenTtlSeconds = REFRESH_TOKEN_TTL_SECONDS,
        ),
    )

    private companion object {
        const val ACCESS_TOKEN_TTL_SECONDS = 900L
        const val ANDROID_CLIENT_ID = "android-client.apps.googleusercontent.com"
        const val FORWARDED_FOR = "X-Forwarded-For"
        const val IOS_CLIENT_ID = "ios-client.apps.googleusercontent.com"
        const val JWT_SECRET = "a-test-secret-that-is-at-least-forty-three-characters"
        const val LIMIT = 5
        const val REFRESH_TOKEN_TTL_SECONDS = 7_776_000L
        const val WEB_CLIENT_ID = "web-client.apps.googleusercontent.com"
    }
}
