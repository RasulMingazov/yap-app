package app.yap.feature.auth.data.remote

import app.yap.contract.auth.LoginChallengeDto
import app.yap.contract.auth.LoginChallengeRequestDto
import app.yap.contract.auth.LoginRequestDto
import app.yap.contract.auth.RefreshRequestDto
import app.yap.contract.auth.SessionDto
import app.yap.core.common.coroutines.runSuspendCatching
import app.yap.core.network.NetworkClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType

private const val CHALLENGE_PATH = "auth/challenge"
private const val LOGIN_PATH = "auth/login"
private const val REFRESH_PATH = "auth/refresh"

internal class DefaultAuthApi(
    private val networkClient: NetworkClient,
) : AuthApi {

    override suspend fun challenge(
        request: LoginChallengeRequestDto,
    ): AuthApiResult<LoginChallengeDto> = post(path = CHALLENGE_PATH, body = request)

    override suspend fun login(request: LoginRequestDto): AuthApiResult<SessionDto> =
        post(path = LOGIN_PATH, body = request)

    override suspend fun refresh(request: RefreshRequestDto): AuthApiResult<SessionDto> =
        post(path = REFRESH_PATH, body = request)

    private suspend inline fun <reified B : Any, reified R : Any> post(
        path: String,
        body: B,
    ): AuthApiResult<R> = runSuspendCatching {
        val response: HttpResponse = networkClient.httpClient.post("${networkClient.baseUrl}/$path") {
            contentType(ContentType.Application.Json)
            setBody(body)
        }
        val failureKind = failureKindOf(response.status.value)
        when (failureKind) {
            null -> AuthApiResult.Success(response.body<R>())
            else -> AuthApiResult.Failure(failureKind)
        }
    }.getOrElse { AuthApiResult.Failure(AuthApiFailureKind.Unavailable) }
}
