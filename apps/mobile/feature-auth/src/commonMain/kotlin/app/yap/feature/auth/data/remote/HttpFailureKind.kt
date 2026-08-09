package app.yap.feature.auth.data.remote

private const val CLIENT_ERROR_FIRST = 400
private const val SERVER_ERROR_FIRST = 500
private const val SUCCESS_FIRST = 200

/**
 * Translates a status code into the coarse failure the repository reacts to: a client error is a
 * definitive server answer, everything else is transient. `null` means the response is usable.
 */
internal fun failureKindOf(statusCode: Int): AuthApiFailureKind? = when (statusCode) {
    in SUCCESS_FIRST until CLIENT_ERROR_FIRST -> null
    in CLIENT_ERROR_FIRST until SERVER_ERROR_FIRST -> AuthApiFailureKind.Rejected
    else -> AuthApiFailureKind.Unavailable
}
