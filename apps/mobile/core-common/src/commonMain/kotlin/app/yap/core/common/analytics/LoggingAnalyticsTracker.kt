package app.yap.core.common.analytics

/** Debug sink until a vendor SDK is chosen (research R7): events land in the process log only. */
class LoggingAnalyticsTracker : AnalyticsTracker {

    override fun track(event: AnalyticsEvent) {
        println("analytics: ${event.name} ${event.params}")
    }
}
