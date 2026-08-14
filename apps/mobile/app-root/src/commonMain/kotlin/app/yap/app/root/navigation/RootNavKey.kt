package app.yap.app.root.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
internal sealed interface RootNavKey : NavKey {

    @Serializable
    data object Main : RootNavKey
}
