package app.yap.feature.scenario.presentation.scenarios

import app.yap.core.test.runViewModelTest
import app.yap.feature.scenario.StubAnalyticsTracker
import app.yap.feature.scenario.StubNavigator
import app.yap.feature.scenario.api.entity.ScenarioId
import app.yap.feature.scenario.api.entity.ScenarioOverview
import app.yap.feature.scenario.api.entity.ScenarioStatus
import app.yap.feature.scenario.api.entity.SlotUsage
import app.yap.feature.scenario.api.entity.StubScenarioOverview
import app.yap.feature.scenario.domain.ScenarioAnalytics
import app.yap.feature.scenario.domain.usecase.StubGetScenarioOverviewUseCase
import app.yap.feature.scenario.domain.usecase.StubObserveScenarioOverviewUseCase
import app.yap.feature.scenario.domain.usecase.StubOpenScenarioUseCase
import app.yap.feature.scenario.domain.usecase.StubRepeatScenarioUseCase
import app.yap.feature.scenario.presentation.common.RepeatConfirmationNavKey
import app.yap.feature.scenario.presentation.common.ScenarioCopy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent

internal class ScenariosViewModelTest {

    @Test
    fun `GIVEN every status WHEN the list renders THEN rows group by progress and access with slot usage`() =
        runViewModelTest {
            val env = Environment(
                overview = overview(
                    scenarios = listOf(
                        StubScenarioOverview.stubActiveScenario(id = "active-1", title = "Активный"),
                        StubScenarioOverview.stubFreeScenario(id = "free-1", title = "Бесплатный"),
                        StubScenarioOverview.stubLockedScenario(id = "locked-1", title = "Платный"),
                        StubScenarioOverview.stubFreeScenario(
                            id = "done-1",
                            title = "Пройденный",
                            status = ScenarioStatus.Completed,
                        ),
                    ),
                    slotsUsed = 1,
                ),
            )
            runCurrent()

            val content = env.viewModel.uiState.value.content

            assertIs<ScenariosViewModel.UiState.Content.Ready>(content)
            assertEquals(
                expected = listOf("Активные", "Доступные", "По подписке", "Завершённые"),
                actual = content.groups.map { group -> group.title },
            )
            assertEquals(expected = "1 / 5 слотов", actual = content.groups.first().countLabel)
            assertEquals(
                expected = "Бесплатно · 6 шагов",
                actual = content.groups[1].rows.single().meta,
            )
        }

    @Test
    fun `GIVEN only available scenarios WHEN the list renders THEN empty groups are hidden`() =
        runViewModelTest {
            val env = Environment(overview = overview())
            runCurrent()

            val content = env.viewModel.uiState.value.content

            assertIs<ScenariosViewModel.UiState.Content.Ready>(content)
            assertEquals(
                expected = listOf("Доступные", "По подписке"),
                actual = content.groups.map { group -> group.title },
            )
        }

    @Test
    fun `GIVEN the rail WHEN a filter is chosen THEN only that group remains and the choice is recorded`() =
        runViewModelTest {
            val env = Environment(overview = overview())
            runCurrent()

            env.viewModel.onEvent(
                ScenariosViewModel.Event.FilterSelected(ScenarioFilter.BySubscription),
            )
            runCurrent()

            val content = env.viewModel.uiState.value.content
            assertIs<ScenariosViewModel.UiState.Content.Ready>(content)
            assertEquals(expected = listOf("По подписке"), actual = content.groups.map { it.title })
            assertEquals(expected = false, actual = content.showHeaders)
            assertEquals(
                expected = listOf(ScenarioAnalytics.SCENARIOS_FILTER_SELECT),
                actual = env.analyticsTracker.names(),
            )
        }

    @Test
    fun `GIVEN an active row WHEN it is tapped THEN its scenario opens`() = runViewModelTest {
        val active = StubScenarioOverview.stubActiveScenario(id = "active-1")
        val env = Environment(
            overview = overview(scenarios = listOf(active), slotsUsed = 1),
        )
        runCurrent()

        env.viewModel.onEvent(ScenariosViewModel.Event.RowClicked(ScenarioId("active-1")))
        runCurrent()

        env.openScenarioUseCase.invokeCall.called(times = 1)
    }

    @Test
    fun `GIVEN all slots taken WHEN an available row is tapped THEN the limit is explained without activating`() =
        runViewModelTest {
            val env = Environment(
                overview = overview(
                    scenarios = listOf(
                        StubScenarioOverview.stubFreeScenario(id = "free-1"),
                        StubScenarioOverview.stubActiveScenario(id = "active-1"),
                    ),
                    slotsUsed = 5,
                ),
            )
            runCurrent()
            val observed = mutableListOf<ScenariosViewModel.News>()
            val collection = launch { env.viewModel.news.collect(observed::add) }
            runCurrent()

            env.viewModel.onEvent(ScenariosViewModel.Event.RowClicked(ScenarioId("free-1")))
            runCurrent()

            env.openScenarioUseCase.invokeCall.notCalled()
            assertEquals(
                expected = listOf<ScenariosViewModel.News>(
                    ScenariosViewModel.News.ShowMessage(ScenarioCopy.SLOTS_FULL),
                ),
                actual = observed,
            )
            assertEquals(expected = listOf(ScenarioAnalytics.SLOT_LIMIT_REACHED), actual = env.analyticsTracker.names())
            collection.cancel()
        }

    @Test
    fun `GIVEN the screen is shown WHEN it reports itself THEN scenarios_view is recorded exactly once`() =
        runViewModelTest {
            val env = Environment(overview = overview())
            runCurrent()

            env.viewModel.onEvent(ScenariosViewModel.Event.ScreenShown)
            runCurrent()

            assertEquals(expected = listOf(ScenarioAnalytics.SCENARIOS_VIEW), actual = env.analyticsTracker.names())
            env.getScenarioOverviewUseCase.invokeCall.calledWith(true)
        }

    @Test
    fun `GIVEN a lapsed subscriber WHEN an active scenario is locked THEN it stays active with a lock and keeps its slot`() =
        runViewModelTest {
            val env = Environment(
                overview = overview(
                    scenarios = listOf(
                        StubScenarioOverview.stubLockedScenario(
                            id = "paid-active",
                            status = ScenarioStatus.Active(attempt = 1, currentObjective = 3),
                        ),
                        StubScenarioOverview.stubFreeScenario(),
                    ),
                    slotsUsed = 1,
                ),
            )
            runCurrent()

            val content = env.viewModel.uiState.value.content

            assertIs<ScenariosViewModel.UiState.Content.Ready>(content)
            val activeGroup = content.groups.first()
            assertEquals(expected = "Активные", actual = activeGroup.title)
            assertEquals(expected = "1 / 5 слотов", actual = activeGroup.countLabel)
            val row = activeGroup.rows.single()
            assertEquals(expected = "paid-active", actual = row.id)
            assertEquals(expected = ScenariosViewModel.UiState.RowBadge.Lock, actual = row.badge)
        }

    @Test
    fun `GIVEN a completed row WHEN it is tapped THEN only the confirmation sheet opens`() = runViewModelTest {
        val env = Environment(
            overview = overview(
                scenarios = listOf(
                    StubScenarioOverview.stubFreeScenario(
                        id = "done-1",
                        title = "Пройденный",
                        status = ScenarioStatus.Completed,
                    ),
                ),
            ),
        )
        runCurrent()

        env.viewModel.onEvent(ScenariosViewModel.Event.RowClicked(ScenarioId("done-1")))
        runCurrent()

        env.navigator.navigateCall.calledWith(
            RepeatConfirmationNavKey(scenarioId = "done-1", title = "Пройденный"),
        )
        env.repeatScenarioUseCase.invokeCall.notCalled()
        env.openScenarioUseCase.invokeCall.notCalled()
    }

    @Test
    fun `GIVEN the restart is confirmed WHEN the result returns THEN the scenario repeats`() = runViewModelTest {
        val done = StubScenarioOverview.stubFreeScenario(
            id = "done-1",
            status = ScenarioStatus.Completed,
        )
        val env = Environment(overview = overview(scenarios = listOf(done)))
        runCurrent()

        env.viewModel.onEvent(ScenariosViewModel.Event.RepeatConfirmed(ScenarioId("done-1")))
        runCurrent()

        env.repeatScenarioUseCase.invokeCall.calledWith(done)
    }

    private fun overview(
        scenarios: List<app.yap.feature.scenario.api.entity.Scenario> = listOf(
            StubScenarioOverview.stubFreeScenario(),
            StubScenarioOverview.stubLockedScenario(),
        ),
        slotsUsed: Int = 0,
    ): ScenarioOverview = StubScenarioOverview.stubOverview(
        scenarios = scenarios,
        slots = SlotUsage(used = slotsUsed, capacity = 5),
    )

    private class Environment(overview: ScenarioOverview) {

        val analyticsTracker = StubAnalyticsTracker()
        val getScenarioOverviewUseCase = StubGetScenarioOverviewUseCase(overview = overview)
        val navigator = StubNavigator()
        val observeScenarioOverviewUseCase = StubObserveScenarioOverviewUseCase(overview = overview)
        val openScenarioUseCase = StubOpenScenarioUseCase()
        val repeatScenarioUseCase = StubRepeatScenarioUseCase()
        val viewModel = ScenariosViewModel(
            analyticsTracker = analyticsTracker,
            getScenarioOverviewUseCase = getScenarioOverviewUseCase,
            navigator = navigator,
            observeScenarioOverviewUseCase = observeScenarioOverviewUseCase,
            openScenarioUseCase = openScenarioUseCase,
            repeatScenarioUseCase = repeatScenarioUseCase,
            uiStateMapper = ScenariosUiStateMapper(),
        )
    }
}
