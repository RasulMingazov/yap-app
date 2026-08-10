package app.yap.server.feature.auth.persistence

import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * There is no earlier supported schema yet, so the migrations are verified by bootstrapping every
 * one of them into an empty database and querying each declared column back.
 */
@Suppress("FunctionNaming")
class AuthMigrationIntegrationTest {

    @BeforeTest
    fun clearDatabase() {
        AuthDatabase.clear()
    }

    @Test
    fun `GIVEN an empty database WHEN every migration runs THEN each authentication table is queryable`() {
        val rows = transaction(AuthDatabase.database) {
            listOf(
                AccountTable.selectAll().count(),
                ChallengeTable.selectAll().count(),
                ProviderIdentityTable.selectAll().count(),
                SessionTable.selectAll().count(),
            )
        }

        assertEquals(expected = listOf(0L, 0L, 0L, 0L), actual = rows)
    }
}
