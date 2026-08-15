package app.yap.core.network

import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.http.HttpMethod
import kotlin.coroutines.cancellation.CancellationException

suspend inline fun <reified R> ApiClient.get(
    path: String,
    authenticated: Boolean = true,
    noinline configure: HttpRequestBuilder.() -> Unit = {},
): ApiResult<R> = call(HttpMethod.Get, path, authenticated, configure).decode()

suspend inline fun <reified R> ApiClient.post(
    path: String,
    authenticated: Boolean = true,
    noinline configure: HttpRequestBuilder.() -> Unit = {},
): ApiResult<R> = call(HttpMethod.Post, path, authenticated, configure).decode()

suspend inline fun <reified R> ApiClient.put(
    path: String,
    authenticated: Boolean = true,
    noinline configure: HttpRequestBuilder.() -> Unit = {},
): ApiResult<R> = call(HttpMethod.Put, path, authenticated, configure).decode()

suspend inline fun <reified R> ApiClient.delete(
    path: String,
    authenticated: Boolean = true,
    noinline configure: HttpRequestBuilder.() -> Unit = {},
): ApiResult<R> = call(HttpMethod.Delete, path, authenticated, configure).decode()

suspend fun ApiClient.send(
    method: HttpMethod,
    path: String,
    authenticated: Boolean = true,
    configure: HttpRequestBuilder.() -> Unit = {},
): ApiResult<Unit> = when (val call = call(method, path, authenticated, configure)) {
    is ApiCall.Failed -> ApiResult.Failure(call.error)
    is ApiCall.Responded -> ApiResult.Success(Unit)
}

@Suppress("TooGenericExceptionCaught")
suspend inline fun <reified R> ApiCall.decode(): ApiResult<R> = when (this) {
    is ApiCall.Failed -> ApiResult.Failure(error)
    is ApiCall.Responded -> try {
        ApiResult.Success(response.body<R>())
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (_: Throwable) {
        ApiResult.Failure(ApiError.Malformed)
    }
}
