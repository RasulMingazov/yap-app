package app.yap.server.feature.auth.identity

import app.yap.server.feature.auth.model.ProviderId

/**
 * The registered providers. A provider is registered only when its configuration is present, so an
 * unknown or unconfigured identifier resolves to `null` and its login fails as unavailable.
 */
internal class IdentityVerifiers(verifiers: List<IdentityVerifier>) {

    private val byProvider: Map<ProviderId, IdentityVerifier> =
        verifiers.associateBy(IdentityVerifier::providerId)

    fun find(provider: ProviderId): IdentityVerifier? = byProvider[provider]
}
