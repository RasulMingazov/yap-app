package app.yap.feature.auth.data.repository

import app.yap.core.common.network.AccessTokenProvider
import app.yap.feature.auth.api.entity.AuthSessionState
import app.yap.feature.auth.data.CurrentTime
import app.yap.feature.auth.data.SessionStore
import app.yap.feature.auth.domain.repository.AuthSessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

private const val REFRESH_MARGIN_SECONDS = 300L

internal class DefaultAuthSessionRepository(
    private val accessTokenProvider: AccessTokenProvider,
    private val currentTime: CurrentTime,
    private val sessionStore: SessionStore,
) : AuthSessionRepository {

    override fun observe(): Flow<AuthSessionState> = flow {
        emit(sessionStore.sessionState.value)
        sessionStore.resolveOnce()
        emitAll(sessionStore.sessionState)
    }.distinctUntilChanged()

    override suspend fun refresh() {
        val stored = sessionStore.read() ?: return
        val lifetimeSeconds = stored.accessTokenExpiresAtEpochSeconds - currentTime.epochSeconds()
        if (lifetimeSeconds > REFRESH_MARGIN_SECONDS) return

        accessTokenProvider.getAccessToken(rejectedAccessToken = stored.accessToken)
    }
}
