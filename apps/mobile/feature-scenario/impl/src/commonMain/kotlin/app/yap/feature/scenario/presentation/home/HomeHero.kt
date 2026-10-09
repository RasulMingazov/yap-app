package app.yap.feature.scenario.presentation.home

import app.yap.feature.scenario.api.entity.Scenario
import app.yap.feature.scenario.api.entity.ScenarioOverview
import app.yap.feature.scenario.api.entity.ScenarioStatus

/** The five FR-010 hero variants; exactly one applies to any overview. */
internal sealed interface HomeHero {

    data class StartFree(val scenario: Scenario) : HomeHero

    data class Continue(val scenario: Scenario) : HomeHero

    data object PickFromScenarios : HomeHero

    data class Promo(val lockedPreview: List<Scenario>) : HomeHero

    data object AllDone : HomeHero
}

internal object HomeHeroResolver {

    fun resolve(overview: ScenarioOverview): HomeHero {
        overview.lastOpened?.let { lastOpened -> return HomeHero.Continue(lastOpened) }

        val scenarios = overview.scenarios
        val hasAccess = scenarios.any { scenario -> !scenario.isFree && !scenario.locked }
        val neverPractised = scenarios.none { scenario -> scenario.status !is ScenarioStatus.Available }
        val freeScenario = scenarios.firstOrNull(Scenario::isFree)

        return when {
            scenarios.isNotEmpty() && scenarios.all { it.status is ScenarioStatus.Completed } -> HomeHero.AllDone
            neverPractised && freeScenario != null && !hasAccess -> HomeHero.StartFree(freeScenario)
            !hasAccess -> HomeHero.Promo(
                lockedPreview = scenarios.filter { scenario ->
                    scenario.locked && scenario.status is ScenarioStatus.Available
                },
            )
            else -> HomeHero.PickFromScenarios
        }
    }
}
