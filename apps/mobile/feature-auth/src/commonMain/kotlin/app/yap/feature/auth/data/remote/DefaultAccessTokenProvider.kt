package app.yap.feature.auth.data.remote

import app.yap.core.common.network.AccessTokenProvider
import app.yap.feature.auth.data.repository.SessionCredentials

/**
 * Supplies the token for `authenticated()` requests. A rejected token triggers one silent refresh;
 * returning `null` stops the shared modifier from retrying again (R-055, AC-022).
 */
internal class DefaultAccessTokenProvider(
    private val sessionCredentials: SessionCredentials,
) : AccessTokenProvider {

    override suspend fun getAccessToken(rejectedAccessToken: String?): String? {
        val rejected = rejectedAccessToken ?: return sessionCredentials.accessToken()
        return sessionCredentials.refreshedAccessToken(rejectedAccessToken = rejected)
    }
}
