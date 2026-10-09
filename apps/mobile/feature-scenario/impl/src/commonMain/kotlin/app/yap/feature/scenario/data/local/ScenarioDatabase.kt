package app.yap.feature.scenario.data.local

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import org.koin.core.scope.Scope

internal const val SCENARIO_DATABASE_NAME = "yap_scenario.db"

@Database(entities = [OverviewDb::class, ScenarioDb::class, ObjectiveDb::class], version = 1, exportSchema = false)
@ConstructedBy(ScenarioDatabaseConstructor::class)
internal abstract class ScenarioDatabase : RoomDatabase() {

    abstract fun scenarioDao(): ScenarioDao
}

@Suppress("NO_ACTUAL_FOR_EXPECT", "KotlinNoActualForExpect")
internal expect object ScenarioDatabaseConstructor : RoomDatabaseConstructor<ScenarioDatabase> {

    override fun initialize(): ScenarioDatabase
}

internal expect fun Scope.createScenarioDatabase(): ScenarioDatabase

internal fun RoomDatabase.Builder<ScenarioDatabase>.buildScenarioDatabase(): ScenarioDatabase =
    setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
