package app.yap.contract.scenario

import kotlinx.serialization.Serializable

@Serializable
data class ObjectiveDto(
    val order: Int,
    val title: String,
    val achieved: Boolean,
)
