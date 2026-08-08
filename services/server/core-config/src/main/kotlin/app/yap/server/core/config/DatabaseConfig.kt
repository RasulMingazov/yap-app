package app.yap.server.core.config

data class DatabaseConfig(
    val url: String,
    val user: String,
    val password: String,
    val poolSize: Int,
) {
    init {
        require(url.startsWith("jdbc:postgresql://")) { "DATABASE_URL must be a PostgreSQL JDBC URL" }
        require(user.isNotBlank()) { "DATABASE_USER must not be blank" }
        require(poolSize in MIN_POOL_SIZE..MAX_POOL_SIZE) {
            "DATABASE_POOL_SIZE must be between $MIN_POOL_SIZE and $MAX_POOL_SIZE"
        }
    }

    fun validateFor(environment: AppEnvironment) {
        if (environment == AppEnvironment.PRODUCTION) {
            require(password.isNotBlank() && password != "postgres") {
                "DATABASE_PASSWORD must be configured for production"
            }
        }
    }

    private companion object {
        const val MIN_POOL_SIZE = 1
        const val MAX_POOL_SIZE = 100
    }
}
