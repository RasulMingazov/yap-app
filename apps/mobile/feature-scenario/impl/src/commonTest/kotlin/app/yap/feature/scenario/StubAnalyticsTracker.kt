package app.yap.feature.scenario

import app.yap.core.common.analytics.AnalyticsEvent
import app.yap.core.common.analytics.AnalyticsTracker

internal class StubAnalyticsTracker : AnalyticsTracker {

    val tracked = mutableListOf<AnalyticsEvent>()

    override fun track(event: AnalyticsEvent) {
        tracked += event
    }

    fun names(): List<String> = tracked.map(AnalyticsEvent::name)
}
