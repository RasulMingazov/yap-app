package app.yap.feature.scenario.data

private const val DAYS_PER_ERA = 146_097L
private const val YEARS_PER_ERA = 400L
private const val EPOCH_SHIFT = 719_468L

@Suppress("MagicNumber")
internal object IsoDates {

    fun toEpochDay(isoDate: String): Long {
        val (year, month, day) = isoDate.split("-").map(String::toInt)
        val shiftedYear = (if (month <= 2) year - 1 else year).toLong()
        val era = shiftedYear.floorDiv(YEARS_PER_ERA)
        val yearOfEra = shiftedYear - era * YEARS_PER_ERA
        val dayOfYear = (153 * (month + (if (month > 2) -3 else 9)) + 2) / 5 + day - 1
        val dayOfEra = yearOfEra * 365 + yearOfEra / 4 - yearOfEra / 100 + dayOfYear
        return era * DAYS_PER_ERA + dayOfEra - EPOCH_SHIFT
    }

    fun fromEpochDay(epochDay: Long): String {
        val shifted = epochDay + EPOCH_SHIFT
        val era = shifted.floorDiv(DAYS_PER_ERA)
        val dayOfEra = shifted - era * DAYS_PER_ERA
        val yearOfEra = (dayOfEra - dayOfEra / 1460 + dayOfEra / 36_524 - dayOfEra / 146_096) / 365
        val dayOfYear = dayOfEra - (365 * yearOfEra + yearOfEra / 4 - yearOfEra / 100)
        val monthIndex = (5 * dayOfYear + 2) / 153
        val day = dayOfYear - (153 * monthIndex + 2) / 5 + 1
        val month = if (monthIndex < 10) monthIndex + 3 else monthIndex - 9
        val year = yearOfEra + era * YEARS_PER_ERA + (if (month <= 2) 1 else 0)
        return "${year.toString().padStart(4, '0')}-" +
            "${month.toString().padStart(2, '0')}-" +
            day.toString().padStart(2, '0')
    }

    fun isoDayOfWeek(epochDay: Long): Int = ((epochDay + 3).mod(7L)).toInt() + 1
}
