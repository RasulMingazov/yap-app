package app.yap.feature.auth.data.identity

internal sealed interface ProviderAuthResult {

    data object Cancelled : ProviderAuthResult

    data class Failure(val kind: ProviderFailureKind) : ProviderAuthResult

    data class Success(val credential: ProviderCredential) : ProviderAuthResult
}

internal enum class ProviderFailureKind {
    Configuration,
    Connectivity,
    IntegrationNotConfigured,
    Provider,
}
