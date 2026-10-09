package app.yap.feature.scenario.data.local

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

internal class StubScenarioDao : ScenarioDao {

    val overview = MutableStateFlow<OverviewDb?>(null)
    val scenarios = MutableStateFlow<List<ScenarioWithObjectivesDb>>(emptyList())

    fun seed(overview: OverviewDb, scenarios: List<ScenarioDb>, objectives: List<ObjectiveDb>) {
        this.overview.value = overview
        this.scenarios.value = scenarios.map { scenario ->
            ScenarioWithObjectivesDb(scenario, objectives.filter { it.scenarioId == scenario.id })
        }
    }

    override fun observeOverview(): Flow<OverviewDb?> = overview

    override fun observeScenarios(): Flow<List<ScenarioWithObjectivesDb>> = scenarios

    override suspend fun replace(overview: OverviewDb, scenarios: List<ScenarioDb>, objectives: List<ObjectiveDb>) =
        seed(overview, scenarios, objectives)

    override suspend fun clear() {
        overview.value = null
        scenarios.value = emptyList()
    }

    override suspend fun insertOverview(overview: OverviewDb) {
        this.overview.value = overview
    }

    override suspend fun insertScenarios(scenarios: List<ScenarioDb>) {
        this.scenarios.update { current -> current + scenarios.map { ScenarioWithObjectivesDb(it, emptyList()) } }
    }

    override suspend fun insertObjectives(objectives: List<ObjectiveDb>) {
        scenarios.update { current ->
            current.map { row -> row.copy(objectives = row.objectives + objectives.filter { it.scenarioId == row.scenario.id }) }
        }
    }

    override suspend fun deleteOverview() {
        overview.value = null
    }

    override suspend fun deleteScenarios() {
        scenarios.value = emptyList()
    }

    override suspend fun deleteObjectives() {
        scenarios.update { current -> current.map { row -> row.copy(objectives = emptyList()) } }
    }
}
