package app.yap.core.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class NetworkClient internal constructor(
    val baseUrl: String,
    val httpClient: HttpClient,
) : AutoCloseable {

    override fun close() {
        httpClient.close()
    }
}

data class NetworkTimeouts(
    val requestMillis: Long = 15_000L,
    val connectMillis: Long = 10_000L,
)

internal expect fun platformHttpClientEngine(): HttpClientEngine

fun createNetworkClient(
    baseUrl: String,
    engine: HttpClientEngine,
    timeouts: NetworkTimeouts? = NetworkTimeouts(),
): NetworkClient {
    val httpClient = HttpClient(engine) {
        expectSuccess = false
        if (timeouts != null) {
            install(HttpTimeout) {
                requestTimeoutMillis = timeouts.requestMillis
                connectTimeoutMillis = timeouts.connectMillis
                socketTimeoutMillis = timeouts.requestMillis
            }
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
