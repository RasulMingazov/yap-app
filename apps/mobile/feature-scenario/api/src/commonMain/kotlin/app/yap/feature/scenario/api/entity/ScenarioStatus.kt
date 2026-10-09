package app.yap.feature.scenario.api.entity

/** The user's progress fact; `Scenario.locked` is the orthogonal access fact (research R12). */
sealed interface ScenarioStatus {

    data object Available : ScenarioStatus

    data class Active(val attempt: Int, val currentObjective: Int) : ScenarioStatus

    data object Completed : ScenarioStatus
}
