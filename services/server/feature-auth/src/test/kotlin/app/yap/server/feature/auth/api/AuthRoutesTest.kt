package app.yap.server.feature.auth.api

import app.yap.contract.auth.ErrorDto
import app.yap.contract.auth.LoginChallengeDto
import app.yap.contract.auth.SessionDto
import app.yap.server.feature.auth.AuthService
import app.yap.server.feature.auth.StubAuth
import app.yap.server.feature.auth.StubAuthAccount
import app.yap.server.feature.auth.StubAuthChallenge
import app.yap.server.feature.auth.StubAuthRepository
import app.yap.server.feature.auth.StubAuthSession
import app.yap.server.feature.auth.StubIdentityVerifier
import app.yap.server.feature.auth.StubLoginCredential
import app.yap.server.feature.auth.StubTokenService
import app.yap.server.feature.auth.identity.IdentityVerifiers
import app.yap.server.feature.auth.model.AuthAccount
import app.yap.server.feature.auth.model.AuthChallenge
import app.yap.server.feature.auth.model.SessionRotationResult
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.application.plugin
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.RoutingNode
import io.ktor.server.routing.RoutingRoot
import io.ktor.server.routing.getAllRoutes
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import java.time.Clock
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.Json

class AuthRoutesTest {

    @Test
    fun `GIVEN the authentication routes WHEN they are registered THEN only challenge login and refresh exist`() =
        authTest { _ ->
            startApplication()

            val routes = application.plugin(RoutingRoot).getAllRoutes().map(RoutingNode::toString)

            assertEquals(
                expected = listOf(
                    "/auth/challenge/(method:POST)",
                    "/auth/login/(method:POST)",
                    "/auth/refresh/(method:POST)",
                ),
                actual = routes,
            )
        }

    @Test
    fun `GIVEN a PKCE attempt WHEN requesting a challenge THEN the issued challenge is returned`() = authTest { client ->
        val response = client.postJson(
            path = CHALLENGE_PATH,
            body = StubAuthRequest.stubLoginChallengeRequest(
                codeChallenge = StubAuthChallenge.CODE_CHALLENGE,
                codeChallengeMethod = StubAuthRequest.S256_METHOD,
            ),
        )

        assertEquals(
            expected = LoginChallengeDto(
                challengeId = StubAuthChallenge.CHALLENGE_ID,
                nonce = StubAuthChallenge.NONCE,
                expiresAtEpochSeconds = StubAuthChallenge.EXPIRES_AT.epochSecond,
            ),
            actual = response.decode<LoginChallengeDto>(),
        )
    }

    @Test
    fun `GIVEN an attempt without PKCE WHEN requesting a challenge THEN the issued challenge is returned`() =
        authTest { client ->
            val response = client.postJson(
                path = CHALLENGE_PATH,
                body = StubAuthRequest.stubLoginChallengeRequest(),
            )

            assertEquals(
                expected = LoginChallengeDto(
                    challengeId = StubAuthChallenge.CHALLENGE_ID,
                    nonce = StubAuthChallenge.NONCE,
                    expiresAtEpochSeconds = StubAuthChallenge.EXPIRES_AT.epochSecond,
                ),
                actual = response.decode<LoginChallengeDto>(),
            )
        }

    @Test
    fun `GIVEN a code challenge without its method WHEN requesting a challenge THEN the request is invalid`() =
        authTest { client ->
            val response = client.postJson(
                path = CHALLENGE_PATH,
                body = StubAuthRequest.stubLoginChallengeRequest(codeChallenge = StubAuthChallenge.CODE_CHALLENGE),
            )

            assertEquals(expected = INVALID_REQUEST, actual = response.rejection())
        }

    @Test
    fun `GIVEN a code challenge method without its challenge WHEN requesting a challenge THEN the request is invalid`() =
        authTest { client ->
            val response = client.postJson(
                path = CHALLENGE_PATH,
                body = StubAuthRequest.stubLoginChallengeRequest(codeChallengeMethod = StubAuthRequest.S256_METHOD),
            )

            assertEquals(expected = INVALID_REQUEST, actual = response.rejection())
        }

    @Test
    fun `GIVEN an unsupported code challenge method WHEN requesting a challenge THEN the request is invalid`() =
        authTest { client ->
            val response = client.postJson(
                path = CHALLENGE_PATH,
                body = StubAuthRequest.stubLoginChallengeRequest(
                    codeChallenge = StubAuthChallenge.CODE_CHALLENGE,
                    codeChallengeMethod = "plain",
                ),
            )

            assertEquals(expected = INVALID_REQUEST, actual = response.rejection())
        }

    @Test
    fun `GIVEN the unregistered Apple provider WHEN requesting a challenge THEN the provider is unavailable`() =
        authTest { client ->
            val response = client.postJson(
                path = CHALLENGE_PATH,
                body = StubAuthRequest.stubLoginChallengeRequest(provider = APPLE_PROVIDER),
            )

            assertEquals(expected = PROVIDER_UNAVAILABLE, actual = response.rejection())
        }

    @Test
    fun `GIVEN the unregistered T-ID provider WHEN requesting a challenge THEN the provider is unavailable`() =
        authTest { client ->
            val response = client.postJson(
                path = CHALLENGE_PATH,
                body = StubAuthRequest.stubLoginChallengeRequest(provider = TID_PROVIDER),
            )

            assertEquals(expected = PROVIDER_UNAVAILABLE, actual = response.rejection())
        }

    @Test
    fun `GIVEN an identity token WHEN logging in THEN the issued session is returned`() = authTest { client ->
        val response = client.postJson(path = LOGIN_PATH, body = StubAuthRequest.stubLoginRequest())

        assertEquals(
            expected = SessionDto(
                accessToken = StubAuthSession.ACCESS_TOKEN,
                refreshToken = StubAuthSession.REFRESH_TOKEN,
                accessTokenExpiresAtEpochSeconds = StubAuthSession.ACCESS_TOKEN_EXPIRES_AT_EPOCH_SECONDS,
                accountId = StubAuthAccount.ACCOUNT_ID,
            ),
            actual = response.decode<SessionDto>(),
        )
    }

    @Test
    fun `GIVEN a browser fallback authorization code WHEN logging in THEN the issued session is returned`() =
        authTest(
            env = Environment(
                challenge = StubAuthChallenge.stubAuthChallenge(proof = StubAuthChallenge.CODE_CHALLENGE),
            ),
        ) { client ->
            val response = client.postJson(
                path = LOGIN_PATH,
                body = StubAuthRequest.stubAuthorizationCodeLoginRequest(),
            )

            assertEquals(
                expected = SessionDto(
                    accessToken = StubAuthSession.ACCESS_TOKEN,
                    refreshToken = StubAuthSession.REFRESH_TOKEN,
                    accessTokenExpiresAtEpochSeconds = StubAuthSession.ACCESS_TOKEN_EXPIRES_AT_EPOCH_SECONDS,
                    accountId = StubAuthAccount.ACCOUNT_ID,
                ),
                actual = response.decode<SessionDto>(),
            )
        }

    @Test
    fun `GIVEN an identity token login without a token WHEN logging in THEN the request is invalid`() =
        authTest { client ->
            val response = client.postJson(
                path = LOGIN_PATH,
                body = StubAuthRequest.stubLoginRequest(idToken = null),
            )

            assertEquals(expected = INVALID_REQUEST, actual = response.rejection())
        }

    @Test
    fun `GIVEN an identity token login carrying an authorization code WHEN logging in THEN the request is invalid`() =
        authTest { client ->
            val response = client.postJson(
                path = LOGIN_PATH,
                body = StubAuthRequest.stubLoginRequest(
                    authorizationCode = StubLoginCredential.AUTHORIZATION_CODE,
                ),
            )

            assertEquals(expected = INVALID_REQUEST, actual = response.rejection())
        }

    @Test
    fun `GIVEN an identity token login carrying a code verifier WHEN logging in THEN the request is invalid`() =
        authTest { client ->
            val response = client.postJson(
                path = LOGIN_PATH,
                body = StubAuthRequest.stubLoginRequest(codeVerifier = StubAuthChallenge.CODE_VERIFIER),
            )

            assertEquals(expected = INVALID_REQUEST, actual = response.rejection())
        }

    @Test
    fun `GIVEN an identity token login carrying a redirect uri WHEN logging in THEN the request is invalid`() =
        authTest { client ->
            val response = client.postJson(
                path = LOGIN_PATH,
                body = StubAuthRequest.stubLoginRequest(redirectUri = StubLoginCredential.REDIRECT_URI),
            )

            assertEquals(expected = INVALID_REQUEST, actual = response.rejection())
        }

    @Test
    fun `GIVEN an authorization code login carrying an identity token WHEN logging in THEN the request is invalid`() =
        authTest { client ->
            val response = client.postJson(
                path = LOGIN_PATH,
                body = StubAuthRequest.stubAuthorizationCodeLoginRequest(
                    idToken = StubLoginCredential.ID_TOKEN,
                ),
            )

            assertEquals(expected = INVALID_REQUEST, actual = response.rejection())
        }

    @Test
    fun `GIVEN an authorization code login without a code WHEN logging in THEN the request is invalid`() =
        authTest { client ->
            val response = client.postJson(
                path = LOGIN_PATH,
                body = StubAuthRequest.stubAuthorizationCodeLoginRequest(authorizationCode = null),
            )

            assertEquals(expected = INVALID_REQUEST, actual = response.rejection())
        }

    @Test
    fun `GIVEN an authorization code login without a code verifier WHEN logging in THEN the request is invalid`() =
        authTest { client ->
            val response = client.postJson(
                path = LOGIN_PATH,
                body = StubAuthRequest.stubAuthorizationCodeLoginRequest(codeVerifier = null),
            )

            assertEquals(expected = INVALID_REQUEST, actual = response.rejection())
        }

    @Test
    fun `GIVEN an authorization code login without a redirect uri WHEN logging in THEN the request is invalid`() =
        authTest { client ->
            val response = client.postJson(
                path = LOGIN_PATH,
                body = StubAuthRequest.stubAuthorizationCodeLoginRequest(redirectUri = null),
            )

            assertEquals(expected = INVALID_REQUEST, actual = response.rejection())
        }

    @Test
    fun `GIVEN an unknown credential type WHEN logging in THEN the request is invalid`() = authTest { client ->
        val response = client.postJson(
            path = LOGIN_PATH,
            body = StubAuthRequest.stubLoginRequest(credentialType = "password"),
        )

        assertEquals(expected = INVALID_REQUEST, actual = response.rejection())
    }

    @Test
    fun `GIVEN a body that is not a login request WHEN logging in THEN the request is invalid`() = authTest { client ->
        val response = client.post(LOGIN_PATH) {
            contentType(ContentType.Application.Json)
            setBody("{")
        }

        assertEquals(expected = INVALID_REQUEST, actual = response.rejection())
    }

    @Test
    fun `GIVEN the unregistered T-ID provider WHEN logging in THEN the provider is unavailable`() = authTest { client ->
        val response = client.postJson(
            path = LOGIN_PATH,
            body = StubAuthRequest.stubLoginRequest(provider = TID_PROVIDER),
        )

        assertEquals(expected = PROVIDER_UNAVAILABLE, actual = response.rejection())
    }

    @Test
    fun `GIVEN a missing challenge WHEN logging in THEN the challenge is invalid`() =
        authTest(env = Environment(challenge = null)) { client ->
            val response = client.postJson(path = LOGIN_PATH, body = StubAuthRequest.stubLoginRequest())

            assertEquals(expected = CHALLENGE_INVALID, actual = response.rejection())
        }

    @Test
    fun `GIVEN an expired challenge WHEN logging in THEN the challenge is invalid`() =
        authTest(
            env = Environment(challenge = StubAuthChallenge.stubAuthChallenge(expiresAt = StubAuth.NOW)),
        ) { client ->
            val response = client.postJson(path = LOGIN_PATH, body = StubAuthRequest.stubLoginRequest())

            assertEquals(expected = CHALLENGE_INVALID, actual = response.rejection())
        }

    @Test
    fun `GIVEN a challenge without a stored proof WHEN logging in with a code THEN the challenge is invalid`() =
        authTest { client ->
            val response = client.postJson(
                path = LOGIN_PATH,
                body = StubAuthRequest.stubAuthorizationCodeLoginRequest(),
            )

            assertEquals(expected = CHALLENGE_INVALID, actual = response.rejection())
        }

    @Test
    fun `GIVEN a mismatched code verifier WHEN logging in with a code THEN the challenge is invalid`() =
        authTest(
            env = Environment(
                challenge = StubAuthChallenge.stubAuthChallenge(proof = StubAuthChallenge.CODE_CHALLENGE),
            ),
        ) { client ->
            val response = client.postJson(
                path = LOGIN_PATH,
                body = StubAuthRequest.stubAuthorizationCodeLoginRequest(codeVerifier = "another-code-verifier"),
            )

            assertEquals(expected = CHALLENGE_INVALID, actual = response.rejection())
        }

    @Test
    fun `GIVEN a rejected login WHEN it fails THEN the error body carries no credential`() =
        authTest(env = Environment(challenge = null)) { client ->
            val response = client.postJson(path = LOGIN_PATH, body = StubAuthRequest.stubLoginRequest())

            assertEquals(
                expected = ErrorDto(code = "challenge_invalid", message = "The login challenge is not valid."),
                actual = response.decode<ErrorDto>(),
            )
        }

    @Test
    fun `GIVEN a current refresh value WHEN refreshing THEN the rotated session is returned`() = authTest { client ->
        val response = client.postJson(path = REFRESH_PATH, body = StubAuthRequest.stubRefreshRequest())

        assertEquals(
            expected = SessionDto(
                accessToken = StubAuthSession.ACCESS_TOKEN,
                refreshToken = StubAuthSession.REFRESH_TOKEN,
                accessTokenExpiresAtEpochSeconds = StubAuthSession.ACCESS_TOKEN_EXPIRES_AT_EPOCH_SECONDS,
                accountId = StubAuthAccount.ACCOUNT_ID,
            ),
            actual = response.decode<SessionDto>(),
        )
    }

    @Test
    fun `GIVEN an unknown refresh value WHEN refreshing THEN the session is invalid`() =
        authTest(env = Environment(rotation = SessionRotationResult.Unknown)) { client ->
            val response = client.postJson(path = REFRESH_PATH, body = StubAuthRequest.stubRefreshRequest())

            assertEquals(expected = SESSION_INVALID, actual = response.rejection())
        }

    @Test
    fun `GIVEN a replayed refresh value WHEN refreshing THEN the session is invalid`() =
        authTest(env = Environment(rotation = SessionRotationResult.Replayed)) { client ->
            val response = client.postJson(path = REFRESH_PATH, body = StubAuthRequest.stubRefreshRequest())

            assertEquals(expected = SESSION_INVALID, actual = response.rejection())
        }

    @Test
    fun `GIVEN a rejected refresh WHEN it fails THEN the error body carries no credential`() =
        authTest(env = Environment(rotation = SessionRotationResult.Unknown)) { client ->
            val response = client.postJson(path = REFRESH_PATH, body = StubAuthRequest.stubRefreshRequest())

            assertEquals(
                expected = ErrorDto(code = "session_invalid", message = "The session is no longer valid."),
                actual = response.decode<ErrorDto>(),
            )
        }

    @Test
    fun `GIVEN a body that is not a refresh request WHEN refreshing THEN the request is invalid`() =
        authTest { client ->
            val response = client.post(REFRESH_PATH) {
                contentType(ContentType.Application.Json)
                setBody("{}")
            }

            assertEquals(expected = INVALID_REQUEST, actual = response.rejection())
        }

    private fun authTest(
        env: Environment = Environment(),
        block: suspend ApplicationTestBuilder.(HttpClient) -> Unit,
    ) = testApplication {
        application {
            install(ContentNegotiation) { json() }
            routing { authRoutes(env.service) }
        }

        block(client)
    }

    private suspend inline fun <reified T> HttpClient.postJson(path: String, body: T): HttpResponse =
        post(path) {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(body))
        }

    private suspend inline fun <reified T> HttpResponse.decode(): T = Json.decodeFromString(bodyAsText())

    private suspend fun HttpResponse.rejection(): Rejection =
        Rejection(code = decode<ErrorDto>().code, status = status)

    /** One rejected request as a client observes it: the opaque code and the status carrying it. */
    private data class Rejection(val code: String, val status: HttpStatusCode)

    private companion object {
        const val APPLE_PROVIDER = "apple"
        const val CHALLENGE_PATH = "/auth/challenge"
        const val LOGIN_PATH = "/auth/login"
        const val REFRESH_PATH = "/auth/refresh"
        const val TID_PROVIDER = "tid"

        val CHALLENGE_INVALID = Rejection(code = "challenge_invalid", status = HttpStatusCode.Unauthorized)
        val INVALID_REQUEST = Rejection(code = "invalid_request", status = HttpStatusCode.BadRequest)
        val PROVIDER_UNAVAILABLE = Rejection(code = "provider_unavailable", status = HttpStatusCode.ServiceUnavailable)
        val SESSION_INVALID = Rejection(code = "session_invalid", status = HttpStatusCode.Unauthorized)
    }

    private class Environment(
        account: AuthAccount? = StubAuthAccount.stubAuthAccount(),
        challenge: AuthChallenge? = StubAuthChallenge.stubAuthChallenge(),
        rotation: SessionRotationResult = SessionRotationResult.Rotated(StubAuthAccount.ACCOUNT_ID),
    ) {

        val repository = StubAuthRepository(
            account = account,
            challenge = challenge,
            rotation = rotation,
        )
        val tokenService = StubTokenService()
        val verifier = StubIdentityVerifier()
        val service = AuthService(
            clock = Clock.fixed(StubAuth.NOW, ZoneOffset.UTC),
            identityVerifiers = IdentityVerifiers(verifiers = listOf(verifier)),
            refreshTokenTtl = StubAuthSession.INACTIVITY_LIMIT,
            repository = repository,
            tokenService = tokenService,
        )
    }
}
