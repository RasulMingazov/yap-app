package app.yap.feature.auth.domain.repository

import app.yap.feature.auth.domain.entity.LoginOutcome
import app.yap.feature.auth.domain.entity.LoginProviderId
import app.yap.feature.auth.domain.entity.Session
import kotlinx.coroutines.flow.Flow

/**
 * Owns the stored session together with its refresh, because rotation and persistence must stay
 * atomic. Session credentials never cross this port (R-058, R-059, R-060).
 */
internal interface SessionRepository {

    fun observe(): Flow<Session?>

    /**
     * [forceUpdate] `false` returns the stored snapshot without any network work; `true` ensures
     * usable credentials, refreshing them only when the stored access has expired.
     */
    suspend fun get(forceUpdate: Boolean): Session?

    suspend fun logIn(providerId: LoginProviderId): LoginOutcome
}
