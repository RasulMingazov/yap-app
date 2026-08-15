package app.yap.feature.auth.data.remote

import app.yap.contract.auth.GoogleAuthorizationCodeDto
import app.yap.contract.auth.GoogleCredentialsDto
import app.yap.contract.auth.RefreshCredentialsDto
import app.yap.contract.auth.SessionDto
import app.yap.core.network.ApiClient
import app.yap.core.network.ApiResult
import app.yap.core.network.createNetworkClient
import app.yap.feature.auth.data.local.SessionLocal
import app.yap.feature.auth.data.mapper.toLocal
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.content.TextContent
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

internal class AuthRemoteDataSourceTest {

    @Test
    fun `GIVEN the server issues a session WHEN logging in with an id token THEN it maps to the stored record`() =
        runTest {
            val env = Environment()

            val result = env.dataSource.loginWithGoogleIdToken(
                GoogleCredentialsDto(idToken = ID_TOKEN, nonce = NONCE),
            )

            assertEquals(
                expected = SessionLocal(
                    accessToken = ACCESS_TOKEN,
                    refreshToken = REFRESH_TOKEN,
                    accessTokenExpiresAtEpochSeconds = ACCESS_TOKEN_EXPIRES_AT,
                    refreshTokenExpiresAtEpochSeconds = REFRESH_TOKEN_EXPIRES_AT,
                ),
                actual = assertSession(result).toLocal(),
            )
        }

    @Test
    fun `GIVEN an id token WHEN it is submitted THEN it reaches the native Google door`() = runTest {
        val env = Environment()

        env.dataSource.loginWithGoogleIdToken(GoogleCredentialsDto(idToken = ID_TOKEN, nonce = NONCE))

        assertEquals(expected = "/v1/auth/google", actual = env.requested().url.encodedPath)
    }

    @Test
    fun `GIVEN an authorization code WHEN it is submitted THEN it reaches the browser fallback door`() = runTest {
        val env = Environment()

        env.dataSource.loginWithGoogleAuthorizationCode(
            GoogleAuthorizationCodeDto(code = CODE, codeVerifier = CODE_VERIFIER, redirectUri = REDIRECT_URI),
        )

        assertEquals(expected = "/v1/auth/google/code", actual = env.requested().url.encodedPath)
    }

    @Test
    fun `GIVEN a refresh token WHEN it is submitted THEN it reaches the refresh door`() = runTest {
        val env = Environment()

        env.dataSource.refresh(RefreshCredentialsDto(refreshToken = REFRESH_TOKEN))

        assertEquals(expected = "/v1/auth/refresh", actual = env.requested().url.encodedPath)
    }

    @Test
    fun `GIVEN an id token WHEN it is submitted THEN the body carries the nonce it was bound to`() = runTest {
        val env = Environment()

        env.dataSource.loginWithGoogleIdToken(GoogleCredentialsDto(idToken = ID_TOKEN, nonce = NONCE))

        assertTrue(actual = env.requestedBody().contains(NONCE))
    }

    @Test
    fun `GIVEN a session is established WHEN the request is sent THEN it carries no bearer token`() = runTest {
        val env = Environment()

        env.dataSource.loginWithGoogleIdToken(GoogleCredentialsDto(idToken = ID_TOKEN, nonce = NONCE))

        assertNull(actual = env.requested().headers[HttpHeaders.Authorization])
    }

    private fun assertSession(result: ApiResult<SessionDto>): SessionDto =
        (result as ApiResult.Success).value

    private class Environment {

        private val requests = mutableListOf<HttpRequestData>()

        private val engine = MockEngine { request ->
            requests += request
            respond(
                content = SESSION_JSON,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }

        val dataSource: AuthRemoteDataSource = DefaultAuthRemoteDataSource(
            apiClient = ApiClient(createNetworkClient(baseUrl = BASE_URL, engine = engine, timeouts = null)),
        )

        fun requested(): HttpRequestData = requests.single()

        fun requestedBody(): String = (requested().body as TextContent).text
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
