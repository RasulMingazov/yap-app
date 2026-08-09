package app.yap.feature.auth.domain.usecase

import app.yap.feature.auth.domain.entity.LoginProvider
import app.yap.feature.auth.domain.repository.LoginProviderRepository
import kotlinx.coroutines.flow.Flow

internal interface ObserveLoginProvidersUseCase {

    operator fun invoke(): Flow<List<LoginProvider>>
}

internal class DefaultObserveLoginProvidersUseCase(
    private val loginProviderRepository: LoginProviderRepository,
) : ObserveLoginProvidersUseCase {

    override fun invoke(): Flow<List<LoginProvider>> = loginProviderRepository.observeAll()
}
