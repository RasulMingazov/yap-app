package app.yap.server.app

import app.yap.server.core.config.DatabaseConfig
import app.yap.server.core.database.DatabaseFactory
import org.testcontainers.DockerClientFactory
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName

internal object PostgresTestSupport {

    private const val IMAGE = "postgres:17-alpine"

    val isDockerAvailable: Boolean by lazy {
        runCatching { DockerClientFactory.instance().isDockerAvailable }.getOrDefault(false)
    }

    private val container: PostgreSQLContainer<*> by lazy {
        PostgreSQLContainer(DockerImageName.parse(IMAGE)).apply { start() }
    }

    private var isInitialized = false

    @Synchronized
    fun connect() {
        if (isInitialized) return
        isInitialized = true

        DatabaseFactory.init(
            DatabaseConfig(
                url = container.jdbcUrl,
                user = container.username,
                password = container.password,
                poolSize = POOL_SIZE,
            ),
        )
    }

    private const val POOL_SIZE = 4
}
