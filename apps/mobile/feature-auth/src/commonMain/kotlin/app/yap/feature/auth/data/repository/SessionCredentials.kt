package app.yap.feature.auth.data.repository

/**
 * Data-layer access to the stored session credentials. It exists so credentials never appear on a
 * domain port while the network layer can still attach and refresh them (R-051, R-055).
 */
internal interface SessionCredentials {

    suspend fun accessToken(): String?

    /**
     * Refreshes once for [rejectedAccessToken] and returns the rotated access token, or `null`
     * when no usable token can be produced. Concurrent callers share one in-flight refresh.
     */
    suspend fun refreshedAccessToken(rejectedAccessToken: String): String?
}
