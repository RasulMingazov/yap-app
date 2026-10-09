package app.yap.feature.scenario.presentation.home

import app.yap.core.test.runViewModelTest
import app.yap.feature.scenario.StubAnalyticsTracker
import app.yap.feature.scenario.StubNavigator
import app.yap.feature.scenario.api.ScenariosNavKey
import app.yap.feature.scenario.api.entity.ScenarioStatus
import app.yap.feature.scenario.api.entity.StubScenarioOverview
import app.yap.feature.scenario.domain.ScenarioAnalytics
import app.yap.feature.scenario.domain.usecase.OpenScenarioOutcome
import app.yap.feature.scenario.domain.usecase.StubGetScenarioOverviewUseCase
import app.yap.feature.scenario.domain.usecase.StubObserveScenarioOverviewUseCase
import app.yap.feature.scenario.domain.usecase.StubOpenScenarioUseCase
import app.yap.feature.scenario.domain.usecase.StubPaywallUseCase
import app.yap.feature.scenario.presentation.common.ScenarioCopy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent

internal class HomeViewModelTest {

    @Test
    fun `GIVEN a fresh account WHEN home renders THEN the hero starts the free scenario`() = runViewModelTest {
        val env = Environment(overview = StubScenarioOverview.stubOverview())
        runCurrent()

        val content = env.viewModel.uiState.value.content

        assertIs<HomeViewModel.UiState.Content.Ready>(content)
        assertEquals(expected = HomeCopy.START_CTA, actual = content.hero.cta)
        assertEquals(expected = StubScenarioOverview.FREE_TITLE, actual = content.hero.title)
        assertEquals(expected = emptyList(), actual = content.activeCards)
    }

    @Test
    fun `GIVEN an open scenario WHEN home renders THEN the hero continues it with the step position`() =
        runViewModelTest {
            val active = StubScenarioOverview.stubActiveScenario(currentObjective = 2)
            val env = Environment(
                overview = StubScenarioOverview.stubOverview(
                        scenarios = listOf(active, StubScenarioOverview.stubLockedScenario()),
                        lastOpened = active,
                        slots = app.yap.feature.scenario.api.entity.SlotUsage(used = 1, capacity = 5),
                    ),
            )
            runCurrent()

            val content = env.viewModel.uiState.value.content

            assertIs<HomeViewModel.UiState.Content.Ready>(content)
            assertEquals(expected = HomeCopy.CONTINUE_CTA, actual = content.hero.cta)
            assertEquals(expected = "Шаг 2 из 3", actual = content.hero.eyebrow)
            assertEquals(expected = 1, actual = content.activeCards.size)
        }

    @Test
    fun `GIVEN a subscriber with nothing active WHEN home renders THEN the hero leads to the scenarios screen`() =
        runViewModelTest {
            val env = Environment(
                overview = StubScenarioOverview.stubOverview(
                        scenarios = listOf(
                            StubScenarioOverview.stubFreeScenario(status = ScenarioStatus.Completed),
                            StubScenarioOverview.stubLockedScenario(locked = false),
                        ),
                    ),
            )
            runCurrent()

            val content = env.viewModel.uiState.value.content

            assertIs<HomeViewModel.UiState.Content.Ready>(content)
            assertEquals(expected = HomeCopy.PICK_CTA, actual = content.hero.cta)

            env.viewModel.onEvent(HomeViewModel.Event.PrimaryClicked)
            runCurrent()
            env.navigator.navigateCall.calledWith(ScenariosNavKey)
        }

    @Test
    fun `GIVEN the screen is shown WHEN it reports itself THEN home_view is recorded exactly once`() =
        runViewModelTest {
            val env = Environment(overview = StubScenarioOverview.stubOverview())
            runCurrent()

            env.viewModel.onEvent(HomeViewModel.Event.ScreenShown)
            runCurrent()

            assertEquals(
                expected = listOf(ScenarioAnalytics.HOME_VIEW),
                actual = env.analyticsTracker.names(),
            )
            env.getScenarioOverviewUseCase.invokeCall.calledWith(true)
        }

    @Test
    fun `GIVEN the continue hero WHEN the primary action is taken THEN the click is recorded and the scenario opens`() =
        runViewModelTest {
            val active = StubScenarioOverview.stubActiveScenario()
            val env = Environment(
                overview = StubScenarioOverview.stubOverview(
                        scenarios = listOf(active),
                        lastOpened = active,
                    ),
            )
            runCurrent()

            env.viewModel.onEvent(HomeViewModel.Event.PrimaryClicked)
            runCurrent()

            assertEquals(expected = listOf(ScenarioAnalytics.CONTINUE_CLICK), actual = env.analyticsTracker.names())
            env.openScenarioUseCase.invokeCall.called(times = 1)
        }

    @Test
    fun `GIVEN no connection WHEN opening declines THEN the message explains practice needs the network`() =
        runViewModelTest {
            val active = StubScenarioOverview.stubActiveScenario()
            val env = Environment(
                overview = StubScenarioOverview.stubOverview(scenarios = listOf(active), lastOpened = active),
                openOutcome = OpenScenarioOutcome.NoConnection,
            )
            runCurrent()
            val observed = mutableListOf<HomeViewModel.News>()
            val collection = launch { env.viewModel.news.collect(observed::add) }
            runCurrent()

            env.viewModel.onEvent(HomeViewModel.Event.PrimaryClicked)
            runCurrent()

            assertEquals(
                expected = listOf<HomeViewModel.News>(HomeViewModel.News.ShowMessage(ScenarioCopy.NEEDS_CONNECTION)),
                actual = observed,
            )
            collection.cancel()
        }

    @Test
    fun `GIVEN nothing cached WHEN the load fails THEN the full-screen error shows and retry loads again`() =
        runViewModelTest {
            val env = Environment(overview = null, fetched = null)
            runCurrent()
            assertIs<HomeViewModel.UiState.Content.Loading>(env.viewModel.uiState.value.content)

            env.viewModel.onEvent(HomeViewModel.Event.ScreenShown)
            runCurrent()
            assertIs<HomeViewModel.UiState.Content.Unavailable>(env.viewModel.uiState.value.content)

            env.viewModel.onEvent(HomeViewModel.Event.RetryClicked)
            runCurrent()
            env.getScenarioOverviewUseCase.invokeCall.called(times = 2)
        }

    @Test
    fun `GIVEN every scenario is completed WHEN home renders THEN the hero says the programme is done and leads on`() =
        runViewModelTest {
            val env = Environment(
                overview = StubScenarioOverview.stubOverview(
                        scenarios = listOf(
                            StubScenarioOverview.stubFreeScenario(status = ScenarioStatus.Completed),
                            StubScenarioOverview.stubLockedScenario(status = ScenarioStatus.Completed, locked = false),
                        ),
                    ),
            )
            runCurrent()

            val content = env.viewModel.uiState.value.content
            assertIs<HomeViewModel.UiState.Content.Ready>(content)
            assertEquals(expected = HomeCopy.ALL_DONE_TITLE, actual = content.hero.title)

            env.viewModel.onEvent(HomeViewModel.Event.PrimaryClicked)
            runCurrent()
            env.navigator.navigateCall.calledWith(ScenariosNavKey)
        }

    @Test
    fun `GIVEN a scenario just completed WHEN home renders THEN it no longer sits on the active rail`() =
        runViewModelTest {
            val stillActive = StubScenarioOverview.stubActiveScenario(id = "active-1", title = "Активный")
            val env = Environment(
                overview = StubScenarioOverview.stubOverview(
                        scenarios = listOf(
                            stillActive,
                            StubScenarioOverview.stubFreeScenario(
                                id = "done-1",
                                status = ScenarioStatus.Completed,
                            ),
                        ),
                        lastOpened = stillActive,
                    ),
            )
            runCurrent()

            val content = env.viewModel.uiState.value.content

            assertIs<HomeViewModel.UiState.Content.Ready>(content)
            assertEquals(expected = listOf("active-1"), actual = content.activeCards.map { card -> card.id })
        }

    @Test
    fun `GIVEN the free scenario is done without access WHEN home renders THEN the promo hero previews locked ones`() =
        runViewModelTest {
            val env = Environment(
                overview = StubScenarioOverview.stubOverview(
                        scenarios = listOf(
                            StubScenarioOverview.stubFreeScenario(status = ScenarioStatus.Completed),
                            StubScenarioOverview.stubLockedScenario(id = "locked-1", title = "Платный 1"),
                            StubScenarioOverview.stubLockedScenario(id = "locked-2", title = "Платный 2"),
                        ),
                    ),
            )
            runCurrent()

            val content = env.viewModel.uiState.value.content

            assertIs<HomeViewModel.UiState.Content.Ready>(content)
            assertEquals(expected = HomeCopy.PROMO_TITLE, actual = content.hero.title)
            assertEquals(expected = HomeCopy.PROMO_CTA, actual = content.hero.cta)
            assertEquals(
                expected = listOf("locked-1", "locked-2"),
                actual = content.lockedPreview?.cards?.map { card -> card.id },
            )
            assertEquals(expected = HomeCopy.LOCKED_PREVIEW_ALL, actual = content.lockedPreview?.moreLabel)
        }

    private class Environment(
        overview: app.yap.feature.scenario.api.entity.ScenarioOverview?,
        fetched: app.yap.feature.scenario.api.entity.ScenarioOverview? = overview,
        openOutcome: OpenScenarioOutcome = OpenScenarioOutcome.Opened,
    ) {

        val analyticsTracker = StubAnalyticsTracker()
        val getScenarioOverviewUseCase = StubGetScenarioOverviewUseCase(overview = fetched)
        val navigator = StubNavigator()
        val observeScenarioOverviewUseCase = StubObserveScenarioOverviewUseCase(overview = overview)
        val openPaywallUseCase = StubPaywallUseCase()
        val openScenarioUseCase = StubOpenScenarioUseCase(outcome = openOutcome)
        val viewModel = HomeViewModel(
            analyticsTracker = analyticsTracker,
            getScenarioOverviewUseCase = getScenarioOverviewUseCase,
            navigator = navigator,
            observeScenarioOverviewUseCase = observeScenarioOverviewUseCase,
            openPaywallUseCase = openPaywallUseCase,
            openScenarioUseCase = openScenarioUseCase,
            uiStateMapper = HomeUiStateMapper(),
        )
    }
}
