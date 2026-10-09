package app.yap.feature.scenario.api.entity

/** Falls out of cache presence (FR-004, FR-006): no snapshot yet, no snapshot and no network, content. */
sealed interface OverviewState {

    data object Loading : OverviewState

    data object Unavailable : OverviewState

    data class Ready(val overview: ScenarioOverview, val isRefreshing: Boolean) : OverviewState
}
