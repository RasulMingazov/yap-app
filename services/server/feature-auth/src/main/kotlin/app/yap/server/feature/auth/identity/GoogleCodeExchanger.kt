package app.yap.server.feature.auth.identity

import app.yap.server.feature.auth.model.AuthFailure
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.submitForm
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import io.ktor.http.parameters
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

private const val GOOGLE_TOKEN_ENDPOINT = "https://oauth2.googleapis.com/token"

internal class GoogleCodeExchanger(
    private val googleAuthConfig: GoogleAuthConfig,
    private val googleIdentityVerifier: GoogleIdentityVerifier,
    private val httpClient: HttpClient,
    private val tokenEndpoint: String = GOOGLE_TOKEN_ENDPOINT,
) {

    suspend fun exchange(
        code: String,
        codeVerifier: String,
        redirectUri: String,
    ): GoogleIdentity {
        val response = post(code = code, codeVerifier = codeVerifier, redirectUri = redirectUri)

        if (!response.status.isSuccess()) {
            throw response.status.toFailure()
        }

        val idToken = runCatching { response.body<GoogleTokenResponse>().idToken }
            .getOrNull()
            ?.takeIf(String::isNotBlank)
            ?: throw AuthFailure.UnverifiableConfirmation("Token endpoint returned no ID token")

        return googleIdentityVerifier.verify(idToken = idToken, expectedNonce = null)
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun post(
        code: String,
        codeVerifier: String,
        redirectUri: String,
    ): HttpResponse = try {
        httpClient.submitForm(
            url = tokenEndpoint,
            formParameters = parameters {
                append("client_id", googleAuthConfig.androidClientId)
                append("code", code)
                append("code_verifier", codeVerifier)
                append("grant_type", "authorization_code")
                append("redirect_uri", redirectUri)
            },
        )
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (_: Throwable) {
        throw AuthFailure.ProviderUnavailable("Token endpoint unreachable")
    }

    private fun HttpStatusCode.toFailure(): AuthFailure =
        if (this == HttpStatusCode.BadRequest || this == HttpStatusCode.Unauthorized) {
            AuthFailure.UnverifiableConfirmation("Authorization code was refused")
        } else {
            AuthFailure.ProviderUnavailable("Token endpoint answered $value")
        }
}

@Serializable
private data class GoogleTokenResponse(
    @SerialName("id_token") val idToken: String? = null,
)
