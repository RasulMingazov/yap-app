package app.yap.core.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

private const val REQUEST_TIMEOUT_MILLIS = 15_000L
private const val CONNECT_TIMEOUT_MILLIS = 10_000L

class NetworkClient internal constructor(
    val baseUrl: String,
    val httpClient: HttpClient,
) : AutoCloseable {

    override fun close() {
        httpClient.close()
    }
}

internal expect fun platformHttpClientEngine(): HttpClientEngine

internal fun createNetworkClient(
    baseUrl: String,
    engine: HttpClientEngine,
): NetworkClient {
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
        baseUrl = baseUrl.trimEnd('/'),
        httpClient = httpClient,
    )
}
