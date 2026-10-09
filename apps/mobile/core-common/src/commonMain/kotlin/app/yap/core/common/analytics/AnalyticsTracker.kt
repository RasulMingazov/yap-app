package app.yap.core.common.analytics

interface AnalyticsTracker {

    fun track(event: AnalyticsEvent)
}

data class AnalyticsEvent(
    val name: String,
    val params: Map<String, String> = emptyMap(),
)
