package app.yap.server.core.database

import app.yap.server.core.config.DatabaseConfig
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.net.URI
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database
import org.slf4j.LoggerFactory

object DatabaseFactory : AutoCloseable {
    private val logger = LoggerFactory.getLogger(DatabaseFactory::class.java)

    @Volatile
    private var dataSource: HikariDataSource? = null

    @Synchronized
    @Suppress("TooGenericExceptionCaught")
    fun init(config: DatabaseConfig) {
        check(dataSource == null) { "Database has already been initialized" }
        logger.info("Initializing PostgreSQL connection to {}", config.url.toDatabaseEndpoint())
        val source = HikariDataSource(
            HikariConfig().apply {
                jdbcUrl = config.url
                username = config.user
                password = config.password
                driverClassName = "org.postgresql.Driver"
                maximumPoolSize = config.poolSize
                minimumIdle = 1
                connectionTimeout = 5_000
                validationTimeout = 2_000
                idleTimeout = 60_000
                maxLifetime = 30 * 60_000
                isAutoCommit = false
                transactionIsolation = "TRANSACTION_READ_COMMITTED"
                poolName = "yap-db"
                addDataSourceProperty("connectTimeout", "5")
                addDataSourceProperty("socketTimeout", "30")
                addDataSourceProperty("tcpKeepAlive", "true")
            },
        )

        try {
            Flyway.configure()
                .dataSource(source)
                .locations("classpath:db/migration")
                .validateMigrationNaming(true)
                .load()
                .migrate()
            Database.connect(source)
            dataSource = source
        } catch (error: Throwable) {
            source.close()
            throw error
        }
    }

    @Suppress("TooGenericExceptionCaught")
    suspend fun isReady(): Boolean = withContext(Dispatchers.IO) {
        try {
            dataSource?.connection?.use { connection ->
                connection.prepareStatement("SELECT 1").use { statement ->
                    statement.queryTimeout = 2
                    statement.executeQuery().use { it.next() && it.getInt(1) == 1 }
                }
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            false
        } == true
    }

    @Synchronized
    override fun close() {
        dataSource?.close()
        dataSource = null
    }

    private fun String.toDatabaseEndpoint(): String = runCatching {
        val uri = URI(removePrefix("jdbc:"))
        val host = uri.host ?: "unknown-host"
        val port = if (uri.port == -1) 5432 else uri.port
        val database = uri.path.removePrefix("/").ifBlank { "unknown-database" }
        "$host:$port/$database"
    }.getOrDefault("unknown-endpoint")
}
