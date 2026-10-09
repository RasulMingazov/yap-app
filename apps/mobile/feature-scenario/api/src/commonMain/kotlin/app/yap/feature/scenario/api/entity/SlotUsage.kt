package app.yap.feature.scenario.api.entity

data class SlotUsage(
    val used: Int,
    val capacity: Int,
) {

    val free: Int get() = capacity - used
}
