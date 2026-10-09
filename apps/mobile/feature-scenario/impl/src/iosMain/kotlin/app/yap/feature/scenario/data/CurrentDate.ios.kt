package app.yap.feature.scenario.data

import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarIdentifierGregorian
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSLocale

internal actual fun systemIsoDate(): String {
    val formatter = NSDateFormatter().apply {
        dateFormat = "yyyy-MM-dd"
        locale = NSLocale(localeIdentifier = "en_US_POSIX")
        NSCalendar.calendarWithIdentifier(NSCalendarIdentifierGregorian)?.let { calendar = it }
    }
    return formatter.stringFromDate(NSDate())
}
