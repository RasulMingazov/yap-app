package app.yap.core.network

import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.plugin
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode

fun NetworkClient.installAccessTokenModifier(
    getAccessToken: suspend (rejectedAccessToken: String?) -> String?,
) {
    httpClient.plugin(HttpSend).intercept { request ->
        if (request.attributes.getOrNull(AuthenticatedAttributeKey) != true) {
            return@intercept execute(request)
        }

        val accessToken = getAccessToken(null)
            ?: return@intercept execute(request)
        request.headers[HttpHeaders.Authorization] = accessToken.toBearerToken()

        val call = execute(request)
        if (call.response.status != HttpStatusCode.Unauthorized) return@intercept call

        val refreshedAccessToken = getAccessToken(accessToken)
            ?: return@intercept call
        request.headers[HttpHeaders.Authorization] = refreshedAccessToken.toBearerToken()
        execute(request)
    }
}

private fun String.toBearerToken(): String = "Bearer $this"
