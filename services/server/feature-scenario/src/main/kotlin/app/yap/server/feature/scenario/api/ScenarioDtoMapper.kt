package app.yap.server.feature.scenario.api

import app.yap.contract.scenario.ObjectiveDto
import app.yap.contract.scenario.ScenarioDto
import app.yap.contract.scenario.ScenarioStateDto
import app.yap.server.feature.scenario.model.ProgressStatus
import app.yap.server.feature.scenario.model.ScenarioState

internal fun ScenarioState.toDto(): ScenarioStateDto = ScenarioStateDto(
    scenarios = scenarios.map { scenario ->
        ScenarioDto(
            id = scenario.id,
            title = scenario.title,
            position = scenario.position,
            free = scenario.free,
            status = when (scenario.status) {
                ProgressStatus.Available -> "available"
                ProgressStatus.Active -> "active"
                ProgressStatus.Completed -> "completed"
            },
            locked = scenario.locked,
            attempt = scenario.attempt,
            currentObjective = scenario.currentObjective,
            openedAtEpochSeconds = scenario.openedAtEpochSeconds,
            objectiveCount = scenario.objectiveCount,
            objectives = scenario.objectives.map { objective ->
                ObjectiveDto(order = objective.order, title = objective.title, achieved = objective.achieved)
            },
        )
    },
    slotsUsed = slotsUsed,
    slotCapacity = slotCapacity,
    streakDays = streakDays,
    practisedDates = practisedDates.map(Any::toString),
    practiceSeconds = practiceSeconds,
)
