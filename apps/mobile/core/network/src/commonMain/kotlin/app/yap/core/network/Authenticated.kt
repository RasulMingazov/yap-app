package app.yap.core.network

import io.ktor.client.request.HttpRequestBuilder
import io.ktor.util.AttributeKey

internal val AuthenticatedAttributeKey = AttributeKey<Boolean>("Authenticated")

fun HttpRequestBuilder.authenticated() {
    attributes.put(AuthenticatedAttributeKey, true)
}
