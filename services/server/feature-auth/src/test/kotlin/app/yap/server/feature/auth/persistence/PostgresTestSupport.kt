package app.yap.server.feature.auth.persistence

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.util.concurrent.atomic.AtomicInteger
import javax.sql.DataSource
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database
import org.testcontainers.DockerClientFactory
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName

internal object PostgresTestSupport {

    private const val IMAGE = "postgres:17-alpine"
    private const val POSTGRES_PORT = 5432

    private val databaseCounter = AtomicInteger()

    val isDockerAvailable: Boolean by lazy {
        runCatching { DockerClientFactory.instance().isDockerAvailable }.getOrDefault(false)
    }

    private val container: PostgreSQLContainer<*> by lazy {
        PostgreSQLContainer(DockerImageName.parse(IMAGE)).apply { start() }
    }

    fun <T> withDatabase(block: (DataSource) -> T): T {
        val name = "yap_test_${databaseCounter.incrementAndGet()}"
        container.createConnection("").use { connection ->
            connection.createStatement().use { statement ->
                statement.execute("CREATE DATABASE $name")
            }
        }

        val source = HikariDataSource(
            HikariConfig().apply {
                jdbcUrl = "jdbc:postgresql://${container.host}:${container.getMappedPort(POSTGRES_PORT)}/$name"
                username = container.username
                password = container.password
                driverClassName = "org.postgresql.Driver"
                maximumPoolSize = 8
                isAutoCommit = false
            },
        )
        return source.use(block)
    }

    fun <T> withMigratedDatabase(block: (Database) -> T): T = withDatabase { source ->
        migrate(source)
        block(Database.connect(source))
    }

    fun migrate(source: DataSource) {
        Flyway.configure()
            .dataSource(source)
            .locations("classpath:db/migration")
            .validateMigrationNaming(true)
            .load()
            .migrate()
    }
}
