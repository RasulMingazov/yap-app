package app.yap.feature.auth.domain.repository

import app.yap.feature.auth.api.entity.AuthSessionState
import kotlinx.coroutines.flow.Flow

internal interface AuthSessionRepository {

    fun observe(): Flow<AuthSessionState>

    suspend fun refresh()
}
