package app.yap.server.feature.auth.model

/**
 * Carries an expected [AuthFailure] out of the scenario that detected it. [cause] stays on the
 * server for diagnostics; only [AuthFailure.code] reaches the client.
 */
internal class AuthFailureException(
    val failure: AuthFailure,
    cause: Throwable? = null,
) : RuntimeException(failure.code, cause)
