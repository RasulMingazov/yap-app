package app.yap.feature.scenario.api.entity

internal object StubScenarioOverview {

    const val FREE_ID = "cafe-visit"
    const val FREE_TITLE = "Поход в кафе"
    const val LOCKED_ID = "job-interview"
    const val LOCKED_TITLE = "Собеседование"

    fun stubOverview(
        scenarios: List<Scenario> = listOf(stubFreeScenario(), stubLockedScenario()),
        lastOpened: Scenario? = null,
        slots: SlotUsage = SlotUsage(used = 0, capacity = 5),
        practiceMinutes: Int = 0,
    ): ScenarioOverview = ScenarioOverview(
        scenarios = scenarios,
        lastOpened = lastOpened,
        slots = slots,
        practiceMinutes = practiceMinutes,
    )

    fun stubFreeScenario(
        id: String = FREE_ID,
        title: String = FREE_TITLE,
        status: ScenarioStatus = ScenarioStatus.Available,
        locked: Boolean = false,
        objectiveCount: Int = 6,
        objectives: List<Objective> = emptyList(),
    ): Scenario = Scenario(
        id = ScenarioId(id),
        title = title,
        isFree = true,
        status = status,
        locked = locked,
        objectiveCount = objectiveCount,
        objectives = objectives,
    )

    fun stubLockedScenario(
        id: String = LOCKED_ID,
        title: String = LOCKED_TITLE,
        status: ScenarioStatus = ScenarioStatus.Available,
        locked: Boolean = true,
        objectiveCount: Int = 6,
        objectives: List<Objective> = emptyList(),
    ): Scenario = Scenario(
        id = ScenarioId(id),
        title = title,
        isFree = false,
        status = status,
        locked = locked,
        objectiveCount = objectiveCount,
        objectives = objectives,
    )

    fun stubActiveScenario(
        id: String = FREE_ID,
        title: String = FREE_TITLE,
        attempt: Int = 1,
        currentObjective: Int = 2,
        objectives: List<Objective> = listOf(
            Objective(order = 1, title = "Заказ у стойки", achieved = true),
            Objective(order = 2, title = "Вопрос о меню", achieved = false),
            Objective(order = 3, title = "Просьба заменить ингредиент", achieved = false),
        ),
    ): Scenario = stubFreeScenario(
        id = id,
        title = title,
        status = ScenarioStatus.Active(attempt = attempt, currentObjective = currentObjective),
        objectiveCount = objectives.size,
        objectives = objectives,
    )
}
