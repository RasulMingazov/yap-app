package app.yap.feature.scenario.domain.usecase

import app.yap.feature.scenario.domain.gateway.PaywallGateway
import app.yap.feature.scenario.domain.gateway.PaywallSource

internal interface OpenPaywallUseCase {

    suspend operator fun invoke(source: PaywallSource)
}

internal class DefaultOpenPaywallUseCase(
    private val paywallGateway: PaywallGateway,
) : OpenPaywallUseCase {

    override suspend fun invoke(source: PaywallSource) {
        paywallGateway.open(source)
    }
}
