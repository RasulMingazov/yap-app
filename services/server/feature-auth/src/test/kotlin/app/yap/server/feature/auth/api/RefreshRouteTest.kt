package app.yap.server.feature.auth.api

import app.yap.contract.auth.RefreshCredentialsDto
import app.yap.server.core.security.JwtTokenService
import app.yap.server.feature.auth.AuthService
import app.yap.server.feature.auth.StubAuthPersistence
import app.yap.server.feature.auth.identity.StubGoogleToken
import app.yap.server.feature.auth.model.AuthFailure
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
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
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json

internal class RefreshRouteTest {

    @Test
    fun `GIVEN a valid refresh token WHEN it is posted THEN a rotated session is returned`() = testApplication {
        val env = Environment()
        routeApplication(env)

        val response = client.post("/v1/auth/refresh") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(RefreshCredentialsDto(refreshToken = env.refreshToken)))
        }

        assertEquals(expected = HttpStatusCode.OK, actual = response.status)
        assertEquals(
            expected = listOf(
                "accessToken",
                "refreshToken",
                "accessTokenExpiresAtEpochSeconds",
                "refreshTokenExpiresAtEpochSeconds",
            ),
            actual = response.bodyAsText().jsonFieldOrder(),
        )
    }

    @Test
    fun `GIVEN a rotation WHEN it succeeds THEN the presented token is replaced rather than kept`() = testApplication {
        val env = Environment()
        routeApplication(env)

        client.post("/v1/auth/refresh") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(RefreshCredentialsDto(refreshToken = env.refreshToken)))
        }

        val rotation = env.persistence.rotations.single()
        assertEquals(expected = env.tokenService.hash(env.refreshToken), actual = rotation.presentedRefreshTokenHash)
        assertTrue(actual = rotation.refreshTokenHash != rotation.presentedRefreshTokenHash)
    }

    @Test
    fun `GIVEN a session that cannot be rotated WHEN refresh is posted THEN the confirmation is unverifiable`() =
        testApplication {
            val env = Environment(rotatedUserId = null)
            routeApplication(env)

            client.post("/v1/auth/refresh") {
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString(RefreshCredentialsDto(refreshToken = env.refreshToken)))
            }

            assertIs<AuthFailure.UnverifiableConfirmation>(env.raisedFailure)
        }

    @Test
    fun `GIVEN a malformed token value WHEN refresh is posted THEN nothing is rotated`() = testApplication {
        val env = Environment()
        routeApplication(env)

        client.post("/v1/auth/refresh") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(RefreshCredentialsDto(refreshToken = "not-a-refresh-token")))
        }

        assertIs<AuthFailure.MalformedInput>(env.raisedFailure)
        assertEquals(expected = emptyList(), actual = env.persistence.rotations)
    }

    @Test
    fun `GIVEN a malformed body WHEN refresh is posted THEN the service is never reached`() = testApplication {
        val env = Environment()
        routeApplication(env)

        client.post("/v1/auth/refresh") {
            contentType(ContentType.Application.Json)
            setBody("{\"refreshToken\":")
        }

        assertIs<AuthFailure.MalformedInput>(env.raisedFailure)
        assertEquals(expected = emptyList(), actual = env.persistence.rotations)
    }

    private fun ApplicationTestBuilder.routeApplication(env: Environment) = application {
        install(ContentNegotiation) { json() }
        install(StatusPages) {
            exception<AuthFailure> { call, failure ->
                env.raisedFailure = failure
                call.respond(HttpStatusCode.InternalServerError)
            }
        }
        routing { authRoutes(env.authService) }
    }

    private fun String.jsonFieldOrder(): List<String> =
        Regex("\"([A-Za-z]+)\"\\s*:").findAll(this).map { match -> match.groupValues[1] }.toList()

    private class Environment(
        rotatedUserId: String? = "user-1",
    ) {

        var raisedFailure: AuthFailure? = null
        val persistence = StubAuthPersistence(rotatedUserId = rotatedUserId)
        val tokenService = JwtTokenService(
            jwtSecret = "a-test-secret-that-is-at-least-forty-three-characters",
            jwtIssuer = "yap-backend",
            jwtAudience = "yap-mobile",
            accessTokenTtlSeconds = 900,
            clock = Clock.fixed(NOW, ZoneOffset.UTC),
        )
        val refreshToken: String = tokenService.createRefreshToken().value
        val authService = AuthService(
            authPersistence = persistence,
            clock = Clock.fixed(NOW, ZoneOffset.UTC),
            googleCodeExchanger = StubGoogleToken.stubUnusedCodeExchanger(),
            googleIdentityVerifier = StubGoogleToken.stubIdentityVerifier(),
            refreshTokenTtlSeconds = REFRESH_TOKEN_TTL_SECONDS,
            tokenService = tokenService,
        )
    }

    private companion object {
        const val REFRESH_TOKEN_TTL_SECONDS = 7_776_000L
        val NOW: Instant = Instant.parse("2026-08-13T10:00:00Z")
    }
}
