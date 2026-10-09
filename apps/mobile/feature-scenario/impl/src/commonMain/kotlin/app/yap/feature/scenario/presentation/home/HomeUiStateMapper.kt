package app.yap.feature.scenario.presentation.home

import app.yap.feature.scenario.api.entity.Scenario
import app.yap.feature.scenario.api.entity.ScenarioOverview
import app.yap.feature.scenario.api.entity.ScenarioStatus
import app.yap.feature.scenario.presentation.common.pluralRu
import app.yap.feature.scenario.presentation.home.HomeViewModel.DataState
import app.yap.feature.scenario.presentation.home.HomeViewModel.UiState

private const val LOCKED_PREVIEW_COUNT = 4

internal class HomeUiStateMapper {

    fun map(dataState: DataState): UiState = UiState(
        content = when {
            dataState.overview != null -> ready(dataState.overview)
            dataState.isUnavailable -> UiState.Content.Unavailable
            else -> UiState.Content.Loading
        },
    )

    private fun ready(overview: ScenarioOverview): UiState.Content.Ready {
        val hero = HomeHeroResolver.resolve(overview)
        val activeScenarios = overview.scenarios.filter { scenario -> scenario.status is ScenarioStatus.Active }
        val hasFreeSlot = overview.slots.free > 0

        return UiState.Content.Ready(
            activeCards = activeScenarios.map { scenario ->
                activeCard(scenario = scenario, isHighlighted = scenario.id == overview.lastOpened?.id)
            },
            hero = hero.toUi(),
            lockedPreview = (hero as? HomeHero.Promo)?.let(::lockedPreview),
            showFullSlotsNotice = activeScenarios.isNotEmpty() && !hasFreeSlot,
            slotAdd = UiState.SlotAdd(
                count = "+${overview.slots.free}",
                label = pluralRu(
                    count = overview.slots.free,
                    one = "свободный слот",
                    few = "свободных слота",
                    many = "свободных слотов",
                ),
            ).takeIf { activeScenarios.isNotEmpty() && hasFreeSlot },
            slotsLabel = "${overview.slots.used} / ${overview.slots.capacity} слотов"
                .takeIf { activeScenarios.isNotEmpty() },
        )
    }

    private fun HomeHero.toUi(): UiState.Hero = when (this) {
        is HomeHero.StartFree -> UiState.Hero(
            cta = HomeCopy.START_CTA,
            eyebrow = "Бесплатно · шаг 1 из ${scenario.objectiveCount}",
            meta = HomeCopy.START_META,
            pips = List(scenario.objectiveCount) { false },
            title = scenario.title,
        )
        is HomeHero.Continue -> UiState.Hero(
            cta = HomeCopy.CONTINUE_CTA,
            eyebrow = "Шаг ${scenario.currentObjectiveOrFirst()} из ${scenario.objectiveCount}",
            meta = scenario.nextObjectiveTitle(),
            pips = scenario.achievedPips(),
            title = scenario.title,
        )
        is HomeHero.PickFromScenarios -> UiState.Hero(
            cta = HomeCopy.PICK_CTA,
            eyebrow = HomeCopy.PICK_EYEBROW,
            meta = HomeCopy.PICK_META,
            pips = null,
            title = HomeCopy.PICK_TITLE,
        )
        is HomeHero.Promo -> UiState.Hero(
            cta = HomeCopy.PROMO_CTA,
            eyebrow = HomeCopy.PROMO_EYEBROW,
            meta = HomeCopy.PROMO_META,
            pips = null,
            title = HomeCopy.PROMO_TITLE,
        )
        is HomeHero.AllDone -> UiState.Hero(
            cta = HomeCopy.ALL_DONE_CTA,
            eyebrow = HomeCopy.ALL_DONE_EYEBROW,
            meta = HomeCopy.ALL_DONE_META,
            pips = null,
            title = HomeCopy.ALL_DONE_TITLE,
        )
    }

    private fun activeCard(scenario: Scenario, isHighlighted: Boolean): UiState.ActiveCard =
        UiState.ActiveCard(
            id = scenario.id.value,
            isHighlighted = isHighlighted,
            meta = "Шаг ${scenario.currentObjectiveOrFirst()} из ${scenario.objectiveCount}",
            pips = scenario.achievedPips(),
            title = scenario.title,
        )

    private fun lockedPreview(hero: HomeHero.Promo): UiState.LockedPreview? {
        if (hero.lockedPreview.isEmpty()) return null
        return UiState.LockedPreview(
            cards = hero.lockedPreview.take(LOCKED_PREVIEW_COUNT).map { scenario ->
                UiState.LockedCard(
                    id = scenario.id.value,
                    meta = "${scenario.objectiveCount} " +
                        pluralRu(scenario.objectiveCount, "шаг", "шага", "шагов"),
                    title = scenario.title,
                )
            },
            moreLabel = HomeCopy.LOCKED_PREVIEW_ALL,
        )
    }

}

private fun Scenario.currentObjectiveOrFirst(): Int =
    (status as? ScenarioStatus.Active)?.currentObjective ?: 1

private fun Scenario.nextObjectiveTitle(): String? =
    objectives.firstOrNull { objective -> !objective.achieved }?.title

private fun Scenario.achievedPips(): List<Boolean> {
    val current = currentObjectiveOrFirst()
    return if (objectives.isNotEmpty()) {
        objectives.sortedBy { objective -> objective.order }.map { objective -> objective.achieved }
    } else {
        List(objectiveCount) { index -> index < current - 1 }
    }
}
