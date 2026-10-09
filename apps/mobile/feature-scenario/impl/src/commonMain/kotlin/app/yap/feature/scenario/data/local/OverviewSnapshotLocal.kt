package app.yap.feature.scenario.data.local

import app.yap.contract.scenario.ScenarioStateDto
import kotlinx.serialization.Serializable

@Serializable
internal data class OverviewSnapshotLocal(
    val accountId: String,
    val state: ScenarioStateDto,
)
