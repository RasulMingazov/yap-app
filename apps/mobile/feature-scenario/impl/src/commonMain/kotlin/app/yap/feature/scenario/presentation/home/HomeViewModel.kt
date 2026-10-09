package app.yap.feature.scenario.presentation.home

import androidx.lifecycle.viewModelScope
import app.yap.core.common.analytics.AnalyticsEvent
import app.yap.core.common.analytics.AnalyticsTracker
import app.yap.core.common.navigation.Navigator
import app.yap.core.common.presentation.BaseViewModel
import app.yap.feature.scenario.api.ScenariosNavKey
import app.yap.feature.scenario.api.entity.Scenario
import app.yap.feature.scenario.api.entity.ScenarioId
import app.yap.feature.scenario.api.entity.ScenarioOverview
import app.yap.feature.scenario.api.usecase.GetScenarioOverviewUseCase
import app.yap.feature.scenario.api.usecase.ObserveScenarioOverviewUseCase
import app.yap.feature.scenario.domain.ScenarioAnalytics
import app.yap.feature.scenario.domain.gateway.PaywallOrigin
import app.yap.feature.scenario.domain.gateway.PaywallSource
import app.yap.feature.scenario.domain.usecase.OpenPaywallUseCase
import app.yap.feature.scenario.domain.usecase.OpenScenarioOutcome
import app.yap.feature.scenario.domain.usecase.OpenScenarioUseCase
import app.yap.feature.scenario.presentation.common.ScenarioCopy
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class HomeViewModel(
    private val analyticsTracker: AnalyticsTracker,
    private val getScenarioOverviewUseCase: GetScenarioOverviewUseCase,
    private val navigator: Navigator,
    private val observeScenarioOverviewUseCase: ObserveScenarioOverviewUseCase,
    private val openPaywallUseCase: OpenPaywallUseCase,
    private val openScenarioUseCase: OpenScenarioUseCase,
    private val uiStateMapper: HomeUiStateMapper,
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
        is Event.PrimaryClicked -> onPrimaryClicked()
        is Event.CardClicked -> onCardClicked(event.id)
        is Event.AddSlotClicked -> navigator.navigate(ScenariosNavKey)
        is Event.LockedPreviewClicked -> onLockedPreviewClicked(event.id)
        is Event.LockedPreviewAllClicked -> onLockedPreviewAllClicked()
        is Event.RetryClicked -> load()
    }

    private fun onScreenShown() {
        analyticsTracker.track(AnalyticsEvent(name = ScenarioAnalytics.HOME_VIEW))
        load()
    }

    private fun load() {
        if (dataState.value.isLoading) return

        dataState.update { state -> state.copy(isLoading = true, isUnavailable = false) }
        viewModelScope.launch {
            val overview = getScenarioOverviewUseCase(forceUpdate = true)
            dataState.update { state -> state.copy(isLoading = false, isUnavailable = overview == null) }
        }
    }

    private fun onPrimaryClicked() {
        val overview = dataState.value.overview ?: return
        when (val hero = HomeHeroResolver.resolve(overview)) {
            is HomeHero.StartFree -> open(hero.scenario, PaywallOrigin.LockedRow)
            is HomeHero.Continue -> {
                analyticsTracker.track(AnalyticsEvent(name = ScenarioAnalytics.CONTINUE_CLICK))
                open(hero.scenario, PaywallOrigin.LockedRow)
            }
            is HomeHero.PickFromScenarios -> navigator.navigate(ScenariosNavKey)
            is HomeHero.AllDone -> navigator.navigate(ScenariosNavKey)
            is HomeHero.Promo -> viewModelScope.launch {
                openPaywallUseCase(PaywallSource(scenarioId = null, origin = PaywallOrigin.HeroPromo))
            }
        }
    }

    private fun onCardClicked(id: ScenarioId) {
        scenarioOf(id)?.let { scenario -> open(scenario, PaywallOrigin.LockedRow) }
    }

    private fun onLockedPreviewClicked(id: ScenarioId) {
        scenarioOf(id)?.let { scenario -> open(scenario, PaywallOrigin.LockedPreview) }
    }

    private fun onLockedPreviewAllClicked() {
        viewModelScope.launch {
            openPaywallUseCase(PaywallSource(scenarioId = null, origin = PaywallOrigin.LockedPreview))
        }
    }

    private fun open(scenario: Scenario, origin: PaywallOrigin) {
        if (dataState.value.isOpening) return

        dataState.update { state -> state.copy(isOpening = true) }
        viewModelScope.launch {
            val outcome = openScenarioUseCase(scenario, origin)
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

    private fun scenarioOf(id: ScenarioId): Scenario? =
        dataState.value.overview?.scenarios?.firstOrNull { scenario -> scenario.id == id }

    override fun onCleared() {
        super.onCleared()
        newsChannel.close()
    }

    data class DataState(
        val isLoading: Boolean = false,
        val isOpening: Boolean = false,
        val isUnavailable: Boolean = false,
        val overview: ScenarioOverview? = null,
    )

    data class UiState(val content: Content) {

        sealed interface Content {

            data object Loading : Content

            data object Unavailable : Content

            data class Ready(
                val activeCards: List<ActiveCard>,
                val hero: Hero,
                val lockedPreview: LockedPreview?,
                val showFullSlotsNotice: Boolean,
                val slotAdd: SlotAdd?,
                val slotsLabel: String?,
            ) : Content
        }

        data class Hero(
            val cta: String,
            val eyebrow: String?,
            val meta: String?,
            val pips: List<Boolean>?,
            val title: String,
        )

        data class ActiveCard(
            val id: String,
            val isHighlighted: Boolean,
            val meta: String,
            val pips: List<Boolean>,
            val title: String,
        )

        data class SlotAdd(val count: String, val label: String)

        data class LockedPreview(val cards: List<LockedCard>, val moreLabel: String)

        data class LockedCard(val id: String, val meta: String, val title: String)
    }

    sealed interface News {

        data class ShowMessage(val text: String) : News
    }

    sealed interface Event {

        data object ScreenShown : Event

        data object PrimaryClicked : Event

        data class CardClicked(val id: ScenarioId) : Event

        data object AddSlotClicked : Event

        data class LockedPreviewClicked(val id: ScenarioId) : Event

        data object LockedPreviewAllClicked : Event

        data object RetryClicked : Event
    }
}
