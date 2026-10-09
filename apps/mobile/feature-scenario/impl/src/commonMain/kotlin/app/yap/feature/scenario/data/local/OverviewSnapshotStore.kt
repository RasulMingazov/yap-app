package app.yap.feature.scenario.data.local

import kotlinx.coroutines.flow.Flow
import org.koin.core.scope.Scope

internal interface OverviewSnapshotStore {

    fun observe(): Flow<OverviewSnapshotLocal?>

    suspend fun clear()

    suspend fun write(snapshot: OverviewSnapshotLocal)
}

internal expect fun Scope.createOverviewSnapshotStore(): OverviewSnapshotStore
