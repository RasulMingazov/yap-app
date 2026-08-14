package app.yap.feature.auth.api.usecase

import app.yap.feature.auth.api.entity.AuthState
import kotlinx.coroutines.flow.Flow

fun interface ObserveAuthStateUseCase {

    operator fun invoke(): Flow<AuthState>
}
