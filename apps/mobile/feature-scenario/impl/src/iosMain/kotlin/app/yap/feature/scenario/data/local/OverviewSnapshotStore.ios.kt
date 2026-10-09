package app.yap.feature.scenario.data.local

import kotlinx.serialization.json.Json
import org.koin.core.scope.Scope
import platform.Foundation.NSUserDefaults

private const val STORAGE_KEY = "yap_scenario_overview"

internal actual fun Scope.createOverviewSnapshotStore(): OverviewSnapshotStore = IosOverviewSnapshotStore()

internal class IosOverviewSnapshotStore : OverviewSnapshotStore {

    private val defaults = NSUserDefaults.standardUserDefaults

    override suspend fun clear() {
        defaults.removeObjectForKey(STORAGE_KEY)
    }

    override suspend fun read(): OverviewSnapshotLocal? {
        val stored = defaults.stringForKey(STORAGE_KEY) ?: return null
        return runCatching { Json.decodeFromString<OverviewSnapshotLocal>(stored) }.getOrNull()
    }

    override suspend fun write(snapshot: OverviewSnapshotLocal) {
        defaults.setObject(Json.encodeToString(snapshot), forKey = STORAGE_KEY)
    }
}
