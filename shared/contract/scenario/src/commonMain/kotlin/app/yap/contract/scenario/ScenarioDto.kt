package app.yap.contract.scenario

import kotlinx.serialization.Serializable

@Serializable
data class ScenarioDto(
    val id: String,
    val title: String,
    val position: Int,
    val free: Boolean,
    val status: String,
    val locked: Boolean,
    val attempt: Int?,
    val currentObjective: Int?,
    val openedAtEpochSeconds: Long?,
    val objectiveCount: Int,
    val objectives: List<ObjectiveDto>,
)
