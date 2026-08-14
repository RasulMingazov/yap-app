package app.yap.server.feature.auth.api

import app.yap.contract.auth.GoogleAuthorizationCodeDto
import app.yap.server.core.security.JwtTokenService
import app.yap.server.feature.auth.AuthService
import app.yap.server.feature.auth.StubAuthPersistence
import app.yap.server.feature.auth.identity.GoogleCodeExchanger
import app.yap.server.feature.auth.identity.GoogleIdentityVerifier
import app.yap.server.feature.auth.identity.StubGoogleToken
import app.yap.server.feature.auth.model.AuthFailure
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.serialization.json.Json
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation as ServerContentNegotiation

internal class GoogleCodeAuthRoutesTest {

    @Test
    fun `GIVEN a valid authorization code WHEN it is posted THEN a session is returned`() = testApplication {
        val env = Environment(response = TokenEndpointResponse.Success)
        routeApplication(env)

        val response = client.post("/v1/auth/google/code") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(stubCodeDto()))
        }

        assertEquals(expected = HttpStatusCode.OK, actual = response.status)
        assertEquals(expected = 1, actual = env.persistence.createdSessions.size)
    }

    @Test
    fun `GIVEN a replayed authorization code WHEN it is posted THEN nothing is written and it is refused`() =
        testApplication {
            val env = Environment(response = TokenEndpointResponse.InvalidGrant)
            routeApplication(env)

            client.post("/v1/auth/google/code") {
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString(stubCodeDto()))
            }

            assertIs<AuthFailure.UnverifiableConfirmation>(env.raisedFailure)
            assertEquals(expected = emptyList(), actual = env.persistence.resolvedIdentities)
        }

    @Test
    fun `GIVEN an unreachable token endpoint WHEN a code is posted THEN the provider is reported unavailable`() =
        testApplication {
            val env = Environment(response = TokenEndpointResponse.Unreachable)
            routeApplication(env)

            client.post("/v1/auth/google/code") {
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString(stubCodeDto()))
            }

            assertIs<AuthFailure.ProviderUnavailable>(env.raisedFailure)
            assertEquals(expected = emptyList(), actual = env.persistence.resolvedIdentities)
        }

    @Test
    fun `GIVEN a malformed body WHEN it is posted THEN the exchange is never attempted`() = testApplication {
        val env = Environment(response = TokenEndpointResponse.Success)
        routeApplication(env)

        client.post("/v1/auth/google/code") {
            contentType(ContentType.Application.Json)
            setBody("{\"code\":")
        }

        assertIs<AuthFailure.MalformedInput>(env.raisedFailure)
        assertEquals(expected = emptyList(), actual = env.persistence.resolvedIdentities)
    }

    private fun ApplicationTestBuilder.routeApplication(env: Environment) = application {
        install(ServerContentNegotiation) { json() }
        install(StatusPages) {
            exception<AuthFailure> { call, failure ->
                env.raisedFailure = failure
                call.respond(HttpStatusCode.InternalServerError)
            }
        }
        routing { authRoutes(env.authService) }
    }

    private fun stubCodeDto(): GoogleAuthorizationCodeDto = GoogleAuthorizationCodeDto(
        code = CODE,
        codeVerifier = CODE_VERIFIER,
        redirectUri = REDIRECT_URI,
    )

    private enum class TokenEndpointResponse {
        Success,
        InvalidGrant,
        Unreachable,
    }

    private class Environment(
        response: TokenEndpointResponse,
    ) {

        var raisedFailure: AuthFailure? = null
        val persistence = StubAuthPersistence()

        private val verifier = GoogleIdentityVerifier(
            googleAuthConfig = StubGoogleToken.stubGoogleAuthConfig(),
            jwkProvider = StubGoogleToken.stubJwkProvider(),
        )

        private val engine = MockEngine {
            when (response) {
                TokenEndpointResponse.Success -> respond(
                    content = """{"id_token":"${StubGoogleToken.stubIdToken(
                        audience = StubGoogleToken.ANDROID_CLIENT_ID,
                        nonce = null,
                    )}"}""",
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                )

                TokenEndpointResponse.InvalidGrant -> respondError(
                    status = HttpStatusCode.BadRequest,
                    content = """{"error":"invalid_grant"}""",
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                )

                TokenEndpointResponse.Unreachable -> throw java.io.IOException("connect timed out")
            }
        }

        val authService = AuthService(
            authPersistence = persistence,
            clock = Clock.fixed(NOW, ZoneOffset.UTC),
            googleCodeExchanger = GoogleCodeExchanger(
                googleAuthConfig = StubGoogleToken.stubGoogleAuthConfig(),
                googleIdentityVerifier = verifier,
                httpClient = HttpClient(engine) {
                    expectSuccess = false
                    install(ClientContentNegotiation) { json() }
                },
            ),
            googleIdentityVerifier = verifier,
            refreshTokenTtlSeconds = 7_776_000L,
            tokenService = JwtTokenService(
                jwtSecret = "a-test-secret-that-is-at-least-forty-three-characters",
                jwtIssuer = "yap-backend",
                jwtAudience = "yap-mobile",
                accessTokenTtlSeconds = 900,
                clock = Clock.fixed(NOW, ZoneOffset.UTC),
            ),
        )
    }

    private companion object {
        const val CODE = "4/0AY0e-code"
        const val CODE_VERIFIER = "verifier-1234567890"
        const val REDIRECT_URI = "com.googleusercontent.apps.android-client:/oauth2redirect"
        val NOW: Instant = Instant.parse("2026-08-13T10:00:00Z")
    }
}
