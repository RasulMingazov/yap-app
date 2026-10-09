package app.yap.contract.scenario

import kotlinx.serialization.Serializable

@Serializable
data class ScenarioStateDto(
    val scenarios: List<ScenarioDto>,
    val slotsUsed: Int,
    val slotCapacity: Int,
    val practiceSeconds: Long,
)
