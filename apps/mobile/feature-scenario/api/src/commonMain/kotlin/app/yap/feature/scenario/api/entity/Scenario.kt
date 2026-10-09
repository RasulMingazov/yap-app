package app.yap.feature.scenario.api.entity

data class Scenario(
    val id: ScenarioId,
    val title: String,
    val isFree: Boolean,
    val status: ScenarioStatus,
    val locked: Boolean,
    val objectiveCount: Int,
    val objectives: List<Objective>,
)
