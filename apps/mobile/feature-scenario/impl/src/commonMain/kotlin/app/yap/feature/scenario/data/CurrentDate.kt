package app.yap.feature.scenario.data

internal fun interface CurrentDate {

    fun isoDate(): String
}

internal expect fun systemIsoDate(): String

internal class SystemCurrentDate : CurrentDate {

    override fun isoDate(): String = systemIsoDate()
}
