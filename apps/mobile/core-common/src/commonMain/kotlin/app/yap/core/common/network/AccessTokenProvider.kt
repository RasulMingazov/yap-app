package app.yap.core.common.network

interface AccessTokenProvider {

    suspend fun getAccessToken(rejectedAccessToken: String?): String?
}
