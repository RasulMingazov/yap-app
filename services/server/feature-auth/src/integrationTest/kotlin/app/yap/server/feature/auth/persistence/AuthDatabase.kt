package app.yap.server.feature.auth.persistence

import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName

/**
 * One PostgreSQL container for the whole suite, on the same major version the deployment runs.
 * Every migration the feature owns is applied to the empty database from the same classpath
 * location the application uses, so the schema under test is the schema that ships.
 */
internal object AuthDatabase {

    val NOW: Instant = Instant.parse("2026-08-09T12:00:00Z")

    /** A fixed clock, so a persisted timestamp is an expected value rather than an approximation. */
    val clock: Clock = Clock.fixed(NOW, ZoneOffset.UTC)

    val database: Database by lazy(::bootstrap)

    fun repository(): AuthRepository = ExposedAuthRepository(clock = clock, database = database)

    /**
     * Isolation is by deletion rather than by rollback: these scenarios span several connections
     * and several committed transactions.
     */
    fun clear() {
        transaction(database) {
            SessionTable.deleteAll()
            ProviderIdentityTable.deleteAll()
            AccountTable.deleteAll()
            ChallengeTable.deleteAll()
        }
    }

    private fun bootstrap(): Database {
        val container = PostgreSQLContainer<Nothing>(DockerImageName.parse(System.getProperty(IMAGE_PROPERTY)))
        container.start()
        Flyway.configure()
            .dataSource(container.jdbcUrl, container.username, container.password)
            .locations(MIGRATION_LOCATION)
            .validateMigrationNaming(true)
            .load()
            .migrate()

        return Database.connect(
            url = container.jdbcUrl,
            driver = "org.postgresql.Driver",
            user = container.username,
            password = container.password,
        )
    }

    private const val IMAGE_PROPERTY = "yap.postgres.image"
    private const val MIGRATION_LOCATION = "classpath:db/migration"
}
