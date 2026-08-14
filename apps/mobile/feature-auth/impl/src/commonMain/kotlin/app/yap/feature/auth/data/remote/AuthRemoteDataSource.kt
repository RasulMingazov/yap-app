package app.yap.feature.auth.data.remote

import app.yap.contract.auth.GoogleAuthorizationCodeDto
import app.yap.contract.auth.GoogleCredentialsDto
import app.yap.contract.auth.RefreshCredentialsDto
import app.yap.contract.auth.SessionDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlin.coroutines.cancellation.CancellationException

private const val GOOGLE_PATH = "/v1/auth/google"
private const val GOOGLE_CODE_PATH = "/v1/auth/google/code"
private const val REFRESH_PATH = "/v1/auth/refresh"

internal sealed class AuthRemoteFailure(message: String) : RuntimeException(message) {

    class Rejected : AuthRemoteFailure("The server refused the request")

    class Unavailable : AuthRemoteFailure("The server could not be reached")
}

internal interface AuthRemoteDataSource {

    suspend fun refresh(credentials: RefreshCredentialsDto): SessionDto

    suspend fun loginWithGoogleAuthorizationCode(code: GoogleAuthorizationCodeDto): SessionDto

    suspend fun loginWithGoogleIdToken(credentials: GoogleCredentialsDto): SessionDto
}

internal class DefaultAuthRemoteDataSource(
    private val baseUrl: String,
    private val httpClient: HttpClient,
) : AuthRemoteDataSource {

    override suspend fun refresh(credentials: RefreshCredentialsDto): SessionDto =
        postForSession(path = REFRESH_PATH, body = credentials)

    override suspend fun loginWithGoogleAuthorizationCode(code: GoogleAuthorizationCodeDto): SessionDto =
        postForSession(path = GOOGLE_CODE_PATH, body = code)

    override suspend fun loginWithGoogleIdToken(credentials: GoogleCredentialsDto): SessionDto =
        postForSession(path = GOOGLE_PATH, body = credentials)

    private suspend inline fun <reified T : Any> postForSession(path: String, body: T): SessionDto {
        val response = post(path = path, body = body)

        if (!response.status.isSuccess()) {
            throw response.status.toFailure()
        }
        return runCatching { response.body<SessionDto>() }.getOrElse { throw AuthRemoteFailure.Unavailable() }
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend inline fun <reified T : Any> post(path: String, body: T): HttpResponse = try {
        httpClient.post("$baseUrl$path") {
            contentType(ContentType.Application.Json)
            setBody(body)
        }
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (_: Throwable) {
        throw AuthRemoteFailure.Unavailable()
    }

    private fun HttpStatusCode.toFailure(): AuthRemoteFailure =
        if (this == HttpStatusCode.BadRequest || this == HttpStatusCode.Unauthorized) {
            AuthRemoteFailure.Rejected()
        } else {
            AuthRemoteFailure.Unavailable()
        }
}
