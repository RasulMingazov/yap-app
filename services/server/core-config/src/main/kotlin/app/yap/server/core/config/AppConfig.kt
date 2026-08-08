package app.yap.server.core.config

data class AppConfig(
    val port: Int,
    val trustProxyHeaders: Boolean,
    val database: DatabaseConfig,
    val auth: AuthConfig,
) {
    init {
        require(port in MIN_PORT..MAX_PORT) { "PORT must be between $MIN_PORT and $MAX_PORT" }
    }

    private companion object {
        const val MIN_PORT = 1
        const val MAX_PORT = 65535
    }
}
