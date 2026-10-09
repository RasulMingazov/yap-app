package app.yap.feature.scenario.api.entity

data class Streak(
    val days: Int,
    val weekDays: List<WeekDay>,
)

data class WeekDay(
    val isoDate: String,
    val practised: Boolean,
    val isToday: Boolean,
)
