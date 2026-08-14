package app.yap.server.core.config

import java.nio.file.Files
import java.nio.file.Path

object AppConfigLoader {
    private val dotenv: Map<String, String> by lazy(::loadDotEnv)

    fun load(): AppConfig {
        val environment = environment()
        val database = DatabaseConfig(
            url = env("DATABASE_URL") ?: "jdbc:postgresql://localhost:5432/yap",
            user = env("DATABASE_USER") ?: "postgres",
            password = env("DATABASE_PASSWORD") ?: "postgres",
            poolSize = intEnv("DATABASE_POOL_SIZE", default = 10),
        ).also { it.validateFor(environment) }
        val auth = AuthConfig(
            jwtSecret = env("JWT_SECRET").orEmpty(),
            jwtIssuer = env("JWT_ISSUER") ?: "yap-backend",
            jwtAudience = env("JWT_AUDIENCE") ?: "yap-mobile",
            accessTokenTtlSeconds = longEnv("ACCESS_TOKEN_TTL_SECONDS", default = 900),
            refreshTokenTtlSeconds = longEnv("REFRESH_TOKEN_TTL_SECONDS", default = 7_776_000),
        )
        return AppConfig(
            port = intEnv("PORT", default = 8080),
            trustProxyHeaders = booleanEnv("TRUST_PROXY_HEADERS", default = false),
            authRateLimitRequestsPerMinute = intEnv(
                "AUTH_RATE_LIMIT_REQUESTS_PER_MINUTE",
                default = 100,
            ),
            googleAndroidClientId = env("GOOGLE_ANDROID_CLIENT_ID").orEmpty(),
            googleIosClientId = env("GOOGLE_IOS_CLIENT_ID").orEmpty(),
            googleWebClientId = env("GOOGLE_WEB_CLIENT_ID").orEmpty(),
            database = database,
            auth = auth,
        )
    }

    private fun environment(): AppEnvironment {
        val value = env("APP_ENV") ?: "development"
        return AppEnvironment.from(value)
    }

    private fun env(name: String): String? = System.getenv(name) ?: dotenv[name]

    private fun intEnv(name: String, default: Int): Int = env(name)?.let { value ->
        value.toIntOrNull() ?: throw IllegalArgumentException("$name must be an integer")
    } ?: default

    private fun longEnv(name: String, default: Long): Long = env(name)?.let { value ->
        value.toLongOrNull() ?: throw IllegalArgumentException("$name must be an integer")
    } ?: default

    private fun booleanEnv(name: String, default: Boolean): Boolean = env(name)?.let { value ->
        value.trim().lowercase().toBooleanStrictOrNull()
            ?: throw IllegalArgumentException("$name must be true or false")
    } ?: default

    private fun loadDotEnv(): Map<String, String> {
        val path = Path.of(".env")
        if (!Files.exists(path)) return emptyMap()
        return Files.readAllLines(path)
            .asSequence()
            .map(String::trim)
            .filter { it.isNotEmpty() && !it.startsWith('#') && '=' in it }
            .associate { line ->
                val separator = line.indexOf('=')
                line.substring(0, separator).trim() to
                    line.substring(separator + 1).trim().removeSurrounding("\"")
            }
    }
}
