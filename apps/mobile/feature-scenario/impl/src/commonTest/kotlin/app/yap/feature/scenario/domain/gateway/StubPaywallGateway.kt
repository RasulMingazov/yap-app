package app.yap.feature.scenario.domain.gateway

import io.github.rasulmingazov.stubcall.StubCall1

internal class StubPaywallGateway : PaywallGateway {

    val openCall = StubCall1.unit<PaywallSource>()

    override suspend fun open(source: PaywallSource) = openCall.invoke(source)
}
