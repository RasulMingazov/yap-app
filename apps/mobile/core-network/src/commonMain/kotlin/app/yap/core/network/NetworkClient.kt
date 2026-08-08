package app.yap.core.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine

class NetworkClient internal constructor(
    val baseUrl: String,
    val httpClient: HttpClient,
) : AutoCloseable {

    override fun close() {
        httpClient.close()
    }
}

internal expect fun platformHttpClientEngine(): HttpClientEngine
