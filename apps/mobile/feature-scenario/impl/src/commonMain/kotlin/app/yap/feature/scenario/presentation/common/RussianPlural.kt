package app.yap.feature.scenario.presentation.common

private const val HUNDRED = 100
private const val TEN = 10
private const val TEEN_RANGE_START = 11
private const val TEEN_RANGE_END = 14
private val FEW_RANGE = 2..4

internal fun pluralRu(count: Int, one: String, few: String, many: String): String {
    val mod100 = count % HUNDRED
    val mod10 = count % TEN
    return when {
        mod100 in TEEN_RANGE_START..TEEN_RANGE_END -> many
        mod10 == 1 -> one
        mod10 in FEW_RANGE -> few
        else -> many
    }
}
