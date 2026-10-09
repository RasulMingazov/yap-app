package app.yap.feature.scenario.data

import java.time.LocalDate

internal actual fun systemIsoDate(): String = LocalDate.now().toString()
