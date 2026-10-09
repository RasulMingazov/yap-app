package app.yap.feature.scenario.data

import app.yap.contract.scenario.ObjectiveDto
import app.yap.contract.scenario.ScenarioDto
import app.yap.contract.scenario.ScenarioStateDto

internal object StubScenarioState {

    const val FREE_ID = "cafe-visit"
    const val FREE_TITLE = "Поход в кафе"
    const val LOCKED_ID = "job-interview"
    const val LOCKED_TITLE = "Собеседование"

    fun stubStateDto(
        scenarios: List<ScenarioDto> = listOf(stubFreeScenarioDto(), stubLockedScenarioDto()),
        slotsUsed: Int = 0,
        slotCapacity: Int = 5,
        practiceSeconds: Long = 0L,
    ): ScenarioStateDto = ScenarioStateDto(
        scenarios = scenarios,
        slotsUsed = slotsUsed,
        slotCapacity = slotCapacity,
        practiceSeconds = practiceSeconds,
    )

    fun stubFreeScenarioDto(
        id: String = FREE_ID,
        title: String = FREE_TITLE,
        position: Int = 1,
        status: String = "available",
        locked: Boolean = false,
        attempt: Int? = null,
        currentObjective: Int? = null,
        openedAtEpochSeconds: Long? = null,
        objectiveCount: Int = 6,
        objectives: List<ObjectiveDto> = emptyList(),
    ): ScenarioDto = ScenarioDto(
        id = id,
        title = title,
        position = position,
        free = true,
        status = status,
        locked = locked,
        attempt = attempt,
        currentObjective = currentObjective,
        openedAtEpochSeconds = openedAtEpochSeconds,
        objectiveCount = objectiveCount,
        objectives = objectives,
    )

    fun stubLockedScenarioDto(
        id: String = LOCKED_ID,
        title: String = LOCKED_TITLE,
        position: Int = 2,
        status: String = "available",
        locked: Boolean = true,
        attempt: Int? = null,
        currentObjective: Int? = null,
        openedAtEpochSeconds: Long? = null,
        objectiveCount: Int = 6,
        objectives: List<ObjectiveDto> = emptyList(),
    ): ScenarioDto = ScenarioDto(
        id = id,
        title = title,
        position = position,
        free = false,
        status = status,
        locked = locked,
        attempt = attempt,
        currentObjective = currentObjective,
        openedAtEpochSeconds = openedAtEpochSeconds,
        objectiveCount = objectiveCount,
        objectives = objectives,
    )

    fun stubActiveScenarioDto(
        id: String = FREE_ID,
        title: String = FREE_TITLE,
        position: Int = 1,
        attempt: Int = 1,
        currentObjective: Int = 2,
        openedAtEpochSeconds: Long = 1_000L,
        objectives: List<ObjectiveDto> = listOf(
            ObjectiveDto(order = 1, title = "Заказ у стойки", achieved = true),
            ObjectiveDto(order = 2, title = "Вопрос о меню", achieved = false),
        ),
    ): ScenarioDto = stubFreeScenarioDto(
        id = id,
        title = title,
        position = position,
        status = "active",
        attempt = attempt,
        currentObjective = currentObjective,
        openedAtEpochSeconds = openedAtEpochSeconds,
        objectiveCount = objectives.size,
        objectives = objectives,
    )
}
