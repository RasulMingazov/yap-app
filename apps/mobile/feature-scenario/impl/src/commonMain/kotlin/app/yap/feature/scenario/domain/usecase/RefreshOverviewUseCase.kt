package app.yap.feature.scenario.domain.usecase

import app.yap.feature.scenario.domain.repository.ScenarioRepository

internal interface RefreshOverviewUseCase {

    suspend operator fun invoke()
}

/** Refreshes on screen entry; failures stay silent once content is shown (research R3). */
internal class DefaultRefreshOverviewUseCase(
    private val scenarioRepository: ScenarioRepository,
) : RefreshOverviewUseCase {

    override suspend fun invoke() {
        scenarioRepository.refresh()
    }
}
