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
import app.yap.feature.scenario.api.entity.Streak
import app.yap.feature.scenario.api.entity.WeekDay
import app.yap.feature.scenario.data.IsoDates

private const val STATUS_ACTIVE = "active"
private const val STATUS_COMPLETED = "completed"
private const val SECONDS_PER_MINUTE = 60L
private const val WEEK_LENGTH = 7

internal fun ScenarioStateDto.toDomain(todayIsoDate: String): ScenarioOverview {
    val mapped = scenarios.map(ScenarioDto::toDomain)
    val lastOpenedId = scenarios
        .filter { scenario -> scenario.status == STATUS_ACTIVE }
        .maxByOrNull { scenario -> scenario.openedAtEpochSeconds ?: 0L }
        ?.id

    return ScenarioOverview(
        scenarios = mapped,
        lastOpened = mapped.firstOrNull { scenario -> scenario.id.value == lastOpenedId },
        slots = SlotUsage(used = slotsUsed, capacity = slotCapacity),
        streak = Streak(
            days = streakDays,
            weekDays = weekDays(todayIsoDate = todayIsoDate, practisedDates = practisedDates.toSet()),
        ),
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

private fun weekDays(todayIsoDate: String, practisedDates: Set<String>): List<WeekDay> {
    val todayEpochDay = IsoDates.toEpochDay(todayIsoDate)
    val monday = todayEpochDay - (IsoDates.isoDayOfWeek(todayEpochDay) - 1)
    return List(WEEK_LENGTH) { offset ->
        val isoDate = IsoDates.fromEpochDay(monday + offset)
        WeekDay(
            isoDate = isoDate,
            practised = isoDate in practisedDates,
            isToday = monday + offset == todayEpochDay,
        )
    }
}
