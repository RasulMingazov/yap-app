package app.yap.feature.scenario.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import org.koin.core.scope.Scope

private const val DATA_STORE_NAME = "yap_scenario_overview"

private val Context.overviewDataStore: DataStore<Preferences> by preferencesDataStore(DATA_STORE_NAME)

internal actual fun Scope.createOverviewSnapshotStore(): OverviewSnapshotStore =
    AndroidOverviewSnapshotStore(context = get())

internal class AndroidOverviewSnapshotStore(
    private val context: Context,
) : OverviewSnapshotStore {

    private val key = stringPreferencesKey("overview")

    override fun observe(): Flow<OverviewSnapshotLocal?> = context.overviewDataStore.data.map { preferences ->
        preferences[key]?.let { stored -> runCatching { Json.decodeFromString<OverviewSnapshotLocal>(stored) }.getOrNull() }
    }

    override suspend fun clear() {
        context.overviewDataStore.edit { preferences -> preferences.remove(key) }
    }

    override suspend fun write(snapshot: OverviewSnapshotLocal) {
        val encoded = Json.encodeToString(snapshot)
        context.overviewDataStore.edit { preferences -> preferences[key] = encoded }
    }
}
