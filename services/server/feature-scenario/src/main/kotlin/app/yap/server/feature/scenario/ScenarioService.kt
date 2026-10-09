package app.yap.server.feature.scenario

import app.yap.server.feature.scenario.access.AccessPolicy
import app.yap.server.feature.scenario.model.ObjectiveSnapshot
import app.yap.server.feature.scenario.model.ProgressReport
import app.yap.server.feature.scenario.model.ProgressStatus
import app.yap.server.feature.scenario.model.ScenarioSnapshot
import app.yap.server.feature.scenario.model.ScenarioState
import app.yap.server.feature.scenario.persistence.SLOT_CAPACITY
import app.yap.server.feature.scenario.persistence.ScenarioRepository
import app.yap.server.feature.scenario.persistence.StateFacts
import java.time.Clock
import java.time.Instant
import java.util.UUID

internal class ScenarioService(
    private val accessPolicy: AccessPolicy,
    private val clock: Clock,
    private val scenarioRepository: ScenarioRepository,
) {

    fun state(userId: UUID): ScenarioState = compose(facts = scenarioRepository.loadFacts(userId), userId = userId)

    fun activate(userId: UUID, scenarioId: String): ScenarioState {
        scenarioRepository.activate(
            hasAccess = accessPolicy.hasAccess(userId),
            now = Instant.now(clock),
            scenarioId = scenarioId,
            userId = userId,
        )
        return state(userId)
    }

    fun repeat(userId: UUID, scenarioId: String): ScenarioState {
        scenarioRepository.repeat(
            hasAccess = accessPolicy.hasAccess(userId),
            now = Instant.now(clock),
            scenarioId = scenarioId,
            userId = userId,
        )
        return state(userId)
    }

    fun report(userId: UUID, scenarioId: String, report: ProgressReport): ScenarioState {
        scenarioRepository.report(
            now = Instant.now(clock),
            report = report,
            scenarioId = scenarioId,
            userId = userId,
        )
        return state(userId)
    }

    private fun compose(facts: StateFacts, userId: UUID): ScenarioState {
        val hasAccess = accessPolicy.hasAccess(userId)

        val scenarios = facts.scenarios.map { scenario ->
            val progress = facts.progress[scenario.id]
            val status = progress?.status ?: ProgressStatus.Available
            val achieved = facts.achievedObjectives[scenario.id].orEmpty()
            ScenarioSnapshot(
                id = scenario.id,
                title = scenario.title,
                position = scenario.position,
                free = scenario.isFree,
                status = status,
                locked = !scenario.isFree && !hasAccess,
                attempt = progress?.attempt.takeIf { status == ProgressStatus.Active },
                currentObjective = progress?.currentObjective.takeIf { status == ProgressStatus.Active },
                openedAtEpochSeconds = progress?.openedAt?.epochSecond.takeIf { status == ProgressStatus.Active },
                objectiveCount = scenario.objectiveTitles.size,
                objectives = if (status == ProgressStatus.Active) {
                    scenario.objectiveTitles.mapIndexed { index, title ->
                        ObjectiveSnapshot(order = index + 1, title = title, achieved = (index + 1) in achieved)
                    }
                } else {
                    emptyList()
                },
            )
        }

        return ScenarioState(
            scenarios = scenarios,
            slotsUsed = facts.progress.values.count { fact -> fact.status == ProgressStatus.Active },
            slotCapacity = SLOT_CAPACITY,
            practiceSeconds = facts.practiceSeconds,
        )
    }
}
