package app.yap.server.feature.auth.identity

import app.yap.server.feature.auth.model.AuthFailure
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.client.request.forms.FormDataContent
import io.ktor.http.formUrlEncode
import io.ktor.http.headersOf
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

internal class GoogleCodeExchangerTest {

    @Test
    fun `GIVEN a valid code WHEN it is exchanged THEN the returned token is verified into an identity`() = runTest {
        val env = Environment(response = TokenEndpointResponse.Success)

        val identity = env.exchanger.exchange(
            code = CODE,
            codeVerifier = CODE_VERIFIER,
            redirectUri = REDIRECT_URI,
        )

        assertEquals(expected = StubGoogleToken.SUBJECT, actual = identity.subject)
    }

    @Test
    fun `GIVEN a valid code WHEN it is exchanged THEN the Android client id and verifier are submitted`() = runTest {
        val env = Environment(response = TokenEndpointResponse.Success)

        env.exchanger.exchange(code = CODE, codeVerifier = CODE_VERIFIER, redirectUri = REDIRECT_URI)

        val submitted = env.submittedBody()
        assertTrue(actual = submitted.contains("code_verifier=$CODE_VERIFIER"))
        assertTrue(actual = submitted.contains("client_id=${StubGoogleToken.ANDROID_CLIENT_ID}"))
    }

    @Test
    fun `GIVEN an already used code WHEN it is exchanged THEN the confirmation is refused`() = runTest {
        val env = Environment(response = TokenEndpointResponse.InvalidGrant)

        assertFailsWith<AuthFailure.UnverifiableConfirmation> {
            env.exchanger.exchange(code = CODE, codeVerifier = CODE_VERIFIER, redirectUri = REDIRECT_URI)
        }
    }

    @Test
    fun `GIVEN a mismatched verifier WHEN the code is exchanged THEN the confirmation is refused`() = runTest {
        val env = Environment(response = TokenEndpointResponse.InvalidGrant)

        assertFailsWith<AuthFailure.UnverifiableConfirmation> {
            env.exchanger.exchange(code = CODE, codeVerifier = "another-verifier", redirectUri = REDIRECT_URI)
        }
    }

    @Test
    fun `GIVEN Google is unreachable WHEN a code is exchanged THEN the provider is reported unavailable`() = runTest {
        val env = Environment(response = TokenEndpointResponse.Unreachable)

        assertFailsWith<AuthFailure.ProviderUnavailable> {
            env.exchanger.exchange(code = CODE, codeVerifier = CODE_VERIFIER, redirectUri = REDIRECT_URI)
        }
    }

    private enum class TokenEndpointResponse {
        Success,
        InvalidGrant,
        Unreachable,
    }

    private class Environment(
        response: TokenEndpointResponse,
    ) {

        private val requests = mutableListOf<HttpRequestData>()

        private val engine = MockEngine { request ->
            requests += request
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

        val exchanger = GoogleCodeExchanger(
            googleAuthConfig = StubGoogleToken.stubGoogleAuthConfig(),
            googleIdentityVerifier = GoogleIdentityVerifier(
                googleAuthConfig = StubGoogleToken.stubGoogleAuthConfig(),
                jwkProvider = StubGoogleToken.stubJwkProvider(),
            ),
            httpClient = HttpClient(engine) {
                expectSuccess = false
                install(ContentNegotiation) { json() }
            },
        )

        fun submittedBody(): String = (requests.single().body as FormDataContent).formData.formUrlEncode()
    }

    private companion object {
        const val CODE = "4/0AY0e-code"
        const val CODE_VERIFIER = "verifier-1234567890"
        const val REDIRECT_URI = "com.googleusercontent.apps.android-client:/oauth2redirect"
    }
}
