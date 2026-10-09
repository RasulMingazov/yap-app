package app.yap.server.feature.scenario.persistence

import app.yap.server.feature.scenario.model.ProgressStatus
import java.time.Instant
import java.time.LocalDate

internal data class StateFacts(
    val scenarios: List<ScenarioFact>,
    val progress: Map<String, ProgressFact>,
    val achievedObjectives: Map<String, Set<Int>>,
    val practiceDays: Set<LocalDate>,
    val practiceSeconds: Long,
)

internal data class ScenarioFact(
    val id: String,
    val title: String,
    val position: Int,
    val isFree: Boolean,
    val objectiveTitles: List<String>,
)

internal data class ProgressFact(
    val status: ProgressStatus,
    val attempt: Int,
    val currentObjective: Int,
    val openedAt: Instant,
)
