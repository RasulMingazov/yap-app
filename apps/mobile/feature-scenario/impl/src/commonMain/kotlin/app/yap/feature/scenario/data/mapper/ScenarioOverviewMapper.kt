package app.yap.feature.scenario.data.mapper

import app.yap.contract.scenario.ObjectiveDto
import app.yap.contract.scenario.ScenarioDto
import app.yap.contract.scenario.ScenarioStateDto
import app.yap.feature.scenario.api.entity.Objective
import app.yap.feature.scenario.api.entity.Scenario
import app.yap.feature.scenario.api.entity.ScenarioId
import app.yap.feature.scenario.api.entity.ScenarioOverview
import app.yap.feature.scenario.api.entity.ScenarioStatus
import app.yap.feature.scenario.api.entity.SlotUsage

private const val STATUS_ACTIVE = "active"
private const val STATUS_COMPLETED = "completed"
private const val SECONDS_PER_MINUTE = 60L

internal fun ScenarioStateDto.toDomain(): ScenarioOverview {
    val mapped = scenarios.map(ScenarioDto::toDomain)
    val lastOpenedId = scenarios
        .filter { scenario -> scenario.status == STATUS_ACTIVE }
        .maxByOrNull { scenario -> scenario.openedAtEpochSeconds ?: 0L }
        ?.id

    return ScenarioOverview(
        scenarios = mapped,
        lastOpened = mapped.firstOrNull { scenario -> scenario.id.value == lastOpenedId },
        slots = SlotUsage(used = slotsUsed, capacity = slotCapacity),
        practiceMinutes = (practiceSeconds / SECONDS_PER_MINUTE).toInt(),
    )
}

private fun ScenarioDto.toDomain(): Scenario = Scenario(
    id = ScenarioId(id),
    title = title,
    isFree = free,
    status = when (status) {
        STATUS_ACTIVE -> ScenarioStatus.Active(
            attempt = attempt ?: 1,
            currentObjective = currentObjective ?: 1,
        )
        STATUS_COMPLETED -> ScenarioStatus.Completed
        else -> ScenarioStatus.Available
    },
    locked = locked,
    objectiveCount = objectiveCount,
    objectives = objectives.map(ObjectiveDto::toDomain),
)

private fun ObjectiveDto.toDomain(): Objective = Objective(
    order = order,
    title = title,
    achieved = achieved,
)
