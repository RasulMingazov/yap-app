package app.yap.feature.auth.data

import app.yap.contract.auth.SessionDto
import app.yap.feature.auth.api.entity.AuthSessionState
import app.yap.feature.auth.data.local.SessionLocal
import app.yap.feature.auth.data.local.SessionStorage
import app.yap.feature.auth.data.mapper.toDomain
import app.yap.feature.auth.data.mapper.toLocal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class SessionStore(
    private val currentTime: CurrentTime,
    private val sessionStorage: SessionStorage,
) {

    private val resolveMutex = Mutex()
    private val state = MutableStateFlow<AuthSessionState>(AuthSessionState.Unknown)

    private var isResolved = false

    val sessionState: StateFlow<AuthSessionState> = state.asStateFlow()

    suspend fun read(): SessionLocal? = sessionStorage.read()

    suspend fun write(session: SessionDto): SessionLocal {
        val stored = session.toLocal()

        sessionStorage.write(stored)
        isResolved = true
        state.value = stored.toDomain()
        return stored
    }

    suspend fun forget() {
        sessionStorage.clear()
        isResolved = true
        state.value = AuthSessionState.LoggedOut
    }

    suspend fun resolveOnce() {
        resolveMutex.withLock {
            if (isResolved) return
            isResolved = true
            state.value = sessionStorage.read()?.takeUnlessExpired()?.toDomain() ?: AuthSessionState.LoggedOut
        }
    }

    private suspend fun SessionLocal.takeUnlessExpired(): SessionLocal? {
        if (refreshTokenExpiresAtEpochSeconds > currentTime.epochSeconds()) return this

        sessionStorage.clear()
        return null
    }
}
