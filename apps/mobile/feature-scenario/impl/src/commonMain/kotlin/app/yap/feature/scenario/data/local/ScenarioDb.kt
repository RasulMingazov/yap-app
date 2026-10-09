package app.yap.feature.scenario.data.local

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "overview")
internal data class OverviewDb(
    @PrimaryKey val id: Int = SINGLE_ROW_ID,
    val accountId: String,
    val slotsUsed: Int,
    val slotCapacity: Int,
    val practiceSeconds: Long,
) {

    companion object {
        const val SINGLE_ROW_ID = 0
    }
}

@Entity(tableName = "scenario")
internal data class ScenarioDb(
    @PrimaryKey val id: String,
    val title: String,
    val position: Int,
    val isFree: Boolean,
    val status: String,
    val locked: Boolean,
    val attempt: Int?,
    val currentObjective: Int?,
    val openedAtEpochSeconds: Long?,
    val objectiveCount: Int,
)

@Entity(tableName = "objective", primaryKeys = ["scenarioId", "objectiveOrder"])
internal data class ObjectiveDb(
    val scenarioId: String,
    val objectiveOrder: Int,
    val title: String,
    val achieved: Boolean,
)

internal data class ScenarioWithObjectivesDb(
    @Embedded val scenario: ScenarioDb,
    @Relation(parentColumn = "id", entityColumn = "scenarioId") val objectives: List<ObjectiveDb>,
)
