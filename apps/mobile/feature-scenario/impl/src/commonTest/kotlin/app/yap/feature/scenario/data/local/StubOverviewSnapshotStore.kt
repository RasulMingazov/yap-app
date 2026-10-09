package app.yap.feature.scenario.data.local

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

internal class StubOverviewSnapshotStore(
    snapshot: OverviewSnapshotLocal? = null,
) : OverviewSnapshotStore {

    val snapshots = MutableStateFlow(snapshot)

    override fun observe(): Flow<OverviewSnapshotLocal?> = snapshots

    override suspend fun clear() {
        snapshots.value = null
    }

    override suspend fun write(snapshot: OverviewSnapshotLocal) {
        snapshots.value = snapshot
    }
}
