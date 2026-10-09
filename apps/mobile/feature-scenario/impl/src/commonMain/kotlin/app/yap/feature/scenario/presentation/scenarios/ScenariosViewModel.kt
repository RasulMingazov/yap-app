package app.yap.feature.scenario.presentation.scenarios

import androidx.lifecycle.viewModelScope
import app.yap.core.common.analytics.AnalyticsEvent
import app.yap.core.common.analytics.AnalyticsTracker
import app.yap.core.common.navigation.Navigator
import app.yap.core.common.presentation.BaseViewModel
import app.yap.feature.scenario.api.entity.OverviewState
import app.yap.feature.scenario.api.entity.Scenario
import app.yap.feature.scenario.api.entity.ScenarioId
import app.yap.feature.scenario.api.entity.ScenarioStatus
import app.yap.feature.scenario.api.usecase.ObserveScenarioOverviewUseCase
import app.yap.feature.scenario.domain.ScenarioAnalytics
import app.yap.feature.scenario.domain.gateway.PaywallOrigin
import app.yap.feature.scenario.domain.usecase.OpenScenarioOutcome
import app.yap.feature.scenario.domain.usecase.OpenScenarioUseCase
import app.yap.feature.scenario.domain.usecase.RefreshOverviewUseCase
import app.yap.feature.scenario.domain.usecase.RepeatScenarioUseCase
import app.yap.feature.scenario.presentation.common.RepeatConfirmationNavKey
import app.yap.feature.scenario.presentation.common.ScenarioCopy
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class ScenariosViewModel(
    private val analyticsTracker: AnalyticsTracker,
    private val navigator: Navigator,
    private val observeScenarioOverviewUseCase: ObserveScenarioOverviewUseCase,
    private val openScenarioUseCase: OpenScenarioUseCase,
    private val refreshOverviewUseCase: RefreshOverviewUseCase,
    private val repeatScenarioUseCase: RepeatScenarioUseCase,
    private val uiStateMapper: ScenariosUiStateMapper,
) : BaseViewModel() {

    private val dataState = MutableStateFlow(DataState())
    val uiState: StateFlow<UiState> = dataState.mapState(uiStateMapper::map)

    private val newsChannel = Channel<News>(Channel.BUFFERED)
    val news: Flow<News> = newsChannel.receiveAsFlow()

    init {
        viewModelScope.launch {
            observeScenarioOverviewUseCase().collect { overview ->
                dataState.update { state -> state.copy(overview = overview) }
            }
        }
    }

    fun onEvent(event: Event) = when (event) {
        is Event.ScreenShown -> onScreenShown()
        is Event.FilterSelected -> onFilterSelected(event.filter)
        is Event.RowClicked -> onRowClicked(event.id)
        is Event.RepeatConfirmed -> onRepeatConfirmed(event.id)
        is Event.RetryClicked -> refresh()
    }

    private fun onScreenShown() {
        analyticsTracker.track(AnalyticsEvent(name = ScenarioAnalytics.SCENARIOS_VIEW))
        refresh()
    }

    private fun refresh() {
        viewModelScope.launch { refreshOverviewUseCase() }
    }

    private fun onFilterSelected(filter: ScenarioFilter) {
        analyticsTracker.track(
            AnalyticsEvent(
                name = ScenarioAnalytics.SCENARIOS_FILTER_SELECT,
                params = mapOf(ScenarioAnalytics.PARAM_FILTER to filter.name),
            ),
        )
        dataState.update { state -> state.copy(filter = filter) }
    }

    private fun onRowClicked(id: ScenarioId) {
        val overview = (dataState.value.overview as? OverviewState.Ready)?.overview ?: return
        val scenario = overview.scenarios.firstOrNull { candidate -> candidate.id == id } ?: return

        if (scenario.isDeclinedBySlotLimit(hasFreeSlot = overview.slots.free > 0)) {
            analyticsTracker.track(
                AnalyticsEvent(
                    name = ScenarioAnalytics.SLOT_LIMIT_REACHED,
                    params = mapOf(ScenarioAnalytics.PARAM_SCENARIO_ID to scenario.id.value),
                ),
            )
            newsChannel.trySend(News.ShowMessage(ScenarioCopy.SLOTS_FULL))
            return
        }
        if (scenario.status is ScenarioStatus.Completed && !scenario.locked) {
            navigator.navigate(
                RepeatConfirmationNavKey(scenarioId = scenario.id.value, title = scenario.title),
            )
            return
        }

        open(scenario)
    }

    private fun onRepeatConfirmed(id: ScenarioId) {
        val overview = (dataState.value.overview as? OverviewState.Ready)?.overview ?: return
        val scenario = overview.scenarios.firstOrNull { candidate -> candidate.id == id } ?: return
        if (dataState.value.isOpening) return

        dataState.update { state -> state.copy(isOpening = true) }
        viewModelScope.launch {
            val outcome = repeatScenarioUseCase(scenario)
            dataState.update { state -> state.copy(isOpening = false) }
            outcome.toNews()?.let(newsChannel::trySend)
        }
    }

    private fun open(scenario: Scenario) {
        if (dataState.value.isOpening) return

        dataState.update { state -> state.copy(isOpening = true) }
        viewModelScope.launch {
            val outcome = openScenarioUseCase(scenario, PaywallOrigin.LockedRow)
            dataState.update { state -> state.copy(isOpening = false) }
            outcome.toNews()?.let(newsChannel::trySend)
        }
    }

    private fun OpenScenarioOutcome.toNews(): News? = when (this) {
        is OpenScenarioOutcome.NoConnection -> News.ShowMessage(ScenarioCopy.NEEDS_CONNECTION)
        is OpenScenarioOutcome.SlotLimitReached -> {
            analyticsTracker.track(AnalyticsEvent(name = ScenarioAnalytics.SLOT_LIMIT_REACHED))
            News.ShowMessage(ScenarioCopy.SLOTS_FULL)
        }
        is OpenScenarioOutcome.Failed -> News.ShowMessage(ScenarioCopy.OPEN_FAILED)
        is OpenScenarioOutcome.Opened -> null
        is OpenScenarioOutcome.PaywallShown -> null
    }

    private fun Scenario.isDeclinedBySlotLimit(hasFreeSlot: Boolean): Boolean =
        !hasFreeSlot && !locked && status is ScenarioStatus.Available

    override fun onCleared() {
        super.onCleared()
        newsChannel.close()
    }

    data class DataState(
        val filter: ScenarioFilter = ScenarioFilter.All,
        val isOpening: Boolean = false,
        val overview: OverviewState = OverviewState.Loading,
    )

    data class UiState(val content: Content) {

        sealed interface Content {

            data object Loading : Content

            data object Unavailable : Content

            data class Ready(
                val filters: List<FilterUi>,
                val groups: List<GroupUi>,
                val showHeaders: Boolean,
            ) : Content
        }

        data class FilterUi(
            val count: String,
            val filter: ScenarioFilter,
            val isSelected: Boolean,
            val label: String,
        )

        data class GroupUi(
            val countLabel: String,
            val filter: ScenarioFilter,
            val rows: List<RowUi>,
            val title: String,
        )

        data class RowUi(
            val badge: RowBadge,
            val id: String,
            val isDimmed: Boolean,
            val isMetaAccented: Boolean,
            val meta: String,
            val title: String,
        )

        enum class RowBadge { Chevron, Lock, Done }
    }

    sealed interface News {

        data class ShowMessage(val text: String) : News
    }

    sealed interface Event {

        data object ScreenShown : Event

        data class FilterSelected(val filter: ScenarioFilter) : Event

        data class RowClicked(val id: ScenarioId) : Event

        data class RepeatConfirmed(val id: ScenarioId) : Event

        data object RetryClicked : Event
    }
}

internal enum class ScenarioFilter { All, Active, Available, BySubscription, Completed }
