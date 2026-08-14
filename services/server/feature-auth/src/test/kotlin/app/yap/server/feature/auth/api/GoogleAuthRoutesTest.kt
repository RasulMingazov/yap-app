package app.yap.server.feature.auth.api

import app.yap.server.core.security.JwtTokenService
import app.yap.server.feature.auth.AuthService
import app.yap.server.feature.auth.StubAuthPersistence
import app.yap.server.feature.auth.identity.GoogleIdentityVerifier
import app.yap.server.feature.auth.identity.StubGoogleToken
import app.yap.server.feature.auth.model.AuthFailure
import app.yap.contract.auth.GoogleCredentialsDto
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
import kotlinx.serialization.json.Json

internal class GoogleAuthRoutesTest {

    @Test
    fun `GIVEN a verified credential WHEN it is posted THEN a session is returned in wire field order`() =
        testApplication {
            val env = Environment()
            routeApplication(env)

            val response = client.post("/v1/auth/google") {
                contentType(ContentType.Application.Json)
                setBody(
                    Json.encodeToString(
                        GoogleCredentialsDto(
                            idToken = StubGoogleToken.stubIdToken(),
                            nonce = StubGoogleToken.NONCE,
                        ),
                    ),
                )
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
    fun `GIVEN a malformed body WHEN it is posted THEN the service is never reached`() = testApplication {
        val env = Environment()
        routeApplication(env)

        client.post("/v1/auth/google") {
            contentType(ContentType.Application.Json)
            setBody("{\"idToken\":")
        }

        assertIs<AuthFailure.MalformedInput>(env.raisedFailure)
        assertEquals(expected = emptyList(), actual = env.persistence.resolvedIdentities)
    }

    @Test
    fun `GIVEN an unverifiable credential WHEN it is posted THEN nothing is written and it is refused`() =
        testApplication {
            val env = Environment()
            routeApplication(env)

            client.post("/v1/auth/google") {
                contentType(ContentType.Application.Json)
                setBody(
                    Json.encodeToString(
                        GoogleCredentialsDto(
                            idToken = StubGoogleToken.stubIdToken(
                                keyId = StubGoogleToken.OTHER_KEY_ID,
                                signingKeyPair = StubGoogleToken.otherKeyPair,
                            ),
                            nonce = StubGoogleToken.NONCE,
                        ),
                    ),
                )
            }

            assertIs<AuthFailure.UnverifiableConfirmation>(env.raisedFailure)
            assertEquals(expected = emptyList(), actual = env.persistence.resolvedIdentities)
            assertEquals(expected = emptyList(), actual = env.persistence.createdSessions)
        }

    @Test
    fun `GIVEN an unreachable key set WHEN a credential is posted THEN the provider is reported unavailable`() =
        testApplication {
            val env = Environment(isKeySetReachable = false)
            routeApplication(env)

            client.post("/v1/auth/google") {
                contentType(ContentType.Application.Json)
                setBody(
                    Json.encodeToString(
                        GoogleCredentialsDto(
                            idToken = StubGoogleToken.stubIdToken(),
                            nonce = StubGoogleToken.NONCE,
                        ),
                    ),
                )
            }

            assertIs<AuthFailure.ProviderUnavailable>(env.raisedFailure)
            assertEquals(expected = emptyList(), actual = env.persistence.resolvedIdentities)
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
        isKeySetReachable: Boolean = true,
    ) {

        var raisedFailure: AuthFailure? = null
        val persistence = StubAuthPersistence()
        val authService = AuthService(
            authPersistence = persistence,
            clock = Clock.fixed(NOW, ZoneOffset.UTC),
            googleCodeExchanger = StubGoogleToken.stubUnusedCodeExchanger(),
            googleIdentityVerifier = GoogleIdentityVerifier(
                googleAuthConfig = StubGoogleToken.stubGoogleAuthConfig(),
                jwkProvider = if (isKeySetReachable) {
                    StubGoogleToken.stubJwkProvider()
                } else {
                    StubGoogleToken.stubUnreachableJwkProvider()
                },
            ),
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
        val NOW: Instant = Instant.parse("2026-08-13T10:00:00Z")
    }
}
