package app.yap.feature.scenario.data.local

import org.koin.core.scope.Scope

internal interface OverviewSnapshotStore {

    suspend fun clear()

    suspend fun read(): OverviewSnapshotLocal?

    suspend fun write(snapshot: OverviewSnapshotLocal)
}

internal expect fun Scope.createOverviewSnapshotStore(): OverviewSnapshotStore
