package app.yap.feature.auth.domain.usecase

import app.yap.core.common.platform.Platform
import app.yap.feature.auth.api.entity.AuthProvider
import app.yap.feature.auth.api.usecase.ObserveAuthProvidersUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

internal class DefaultObserveAuthProvidersUseCase(
    private val platform: Platform,
) : ObserveAuthProvidersUseCase {

    override fun invoke(): Flow<List<AuthProvider>> = flowOf(
        listOf(
            AuthProvider.Google(isEnabled = true, isVisible = true),
            AuthProvider.Apple(isEnabled = false, isVisible = platform == Platform.IOS),
            AuthProvider.TId(isEnabled = false, isVisible = true),
        ),
    )
}
