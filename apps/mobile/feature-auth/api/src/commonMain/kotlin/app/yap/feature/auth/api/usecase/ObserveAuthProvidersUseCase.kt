package app.yap.feature.auth.api.usecase

import app.yap.feature.auth.api.entity.AuthProvider
import kotlinx.coroutines.flow.Flow

fun interface ObserveAuthProvidersUseCase {

    operator fun invoke(): Flow<List<AuthProvider>>
}
