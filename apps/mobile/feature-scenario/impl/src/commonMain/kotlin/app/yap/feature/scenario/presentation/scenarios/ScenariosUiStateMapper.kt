package app.yap.feature.scenario.presentation.scenarios

import app.yap.feature.scenario.api.entity.OverviewState
import app.yap.feature.scenario.api.entity.Scenario
import app.yap.feature.scenario.api.entity.ScenarioOverview
import app.yap.feature.scenario.api.entity.ScenarioStatus
import app.yap.feature.scenario.presentation.common.pluralRu
import app.yap.feature.scenario.presentation.scenarios.ScenariosViewModel.DataState
import app.yap.feature.scenario.presentation.scenarios.ScenariosViewModel.UiState

internal class ScenariosUiStateMapper {

    fun map(dataState: DataState): UiState = UiState(
        content = when (val overview = dataState.overview) {
            is OverviewState.Loading -> UiState.Content.Loading
            is OverviewState.Unavailable -> UiState.Content.Unavailable
            is OverviewState.Ready -> ready(overview.overview, dataState.filter)
        },
    )

    private fun ready(overview: ScenarioOverview, filter: ScenarioFilter): UiState.Content.Ready {
        val groups = groups(overview)
        val activeFilter = if (groups.none { group -> group.filter == filter }) ScenarioFilter.All else filter

        return UiState.Content.Ready(
            filters = buildList {
                add(filterUi(ScenarioFilter.All, "Все", groups.sumOf { group -> group.rows.size }, activeFilter))
                groups.forEach { group ->
                    add(filterUi(group.filter, group.title, group.rows.size, activeFilter))
                }
            },
            groups = if (activeFilter == ScenarioFilter.All) {
                groups
            } else {
                groups.filter { group -> group.filter == activeFilter }
            },
            showHeaders = activeFilter == ScenarioFilter.All,
        )
    }

    private fun filterUi(
        filter: ScenarioFilter,
        label: String,
        count: Int,
        selected: ScenarioFilter,
    ): UiState.FilterUi = UiState.FilterUi(
        count = count.toString(),
        filter = filter,
        isSelected = filter == selected,
        label = label,
    )

    // Grouping derives from the orthogonal status × locked pair (contracts/scenario-api.md).
    private fun groups(overview: ScenarioOverview): List<UiState.GroupUi> {
        val hasFreeSlot = overview.slots.free > 0
        val active = overview.scenarios.filter { it.status is ScenarioStatus.Active }
        val available = overview.scenarios.filter { it.status is ScenarioStatus.Available && !it.locked }
        val bySubscription = overview.scenarios.filter { it.status is ScenarioStatus.Available && it.locked }
        val completed = overview.scenarios.filter { it.status is ScenarioStatus.Completed }

        return buildList {
            if (active.isNotEmpty()) {
                add(
                    UiState.GroupUi(
                        countLabel = "${overview.slots.used} / ${overview.slots.capacity} слотов",
                        filter = ScenarioFilter.Active,
                        rows = active.map(::activeRow),
                        title = "Активные",
                    ),
                )
            }
            if (available.isNotEmpty()) {
                add(
                    UiState.GroupUi(
                        countLabel = available.size.toString(),
                        filter = ScenarioFilter.Available,
                        rows = available.map { scenario -> availableRow(scenario, hasFreeSlot) },
                        title = "Доступные",
                    ),
                )
            }
            if (bySubscription.isNotEmpty()) {
                add(
                    UiState.GroupUi(
                        countLabel = bySubscription.size.toString(),
                        filter = ScenarioFilter.BySubscription,
                        rows = bySubscription.map(::lockedRow),
                        title = "По подписке",
                    ),
                )
            }
            if (completed.isNotEmpty()) {
                add(
                    UiState.GroupUi(
                        countLabel = completed.size.toString(),
                        filter = ScenarioFilter.Completed,
                        rows = completed.map(::completedRow),
                        title = "Завершённые",
                    ),
                )
            }
        }
    }

    private fun activeRow(scenario: Scenario): UiState.RowUi {
        val achieved = (scenario.status as? ScenarioStatus.Active)?.currentObjective?.minus(1) ?: 0
        return UiState.RowUi(
            badge = if (scenario.locked) UiState.RowBadge.Lock else UiState.RowBadge.Chevron,
            id = scenario.id.value,
            isDimmed = false,
            isMetaAccented = true,
            meta = "$achieved из ${scenario.objectiveCount} ${stepsWord(scenario.objectiveCount)}",
            title = scenario.title,
        )
    }

    private fun availableRow(scenario: Scenario, hasFreeSlot: Boolean): UiState.RowUi = UiState.RowUi(
        badge = UiState.RowBadge.Chevron,
        id = scenario.id.value,
        isDimmed = !hasFreeSlot,
        isMetaAccented = scenario.isFree,
        meta = when {
            !hasFreeSlot -> "Нет свободного слота"
            scenario.isFree -> "Бесплатно · ${scenario.objectiveCount} ${stepsWord(scenario.objectiveCount)}"
            else -> "${scenario.objectiveCount} ${stepsWord(scenario.objectiveCount)}"
        },
        title = scenario.title,
    )

    private fun lockedRow(scenario: Scenario): UiState.RowUi = UiState.RowUi(
        badge = UiState.RowBadge.Lock,
        id = scenario.id.value,
        isDimmed = true,
        isMetaAccented = false,
        meta = "${scenario.objectiveCount} ${stepsWord(scenario.objectiveCount)}",
        title = scenario.title,
    )

    private fun completedRow(scenario: Scenario): UiState.RowUi = UiState.RowUi(
        badge = UiState.RowBadge.Done,
        id = scenario.id.value,
        isDimmed = true,
        isMetaAccented = false,
        meta = "${scenario.objectiveCount} из ${scenario.objectiveCount} ${stepsWord(scenario.objectiveCount)}",
        title = scenario.title,
    )

    private fun stepsWord(count: Int): String = pluralRu(count, "шаг", "шага", "шагов")
}
