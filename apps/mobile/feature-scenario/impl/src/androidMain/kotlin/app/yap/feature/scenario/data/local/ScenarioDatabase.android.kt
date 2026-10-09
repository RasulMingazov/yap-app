package app.yap.feature.scenario.data.local

import android.content.Context
import androidx.room.Room
import org.koin.core.scope.Scope

internal actual fun Scope.createScenarioDatabase(): ScenarioDatabase {
    val context = get<Context>()
    return Room.databaseBuilder<ScenarioDatabase>(
        context = context,
        name = context.getDatabasePath(SCENARIO_DATABASE_NAME).absolutePath,
    ).buildScenarioDatabase()
}
