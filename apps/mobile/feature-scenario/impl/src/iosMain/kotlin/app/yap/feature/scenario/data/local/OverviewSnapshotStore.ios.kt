package app.yap.feature.scenario.data.local

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.json.Json
import org.koin.core.scope.Scope
import platform.Foundation.NSUserDefaults

private const val STORAGE_KEY = "yap_scenario_overview"

internal actual fun Scope.createOverviewSnapshotStore(): OverviewSnapshotStore = IosOverviewSnapshotStore()

internal class IosOverviewSnapshotStore : OverviewSnapshotStore {

    private val defaults = NSUserDefaults.standardUserDefaults
    private val snapshots = MutableStateFlow(stored())

    override fun observe(): Flow<OverviewSnapshotLocal?> = snapshots

    override suspend fun clear() {
        defaults.removeObjectForKey(STORAGE_KEY)
        snapshots.value = null
    }

    override suspend fun write(snapshot: OverviewSnapshotLocal) {
        defaults.setObject(Json.encodeToString(snapshot), forKey = STORAGE_KEY)
        snapshots.value = snapshot
    }

    private fun stored(): OverviewSnapshotLocal? = defaults.stringForKey(STORAGE_KEY)?.let { stored ->
        runCatching { Json.decodeFromString<OverviewSnapshotLocal>(stored) }.getOrNull()
    }
}
