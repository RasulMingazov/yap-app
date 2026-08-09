package app.yap.server.feature.auth.identity

import app.yap.server.feature.auth.model.AuthFailure
import app.yap.server.feature.auth.model.AuthFailureException
import java.io.IOException
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.future.await
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Exchanges an Android browser-fallback authorization code at Google's token endpoint. The
 * fallback uses a public client bound by PKCE, so no client secret is involved.
 *
 * A confirmed rejection of the code or verifier is opaque to the client
 * ([AuthFailure.ChallengeInvalid]); a rate limit, an outage, a transport failure, and an unreadable
 * response are all reported as [AuthFailure.ProviderUnavailable].
 *
 * Nothing here logs the request or the response: both carry the authorization code, the code
 * verifier, and the identity token.
 */
internal class HttpGoogleTokenExchange(
    private val clientId: String,
    private val httpClient: HttpClient = HttpClient.newHttpClient(),
    private val tokenEndpoint: String = GOOGLE_TOKEN_ENDPOINT,
) : GoogleTokenExchange {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun exchange(
        code: String,
        codeVerifier: String,
        redirectUri: String,
    ): String {
        val request = HttpRequest.newBuilder()
            .uri(URI.create(tokenEndpoint))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(
                HttpRequest.BodyPublishers.ofString(
                    formBody(code = code, codeVerifier = codeVerifier, redirectUri = redirectUri),
                ),
            )
            .build()

        return identityToken(send(request))
    }

    /**
     * Only a confirmed credential rejection is the client's problem. Everything else — a rate
     * limit, an outage, a response we cannot read, or a success without an identity token — is a
     * provider or protocol failure, and reporting it as an invalid challenge would blame a client
     * that did nothing wrong.
     */
    private fun identityToken(response: HttpResponse<String>): String {
        val payload = runCatching { json.decodeFromString<TokenResponse>(response.body()) }.getOrNull()
        if (response.statusCode() == HTTP_OK) {
            return payload?.idToken ?: throw AuthFailureException(AuthFailure.ProviderUnavailable)
        }

        throw AuthFailureException(failureOf(payload = payload, statusCode = response.statusCode()))
    }

    private fun failureOf(payload: TokenResponse?, statusCode: Int): AuthFailure {
        val isCredentialRejection = payload?.error == INVALID_GRANT_ERROR &&
            statusCode in CLIENT_ERROR_STATUS_CODES &&
            statusCode != HTTP_TOO_MANY_REQUESTS

        return if (isCredentialRejection) AuthFailure.ChallengeInvalid else AuthFailure.ProviderUnavailable
    }

    /**
     * Sends the request without blocking a thread, so cancelling the login coroutine also abandons
     * the exchange instead of leaving it running to completion.
     */
    private suspend fun send(request: HttpRequest): HttpResponse<String> = try {
        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString()).await()
    } catch (error: IOException) {
        throw AuthFailureException(AuthFailure.ProviderUnavailable, error)
    }

    private fun formBody(code: String, codeVerifier: String, redirectUri: String): String = mapOf(
        "client_id" to clientId,
        "code" to code,
        "code_verifier" to codeVerifier,
        "grant_type" to "authorization_code",
        "redirect_uri" to redirectUri,
    ).entries.joinToString(separator = "&") { (name, value) -> "$name=${value.urlEncoded()}" }

    private fun String.urlEncoded(): String = URLEncoder.encode(this, StandardCharsets.UTF_8)

    @Serializable
    private data class TokenResponse(
        @SerialName("error") val error: String? = null,
        @SerialName("id_token") val idToken: String? = null,
    )

    private companion object {
        const val GOOGLE_TOKEN_ENDPOINT = "https://oauth2.googleapis.com/token"

        /** The one OAuth error that names the presented credential itself as the problem. */
        const val INVALID_GRANT_ERROR = "invalid_grant"

        const val HTTP_OK = 200
        const val HTTP_TOO_MANY_REQUESTS = 429
        val CLIENT_ERROR_STATUS_CODES = 400..499
    }
}
