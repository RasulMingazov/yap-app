package app.yap.core.network

sealed interface ApiResult<out T> {

    data class Success<out T>(val value: T) : ApiResult<T>

    data class Failure(val error: ApiError) : ApiResult<Nothing>
}

sealed interface ApiError {

    data class Rejected(val code: String?) : ApiError

    data object Unauthorized : ApiError

    data object Unavailable : ApiError

    data object Malformed : ApiError
}
