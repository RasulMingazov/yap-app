package app.yap.feature.auth.domain.usecase

import app.yap.feature.auth.api.entity.AuthProvider
import app.yap.feature.auth.api.usecase.ObserveAuthProvidersUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

internal class StubObserveAuthProvidersUseCase(
    providers: List<AuthProvider> = emptyList(),
) : ObserveAuthProvidersUseCase {

    val providers = MutableStateFlow(providers)

    override fun invoke(): Flow<List<AuthProvider>> = this.providers
}
