package app.yap.server.feature.scenario.model

import java.util.UUID

internal data class ProgressReport(
    val reportId: UUID,
    val attempt: Int,
    val achievedObjective: Int?,
    val elapsedSeconds: Long,
)
