package app.yap.server.feature.auth.persistence

import javax.sql.DataSource
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.jupiter.api.Assumptions.assumeTrue

internal class AuthMigrationIntegrationTest {

    @Test
    fun `GIVEN an empty database WHEN every migration is applied THEN the three auth tables exist`() {
        assumeDocker()

        val tables = PostgresTestSupport.withDatabase { source ->
            PostgresTestSupport.migrate(source)
            source.tableNames()
        }

        assertEquals(
            expected = setOf("provider_identities", "sessions", "users"),
            actual = tables - "flyway_schema_history",
        )
    }

    @Test
    fun `GIVEN a migrated database WHEN a provider identity is inserted twice THEN the second is refused`() {
        assumeDocker()

        val secondInsertFailed = PostgresTestSupport.withDatabase { source ->
            PostgresTestSupport.migrate(source)
            source.insertUserWithIdentity()
            runCatching { source.insertUserWithIdentity() }.isFailure
        }

        assertEquals(expected = true, actual = secondInsertFailed)
    }

    @Test
    fun `GIVEN a migrated database WHEN indexes are read THEN both user id indexes exist`() {
        assumeDocker()

        val indexed = PostgresTestSupport.withDatabase { source ->
            PostgresTestSupport.migrate(source)
            source.indexedTablesOnUserId()
        }

        assertEquals(expected = setOf("provider_identities", "sessions"), actual = indexed)
    }

    private fun assumeDocker() = assumeTrue(
        PostgresTestSupport.isDockerAvailable,
        "Docker is not available: the PostgreSQL integration suite did not run",
    )

    private fun DataSource.tableNames(): Set<String> = query(
        "select table_name from information_schema.tables where table_schema = 'public'",
    )

    private fun DataSource.indexedTablesOnUserId(): Set<String> = query(
        """
        select distinct t.relname
        from pg_index i
        join pg_class t on t.oid = i.indrelid
        join pg_attribute a on a.attrelid = t.oid and a.attnum = any (i.indkey)
        where a.attname = 'user_id'
        """.trimIndent(),
    )

    private fun DataSource.insertUserWithIdentity() {
        connection.use { connection ->
            connection.autoCommit = false
            connection.createStatement().use { statement ->
                statement.execute(
                    """
                    with created as (insert into users (id) values (gen_random_uuid()) returning id)
                    insert into provider_identities (id, user_id, provider, provider_user_id)
                    select gen_random_uuid(), created.id, 'google', 'sub-1' from created
                    """.trimIndent(),
                )
            }
            connection.commit()
        }
    }

    private fun DataSource.query(sql: String): Set<String> = connection.use { connection ->
        connection.createStatement().use { statement ->
            statement.executeQuery(sql).use { rows ->
                buildSet {
                    while (rows.next()) {
                        add(rows.getString(1))
                    }
                }
            }
        }
    }
}
