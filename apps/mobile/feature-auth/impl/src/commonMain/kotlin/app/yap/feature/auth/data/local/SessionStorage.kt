package app.yap.feature.auth.data.local

import org.koin.core.scope.Scope

internal interface SessionStorage {

    suspend fun clear()

    suspend fun read(): SessionLocal?

    suspend fun write(session: SessionLocal)
}

internal expect fun Scope.createSessionStorage(): SessionStorage
