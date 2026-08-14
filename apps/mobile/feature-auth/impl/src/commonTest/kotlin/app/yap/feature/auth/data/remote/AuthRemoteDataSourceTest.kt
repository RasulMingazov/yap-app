package app.yap.feature.auth.data.remote

import app.yap.contract.auth.GoogleAuthorizationCodeDto
import app.yap.contract.auth.GoogleCredentialsDto
import app.yap.feature.auth.data.local.SessionLocal
import app.yap.feature.auth.data.mapper.toLocal
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

internal class AuthRemoteDataSourceTest {

    @Test
    fun `GIVEN the server issues a session WHEN logging in with an id token THEN it maps to the stored record`() =
        runTest {
            val env = Environment(outcome = ServerOutcome.Session)

            val session = env.dataSource.loginWithGoogleIdToken(
                GoogleCredentialsDto(idToken = ID_TOKEN, nonce = NONCE),
            )

            assertEquals(
                expected = SessionLocal(
                    accessToken = ACCESS_TOKEN,
                    refreshToken = REFRESH_TOKEN,
                    accessTokenExpiresAtEpochSeconds = ACCESS_TOKEN_EXPIRES_AT,
                    refreshTokenExpiresAtEpochSeconds = REFRESH_TOKEN_EXPIRES_AT,
                ),
                actual = session.toLocal(),
            )
        }

    @Test
    fun `GIVEN an id token WHEN it is submitted THEN it reaches the native Google door`() = runTest {
        val env = Environment(outcome = ServerOutcome.Session)

        env.dataSource.loginWithGoogleIdToken(GoogleCredentialsDto(idToken = ID_TOKEN, nonce = NONCE))

        assertEquals(expected = "/v1/auth/google", actual = env.requestedPath())
    }

    @Test
    fun `GIVEN an authorization code WHEN it is submitted THEN it reaches the browser fallback door`() = runTest {
        val env = Environment(outcome = ServerOutcome.Session)

        env.dataSource.loginWithGoogleAuthorizationCode(
            GoogleAuthorizationCodeDto(code = CODE, codeVerifier = CODE_VERIFIER, redirectUri = REDIRECT_URI),
        )

        assertEquals(expected = "/v1/auth/google/code", actual = env.requestedPath())
    }

    @Test
    fun `GIVEN the server refuses the confirmation WHEN logging in THEN it is reported as rejected`() = runTest {
        val env = Environment(outcome = ServerOutcome.Unauthorized)

        assertFailsWith<AuthRemoteFailure.Rejected> {
            env.dataSource.loginWithGoogleIdToken(GoogleCredentialsDto(idToken = ID_TOKEN, nonce = NONCE))
        }
    }

    @Test
    fun `GIVEN the server is unavailable WHEN logging in THEN it is reported as no answer`() = runTest {
        val env = Environment(outcome = ServerOutcome.ServerError)

        assertFailsWith<AuthRemoteFailure.Unavailable> {
            env.dataSource.loginWithGoogleIdToken(GoogleCredentialsDto(idToken = ID_TOKEN, nonce = NONCE))
        }
    }

    @Test
    fun `GIVEN the caller is rate limited WHEN logging in THEN it is reported as no answer`() = runTest {
        val env = Environment(outcome = ServerOutcome.RateLimited)

        assertFailsWith<AuthRemoteFailure.Unavailable> {
            env.dataSource.loginWithGoogleIdToken(GoogleCredentialsDto(idToken = ID_TOKEN, nonce = NONCE))
        }
    }

    @Test
    fun `GIVEN there is no network WHEN logging in THEN it is reported as no answer`() = runTest {
        val env = Environment(outcome = ServerOutcome.NoNetwork)

        assertFailsWith<AuthRemoteFailure.Unavailable> {
            env.dataSource.loginWithGoogleIdToken(GoogleCredentialsDto(idToken = ID_TOKEN, nonce = NONCE))
        }
    }

    @Test
    fun `GIVEN the server refuses WHEN logging in THEN the failure carries no transport detail`() = runTest {
        val env = Environment(outcome = ServerOutcome.Unauthorized)

        val failure = assertFailsWith<AuthRemoteFailure.Rejected> {
            env.dataSource.loginWithGoogleIdToken(GoogleCredentialsDto(idToken = ID_TOKEN, nonce = NONCE))
        }

        assertTrue(actual = failure.message.orEmpty().none(Char::isDigit))
    }

    private enum class ServerOutcome {
        Session,
        Unauthorized,
        ServerError,
        RateLimited,
        NoNetwork,
    }

    private class Environment(
        outcome: ServerOutcome,
    ) {

        private val requests = mutableListOf<HttpRequestData>()

        private val engine = MockEngine { request ->
            requests += request
            when (outcome) {
                ServerOutcome.Session -> respond(
                    content = SESSION_JSON,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                )

                ServerOutcome.Unauthorized -> respondError(HttpStatusCode.Unauthorized)
                ServerOutcome.ServerError -> respondError(HttpStatusCode.InternalServerError)
                ServerOutcome.RateLimited -> respondError(HttpStatusCode.TooManyRequests)
                ServerOutcome.NoNetwork -> error("no network")
            }
        }

        val dataSource: AuthRemoteDataSource = DefaultAuthRemoteDataSource(
            baseUrl = BASE_URL,
            httpClient = HttpClient(engine) {
                expectSuccess = false
                install(ContentNegotiation) { json() }
            },
        )

        fun requestedPath(): String = requests.single().url.encodedPath
    }

    private companion object {
        const val BASE_URL = "https://api.example.com"
        const val ACCESS_TOKEN = "access-token"
        const val REFRESH_TOKEN = "refresh-token"
        const val ACCESS_TOKEN_EXPIRES_AT = 1_800_000_900L
        const val REFRESH_TOKEN_EXPIRES_AT = 1_807_776_000L
        const val ID_TOKEN = "google-id-token"
        const val NONCE = "nonce-1"
        const val CODE = "auth-code"
        const val CODE_VERIFIER = "verifier"
        const val REDIRECT_URI = "com.googleusercontent.apps.android:/oauth2redirect"
        val SESSION_JSON = """
            {
              "accessToken": "$ACCESS_TOKEN",
              "refreshToken": "$REFRESH_TOKEN",
              "accessTokenExpiresAtEpochSeconds": $ACCESS_TOKEN_EXPIRES_AT,
              "refreshTokenExpiresAtEpochSeconds": $REFRESH_TOKEN_EXPIRES_AT
            }
        """.trimIndent()
    }
}
