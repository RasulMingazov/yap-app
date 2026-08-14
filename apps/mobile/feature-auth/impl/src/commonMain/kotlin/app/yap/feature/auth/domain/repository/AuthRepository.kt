package app.yap.feature.auth.domain.repository

import app.yap.feature.auth.api.entity.AuthState
import app.yap.feature.auth.api.entity.LoginOutcome
import kotlinx.coroutines.flow.Flow

internal interface AuthRepository {

    fun observe(): Flow<AuthState>

    suspend fun loginWithGoogle(): LoginOutcome

    suspend fun accessTokenLifetimeSeconds(): Long?

    suspend fun renewSession()
}
