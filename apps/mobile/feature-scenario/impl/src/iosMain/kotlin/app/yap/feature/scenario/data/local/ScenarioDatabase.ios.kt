package app.yap.feature.scenario.data.local

import androidx.room.Room
import kotlinx.cinterop.ExperimentalForeignApi
import org.koin.core.scope.Scope
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

internal actual fun Scope.createScenarioDatabase(): ScenarioDatabase =
    Room.databaseBuilder<ScenarioDatabase>(name = documentsDirectory() + "/" + SCENARIO_DATABASE_NAME)
        .buildScenarioDatabase()

@OptIn(ExperimentalForeignApi::class)
private fun documentsDirectory(): String = requireNotNull(
    NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = true,
        error = null,
    )?.path,
)
