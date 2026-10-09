package app.yap.feature.scenario.data.local

internal class StubOverviewSnapshotStore(
    snapshot: OverviewSnapshotLocal? = null,
) : OverviewSnapshotStore {

    var stored: OverviewSnapshotLocal? = snapshot
        private set

    var clearCount: Int = 0
        private set

    override suspend fun clear() {
        clearCount++
        stored = null
    }

    override suspend fun read(): OverviewSnapshotLocal? = stored

    override suspend fun write(snapshot: OverviewSnapshotLocal) {
        stored = snapshot
    }
}
