package app.yap.feature.scenario.data.mapper

import app.yap.contract.scenario.ObjectiveDto
import app.yap.contract.scenario.ScenarioDto
import app.yap.contract.scenario.ScenarioStateDto
import app.yap.feature.scenario.data.local.ObjectiveDb
import app.yap.feature.scenario.data.local.OverviewDb
import app.yap.feature.scenario.data.local.ScenarioDb

internal fun ScenarioStateDto.toDb(accountId: String): OverviewDb = OverviewDb(
    accountId = accountId,
    slotsUsed = slotsUsed,
    slotCapacity = slotCapacity,
    practiceSeconds = practiceSeconds,
)

internal fun ScenarioDto.toDb(): ScenarioDb = ScenarioDb(
    id = id,
    title = title,
    position = position,
    isFree = free,
    status = status,
    locked = locked,
    attempt = attempt,
    currentObjective = currentObjective,
    openedAtEpochSeconds = openedAtEpochSeconds,
    objectiveCount = objectiveCount,
)

internal fun ObjectiveDto.toDb(scenarioId: String): ObjectiveDb = ObjectiveDb(
    scenarioId = scenarioId,
    objectiveOrder = order,
    title = title,
    achieved = achieved,
)
