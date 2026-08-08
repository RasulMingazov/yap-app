package app.yap.server.core.config

enum class AppEnvironment {
    DEVELOPMENT,
    TEST,
    PRODUCTION;

    companion object {
        fun from(value: String): AppEnvironment = entries
            .firstOrNull { it.name.equals(value, ignoreCase = true) }
            ?: throw IllegalArgumentException("APP_ENV must be one of: development, test, production")
    }
}
