package app.yap.core.network

import app.yap.contract.common.ErrorResponseDto
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.request
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlin.coroutines.cancellation.CancellationException

private const val CLIENT_ERROR_MIN = 400
private const val CLIENT_ERROR_MAX = 499

private val TRANSIENT_STATUSES = setOf(
    HttpStatusCode.RequestTimeout,
    HttpStatusCode.TooManyRequests,
)

sealed interface ApiCall {

    class Responded(val response: HttpResponse) : ApiCall

    data class Failed(val error: ApiError) : ApiCall
}

class ApiClient(private val networkClient: NetworkClient) {

    suspend fun call(
        method: HttpMethod,
        path: String,
        authenticated: Boolean,
        configure: HttpRequestBuilder.() -> Unit,
    ): ApiCall {
        val response = execute(
            method = method,
            path = path,
            authenticated = authenticated,
            configure = configure,
        ) ?: return ApiCall.Failed(ApiError.Unavailable)
        if (!response.status.isSuccess()) return ApiCall.Failed(response.toError())

        return ApiCall.Responded(response)
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun execute(
        method: HttpMethod,
        path: String,
        authenticated: Boolean,
        configure: HttpRequestBuilder.() -> Unit,
    ): HttpResponse? = try {
        networkClient.httpClient.request("${networkClient.baseUrl}$path") {
            this.method = method
            contentType(ContentType.Application.Json)
            if (authenticated) authenticated()
            configure()
        }
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (_: Throwable) {
        null
    }

    private suspend fun HttpResponse.toError(): ApiError = when {
        status == HttpStatusCode.Unauthorized || status == HttpStatusCode.Forbidden -> ApiError.Unauthorized
        status in TRANSIENT_STATUSES -> ApiError.Unavailable
        status.value in CLIENT_ERROR_MIN..CLIENT_ERROR_MAX -> ApiError.Rejected(errorCode())
        else -> ApiError.Unavailable
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun HttpResponse.errorCode(): String? = try {
        body<ErrorResponseDto>().error
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (_: Throwable) {
        null
    }
}
