package app.yap.server.feature.scenario.persistence

import app.yap.server.feature.scenario.model.ProgressReport
import app.yap.server.feature.scenario.model.ProgressStatus
import app.yap.server.feature.scenario.model.ScenarioFailure
import java.time.Instant
import java.util.UUID
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.plus
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

private const val STATUS_ACTIVE = "active"
private const val STATUS_COMPLETED = "completed"

internal const val SLOT_CAPACITY = 5

internal class ScenarioRepository {

    fun loadFacts(userId: UUID): StateFacts = withoutRetry {
        val objectiveTitles = ScenarioObjectiveTable
            .selectAll()
            .orderBy(ScenarioObjectiveTable.objectiveOrder)
            .groupBy({ row -> row[ScenarioObjectiveTable.scenarioId] }, { row -> row[ScenarioObjectiveTable.title] })

        val scenarios = ScenarioTable
            .selectAll()
            .orderBy(ScenarioTable.position)
            .map { row ->
                ScenarioFact(
                    id = row[ScenarioTable.id],
                    title = row[ScenarioTable.title],
                    position = row[ScenarioTable.position],
                    isFree = row[ScenarioTable.isFree],
                    objectiveTitles = objectiveTitles[row[ScenarioTable.id]].orEmpty(),
                )
            }

        val progress = UserScenarioTable
            .selectAll()
            .where { UserScenarioTable.userId eq userId }
            .associate { row -> row[UserScenarioTable.scenarioId] to row.toProgressFact() }

        StateFacts(
            scenarios = scenarios,
            progress = progress,
            achievedObjectives = achievedObjectives(userId, progress),
            practiceDays = PracticeDayTable
                .selectAll()
                .where { PracticeDayTable.userId eq userId }
                .map { row -> row[PracticeDayTable.localDate] }
                .toSet(),
            practiceSeconds = UserStatsTable
                .selectAll()
                .where { UserStatsTable.userId eq userId }
                .singleOrNull()
                ?.get(UserStatsTable.practiceSeconds)
                ?: 0L,
        )
    }

    fun activate(userId: UUID, scenarioId: String, hasAccess: Boolean, now: Instant): Unit = withoutRetry {
        lockUser(userId)
        val scenario = scenarioOrNotFound(scenarioId)
        if (!scenario[ScenarioTable.isFree] && !hasAccess) throw ScenarioFailure.AccessRequired()

        when (progressRow(userId, scenarioId)?.get(UserScenarioTable.status)) {
            STATUS_ACTIVE -> return@withoutRetry
            STATUS_COMPLETED -> throw ScenarioFailure.MalformedInput()
            else -> {
                requireFreeSlot(userId)
                UserScenarioTable.insert { row ->
                    row[this.userId] = userId
                    row[this.scenarioId] = scenarioId
                    row[status] = STATUS_ACTIVE
                    row[attempt] = 1
                    row[currentObjective] = 1
                    row[openedAt] = now
                }
            }
        }
    }

    fun repeat(userId: UUID, scenarioId: String, hasAccess: Boolean, now: Instant): Unit = withoutRetry {
        lockUser(userId)
        val scenario = scenarioOrNotFound(scenarioId)
        val progress = progressRow(userId, scenarioId)
        if (progress?.get(UserScenarioTable.status) != STATUS_COMPLETED) throw ScenarioFailure.NotRepeatable()
        if (!scenario[ScenarioTable.isFree] && !hasAccess) throw ScenarioFailure.AccessRequired()
        requireFreeSlot(userId)

        UserScenarioTable.update(
            where = { (UserScenarioTable.userId eq userId) and (UserScenarioTable.scenarioId eq scenarioId) },
        ) { row ->
            row[status] = STATUS_ACTIVE
            row[attempt] = progress[UserScenarioTable.attempt] + 1
            row[currentObjective] = 1
            row[openedAt] = now
            row[completedAt] = null
        }
    }

    fun report(userId: UUID, scenarioId: String, report: ProgressReport, now: Instant): Unit = withoutRetry {
        lockUser(userId)
        scenarioOrNotFound(scenarioId)
        val alreadyApplied = ProgressReportTable
            .selectAll()
            .where { (ProgressReportTable.userId eq userId) and (ProgressReportTable.reportId eq report.reportId) }
            .any()
        if (alreadyApplied) return@withoutRetry

        val progress = progressRow(userId, scenarioId) ?: throw ScenarioFailure.MalformedInput()
        if (progress[UserScenarioTable.status] != STATUS_ACTIVE) throw ScenarioFailure.MalformedInput()
        if (progress[UserScenarioTable.attempt] != report.attempt) throw ScenarioFailure.MalformedInput()

        report.achievedObjective?.let { achieved ->
            applyAchievement(
                achieved = achieved,
                now = now,
                progress = progress,
                report = report,
                scenarioId = scenarioId,
                userId = userId,
            )
        }

        ProgressReportTable.insert { row ->
            row[this.userId] = userId
            row[reportId] = report.reportId
            row[receivedAt] = now
        }
        UserStatsTable.update(where = { UserStatsTable.userId eq userId }) { row ->
            row[practiceSeconds] = practiceSeconds + report.elapsedSeconds
        }
    }

    private fun achievedObjectives(
        userId: UUID,
        progress: Map<String, ProgressFact>,
    ): Map<String, Set<Int>> = progress
        .filterValues { fact -> fact.status == ProgressStatus.Active }
        .mapValues { (scenarioId, fact) ->
            UserObjectiveTable
                .selectAll()
                .where {
                    (UserObjectiveTable.userId eq userId) and
                        (UserObjectiveTable.scenarioId eq scenarioId) and
                        (UserObjectiveTable.attempt eq fact.attempt)
                }
                .map { row -> row[UserObjectiveTable.objectiveOrder] }
                .toSet()
        }

    private fun lockUser(userId: UUID) {
        UserStatsTable.insertIgnore { row ->
            row[this.userId] = userId
            row[practiceSeconds] = 0
        }
        UserStatsTable.selectAll().where { UserStatsTable.userId eq userId }.forUpdate().single()
    }

    private fun scenarioOrNotFound(scenarioId: String): ResultRow = ScenarioTable
        .selectAll()
        .where { ScenarioTable.id eq scenarioId }
        .singleOrNull()
        ?: throw ScenarioFailure.NotFound()

    private fun progressRow(userId: UUID, scenarioId: String): ResultRow? = UserScenarioTable
        .selectAll()
        .where { (UserScenarioTable.userId eq userId) and (UserScenarioTable.scenarioId eq scenarioId) }
        .singleOrNull()

    private fun requireFreeSlot(userId: UUID) {
        val active = UserScenarioTable
            .selectAll()
            .where { (UserScenarioTable.userId eq userId) and (UserScenarioTable.status eq STATUS_ACTIVE) }
            .count()
        if (active >= SLOT_CAPACITY) throw ScenarioFailure.SlotLimitReached()
    }

    private fun <T> withoutRetry(statement: Transaction.() -> T): T = transaction {
        maxAttempts = 1
        statement()
    }
}

private fun applyAchievement(
    achieved: Int,
    now: Instant,
    progress: ResultRow,
    report: ProgressReport,
    scenarioId: String,
    userId: UUID,
) {
    val objectiveCount = ScenarioObjectiveTable
        .selectAll()
        .where { ScenarioObjectiveTable.scenarioId eq scenarioId }
        .count()
        .toInt()
    if (achieved !in 1..objectiveCount) throw ScenarioFailure.MalformedInput()

    val currentObjective = progress[UserScenarioTable.currentObjective]
    if (achieved > currentObjective) throw ScenarioFailure.MalformedInput()

    val inserted = UserObjectiveTable.insertIgnore { row ->
        row[this.userId] = userId
        row[this.scenarioId] = scenarioId
        row[attempt] = report.attempt
        row[objectiveOrder] = achieved
        row[achievedOn] = report.localDate
    }.insertedCount > 0
    if (!inserted) return

    PracticeDayTable.insertIgnore { row ->
        row[this.userId] = userId
        row[localDate] = report.localDate
    }

    UserScenarioTable.update(
        where = { (UserScenarioTable.userId eq userId) and (UserScenarioTable.scenarioId eq scenarioId) },
    ) { row ->
        if (achieved == objectiveCount) {
            row[status] = STATUS_COMPLETED
            row[completedAt] = now
        } else {
            row[this.currentObjective] = maxOf(currentObjective, achieved + 1)
        }
    }
}

private fun ResultRow.toProgressFact(): ProgressFact = ProgressFact(
    status = when (this[UserScenarioTable.status]) {
        STATUS_COMPLETED -> ProgressStatus.Completed
        else -> ProgressStatus.Active
    },
    attempt = this[UserScenarioTable.attempt],
    currentObjective = this[UserScenarioTable.currentObjective],
    openedAt = this[UserScenarioTable.openedAt],
)
