package app.yap.feature.scenario.data.mapper

import app.yap.feature.scenario.api.entity.Objective
import app.yap.feature.scenario.api.entity.Scenario
import app.yap.feature.scenario.api.entity.ScenarioId
import app.yap.feature.scenario.api.entity.ScenarioOverview
import app.yap.feature.scenario.api.entity.ScenarioStatus
import app.yap.feature.scenario.api.entity.SlotUsage
import app.yap.feature.scenario.data.local.ObjectiveDb
import app.yap.feature.scenario.data.local.OverviewDb
import app.yap.feature.scenario.data.local.ScenarioWithObjectivesDb

private const val STATUS_ACTIVE = "active"
private const val STATUS_COMPLETED = "completed"
private const val SECONDS_PER_MINUTE = 60L

internal fun OverviewDb.toDomain(scenarios: List<ScenarioWithObjectivesDb>): ScenarioOverview {
    val mapped = scenarios.map(ScenarioWithObjectivesDb::toDomain)
    val lastOpenedId = scenarios
        .filter { row -> row.scenario.status == STATUS_ACTIVE }
        .maxByOrNull { row -> row.scenario.openedAtEpochSeconds ?: 0L }
        ?.scenario
        ?.id

    return ScenarioOverview(
        scenarios = mapped,
        lastOpened = mapped.firstOrNull { scenario -> scenario.id.value == lastOpenedId },
        slots = SlotUsage(used = slotsUsed, capacity = slotCapacity),
        practiceMinutes = (practiceSeconds / SECONDS_PER_MINUTE).toInt(),
    )
}

private fun ScenarioWithObjectivesDb.toDomain(): Scenario = Scenario(
    id = ScenarioId(scenario.id),
    title = scenario.title,
    isFree = scenario.isFree,
    status = when (scenario.status) {
        STATUS_ACTIVE -> ScenarioStatus.Active(
            attempt = scenario.attempt ?: 1,
            currentObjective = scenario.currentObjective ?: 1,
        )
        STATUS_COMPLETED -> ScenarioStatus.Completed
        else -> ScenarioStatus.Available
    },
    locked = scenario.locked,
    objectiveCount = scenario.objectiveCount,
    objectives = objectives.sortedBy(ObjectiveDb::objectiveOrder).map(ObjectiveDb::toDomain),
)

private fun ObjectiveDb.toDomain(): Objective = Objective(
    order = objectiveOrder,
    title = title,
    achieved = achieved,
)
