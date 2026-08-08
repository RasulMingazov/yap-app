package app.yap.core.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

interface NetworkContainer {

    val networkClient: NetworkClient
}

internal class DefaultNetworkContainer(
    baseUrl: String,
    engine: HttpClientEngine = platformHttpClientEngine(),
) : NetworkContainer {

    override val networkClient: NetworkClient = createNetworkClient(baseUrl, engine)

    private fun createNetworkClient(baseUrl: String, engine: HttpClientEngine): NetworkClient {
        val normalizedBaseUrl = baseUrl.trimEnd('/') + "/"
        val httpClient = HttpClient(engine) {
            expectSuccess = false
            install(HttpTimeout) {
                requestTimeoutMillis = REQUEST_TIMEOUT_MILLIS
                connectTimeoutMillis = CONNECT_TIMEOUT_MILLIS
                socketTimeoutMillis = REQUEST_TIMEOUT_MILLIS
            }
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true; explicitNulls = false })
            }
        }
        return NetworkClient(
            baseUrl = normalizedBaseUrl.trimEnd('/'),
            httpClient = httpClient,
        )
    }

    private companion object {
        const val REQUEST_TIMEOUT_MILLIS = 15_000L
        const val CONNECT_TIMEOUT_MILLIS = 10_000L
    }
}

fun createNetworkContainer(baseUrl: String): NetworkContainer = DefaultNetworkContainer(baseUrl)
