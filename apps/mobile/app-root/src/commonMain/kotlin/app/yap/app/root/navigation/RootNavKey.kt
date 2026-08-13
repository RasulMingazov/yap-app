package app.yap.app.root.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Application-level destinations owned by the composition root.
 * Feature-internal destinations stay inside their feature module.
 */
@Serializable
sealed interface RootNavKey : NavKey {

    @Serializable
    data object Auth : RootNavKey
}
