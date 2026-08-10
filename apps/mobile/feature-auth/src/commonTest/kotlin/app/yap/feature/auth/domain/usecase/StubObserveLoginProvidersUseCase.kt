package app.yap.feature.auth.domain.usecase

import app.yap.feature.auth.domain.entity.LoginProvider
import app.yap.feature.auth.domain.entity.StubLoginProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

internal class StubObserveLoginProvidersUseCase(
    providers: List<LoginProvider> = StubLoginProvider.stubIosProviders(),
) : ObserveLoginProvidersUseCase {

    val providersState = MutableStateFlow(providers)

    override fun invoke(): Flow<List<LoginProvider>> = providersState
}
