package app.yap.feature.scenario.api.usecase

import app.yap.feature.scenario.api.entity.ScenarioOverview

interface GetScenarioOverviewUseCase {

    suspend operator fun invoke(forceUpdate: Boolean): ScenarioOverview?
}
