package app.yap.server.feature.scenario.persistence

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.javatime.timestamp

internal object ScenarioTable : Table("scenario") {

    val id = text("id")
    val title = text("title")
    val position = integer("position")
    val isFree = bool("is_free")

    override val primaryKey = PrimaryKey(id)
}

internal object ScenarioObjectiveTable : Table("scenario_objective") {

    val scenarioId = text("scenario_id")
    val objectiveOrder = integer("objective_order")
    val title = text("title")

    override val primaryKey = PrimaryKey(scenarioId, objectiveOrder)
}

internal object UserScenarioTable : Table("user_scenario") {

    val userId = uuid("user_id")
    val scenarioId = text("scenario_id")
    val status = text("status")
    val attempt = integer("attempt")
    val currentObjective = integer("current_objective")
    val openedAt = timestamp("opened_at")
    val completedAt = timestamp("completed_at").nullable()

    override val primaryKey = PrimaryKey(userId, scenarioId)
}

internal object UserObjectiveTable : Table("user_objective") {

    val userId = uuid("user_id")
    val scenarioId = text("scenario_id")
    val attempt = integer("attempt")
    val objectiveOrder = integer("objective_order")
    val achievedOn = date("achieved_on")

    override val primaryKey = PrimaryKey(userId, scenarioId, attempt, objectiveOrder)
}

internal object PracticeDayTable : Table("practice_day") {

    val userId = uuid("user_id")
    val localDate = date("local_date")

    override val primaryKey = PrimaryKey(userId, localDate)
}

internal object UserStatsTable : Table("user_stats") {

    val userId = uuid("user_id")
    val practiceSeconds = long("practice_seconds")

    override val primaryKey = PrimaryKey(userId)
}

internal object ProgressReportTable : Table("progress_report") {

    val userId = uuid("user_id")
    val reportId = uuid("report_id")
    val receivedAt = timestamp("received_at")

    override val primaryKey = PrimaryKey(userId, reportId)
}
