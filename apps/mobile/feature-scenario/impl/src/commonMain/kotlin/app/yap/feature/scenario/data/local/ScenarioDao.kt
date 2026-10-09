package app.yap.feature.scenario.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
internal interface ScenarioDao {

    @Query("SELECT * FROM overview WHERE id = ${OverviewDb.SINGLE_ROW_ID}")
    fun observeOverview(): Flow<OverviewDb?>

    @Transaction
    @Query("SELECT * FROM scenario ORDER BY position")
    fun observeScenarios(): Flow<List<ScenarioWithObjectivesDb>>

    @Transaction
    suspend fun replace(overview: OverviewDb, scenarios: List<ScenarioDb>, objectives: List<ObjectiveDb>) {
        clear()
        insertOverview(overview)
        insertScenarios(scenarios)
        insertObjectives(objectives)
    }

    @Transaction
    suspend fun clear() {
        deleteObjectives()
        deleteScenarios()
        deleteOverview()
    }

    @Insert
    suspend fun insertOverview(overview: OverviewDb)

    @Insert
    suspend fun insertScenarios(scenarios: List<ScenarioDb>)

    @Insert
    suspend fun insertObjectives(objectives: List<ObjectiveDb>)

    @Query("DELETE FROM overview")
    suspend fun deleteOverview()

    @Query("DELETE FROM scenario")
    suspend fun deleteScenarios()

    @Query("DELETE FROM objective")
    suspend fun deleteObjectives()
}
