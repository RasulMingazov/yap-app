package app.yap.feature.scenario.api.entity

sealed interface ScenarioStatus {

    data object Available : ScenarioStatus

    data class Active(val attempt: Int, val currentObjective: Int) : ScenarioStatus

    data object Completed : ScenarioStatus
}
