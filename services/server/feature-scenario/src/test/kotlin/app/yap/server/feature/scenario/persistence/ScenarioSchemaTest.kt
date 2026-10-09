package app.yap.server.feature.scenario.persistence

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue

internal class ScenarioSchemaTest {

    @Test
    fun `GIVEN all migrations WHEN the database is bootstrapped THEN the seed holds 20 scenarios in position order with one free`() {
        assumeTrue(PostgresTestSupport.isDockerAvailable, "Docker is unavailable — integration suite not run")

        PostgresTestSupport.withDatabase { source ->
            PostgresTestSupport.migrate(source)

            source.connection.use { connection ->
                val positions = mutableListOf<Int>()
                var freeCount = 0
                connection.createStatement().use { statement ->
                    statement.executeQuery("select position, is_free from scenario order by position").use { rows ->
                        while (rows.next()) {
                            positions += rows.getInt("position")
                            if (rows.getBoolean("is_free")) freeCount++
                        }
                    }
                }

                assertEquals(expected = (1..20).toList(), actual = positions)
                assertEquals(expected = 1, actual = freeCount)
            }
        }
    }

    @Test
    fun `GIVEN the seed WHEN objectives are counted per scenario THEN each scenario has 5 or 6 sequential objectives`() {
        assumeTrue(PostgresTestSupport.isDockerAvailable, "Docker is unavailable — integration suite not run")

        PostgresTestSupport.withDatabase { source ->
            PostgresTestSupport.migrate(source)

            source.connection.use { connection ->
                val counts = mutableMapOf<String, Pair<Int, Int>>()
                connection.createStatement().use { statement ->
                    statement.executeQuery(
                        "select scenario_id, count(*) as total, max(objective_order) as last " +
                            "from scenario_objective group by scenario_id",
                    ).use { rows ->
                        while (rows.next()) {
                            counts[rows.getString("scenario_id")] =
                                rows.getInt("total") to rows.getInt("last")
                        }
                    }
                }

                assertEquals(expected = 20, actual = counts.size)
                counts.forEach { (scenarioId, totals) ->
                    val (total, last) = totals
                    assertTrue(total in 5..6, "scenario $scenarioId has $total objectives")
                    assertEquals(expected = total, actual = last, "scenario $scenarioId objectives are not sequential")
                }
            }
        }
    }
}
