package app.yap.server.feature.scenario.model

internal data class ScenarioState(
    val scenarios: List<ScenarioSnapshot>,
    val slotsUsed: Int,
    val slotCapacity: Int,
    val practiceSeconds: Long,
)

internal data class ScenarioSnapshot(
    val id: String,
    val title: String,
    val position: Int,
    val free: Boolean,
    val status: ProgressStatus,
    val locked: Boolean,
    val attempt: Int?,
    val currentObjective: Int?,
    val openedAtEpochSeconds: Long?,
    val objectiveCount: Int,
    val objectives: List<ObjectiveSnapshot>,
)

internal data class ObjectiveSnapshot(
    val order: Int,
    val title: String,
    val achieved: Boolean,
)

internal enum class ProgressStatus { Available, Active, Completed }
