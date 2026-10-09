package app.yap.contract.scenario

import kotlinx.serialization.Serializable

@Serializable
data class ReportProgressRequestDto(
    val reportId: String,
    val attempt: Int,
    val achievedObjective: Int?,
    val elapsedSeconds: Long,
)
