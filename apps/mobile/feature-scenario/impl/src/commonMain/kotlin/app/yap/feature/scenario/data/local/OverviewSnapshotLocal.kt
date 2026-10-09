package app.yap.feature.scenario.data.local

import app.yap.contract.scenario.ScenarioStateDto
import kotlinx.serialization.Serializable

/**
 * The last successful aggregate as served, bound to the account it belongs to (FR-004): a device
 * shared between accounts never renders another user's progress.
 */
@Serializable
internal data class OverviewSnapshotLocal(
    val accountId: String,
    val state: ScenarioStateDto,
    val todayIsoDate: String,
)
