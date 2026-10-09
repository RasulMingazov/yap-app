package app.yap.feature.scenario.api.entity

data class ScenarioOverview(
    val scenarios: List<Scenario>,
    val lastOpened: Scenario?,
    val slots: SlotUsage,
    val practiceMinutes: Int,
)
