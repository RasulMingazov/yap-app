package app.yap.server.feature.scenario

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

internal class StreakCalculationTest {

    private val today = LocalDate.parse("2026-10-09")

    @Test
    fun `GIVEN practice today and the two days before WHEN the streak is computed THEN it anchors at today`() {
        val dates = setOf(today, today.minusDays(1), today.minusDays(2))

        assertEquals(expected = 3, actual = StreakCalculation.streakDays(dates, today))
    }

    @Test
    fun `GIVEN no practice today but a run ending yesterday WHEN computed THEN the run stays visible`() {
        val dates = setOf(today.minusDays(1), today.minusDays(2))

        assertEquals(expected = 2, actual = StreakCalculation.streakDays(dates, today))
    }

    @Test
    fun `GIVEN no practice today or yesterday WHEN computed THEN the streak is zero`() {
        val dates = setOf(today.minusDays(2), today.minusDays(3))

        assertEquals(expected = 0, actual = StreakCalculation.streakDays(dates, today))
    }

    @Test
    fun `GIVEN a gap inside the history WHEN computed THEN the run counts only back to the gap`() {
        val dates = setOf(today, today.minusDays(1), today.minusDays(3), today.minusDays(4))

        assertEquals(expected = 2, actual = StreakCalculation.streakDays(dates, today))
    }

    @Test
    fun `GIVEN a run crossing the week boundary WHEN computed THEN the week edge does not break it`() {
        val monday = LocalDate.parse("2026-10-05")
        val dates = setOf(monday, monday.minusDays(1), monday.minusDays(2))

        assertEquals(expected = 3, actual = StreakCalculation.streakDays(dates, monday))
    }

    @Test
    fun `GIVEN the same days reported from two devices WHEN merged as a set THEN the union counts each day once`() {
        val deviceA = setOf(today, today.minusDays(1))
        val deviceB = setOf(today.minusDays(1), today.minusDays(2))

        assertEquals(expected = 3, actual = StreakCalculation.streakDays(deviceA + deviceB, today))
    }
}
