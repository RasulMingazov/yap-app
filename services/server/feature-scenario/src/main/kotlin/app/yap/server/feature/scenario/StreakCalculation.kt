package app.yap.server.feature.scenario

import java.time.LocalDate

internal object StreakCalculation {

    fun streakDays(practisedDates: Set<LocalDate>, today: LocalDate): Int {
        val anchor = when {
            today in practisedDates -> today
            today.minusDays(1) in practisedDates -> today.minusDays(1)
            else -> return 0
        }

        return generateSequence(anchor) { day -> day.minusDays(1) }
            .takeWhile { day -> day in practisedDates }
            .count()
    }
}
