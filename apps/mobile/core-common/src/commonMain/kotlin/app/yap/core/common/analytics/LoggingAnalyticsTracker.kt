package app.yap.core.common.analytics

class LoggingAnalyticsTracker : AnalyticsTracker {

    override fun track(event: AnalyticsEvent) {
        println("analytics: ${event.name} ${event.params}")
    }
}
