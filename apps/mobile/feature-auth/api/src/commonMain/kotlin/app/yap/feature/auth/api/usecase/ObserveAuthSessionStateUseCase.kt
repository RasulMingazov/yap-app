package app.yap.feature.auth.api.usecase

import app.yap.feature.auth.api.entity.AuthSessionState
import kotlinx.coroutines.flow.Flow

fun interface ObserveAuthSessionStateUseCase {

    operator fun invoke(): Flow<AuthSessionState>
}
