package app.yap.server.core.config

data class AppConfig(
    val port: Int,
    val trustProxyHeaders: Boolean,
    val authRateLimitRequestsPerMinute: Int,
    val googleAndroidClientId: String,
    val googleIosClientId: String,
    val googleWebClientId: String,
    val database: DatabaseConfig,
    val auth: AuthConfig,
) {
    init {
        require(port in MIN_PORT..MAX_PORT) { "PORT must be between $MIN_PORT and $MAX_PORT" }
        require(authRateLimitRequestsPerMinute >= MIN_RATE_LIMIT) {
            "AUTH_RATE_LIMIT_REQUESTS_PER_MINUTE must be at least $MIN_RATE_LIMIT"
        }
    }

    private companion object {
        const val MIN_PORT = 1
        const val MAX_PORT = 65535
        const val MIN_RATE_LIMIT = 1
    }
}
