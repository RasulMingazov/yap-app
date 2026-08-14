package app.yap.feature.auth.api

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface AuthNavKey : NavKey {

    @Serializable
    data object Login : AuthNavKey
}
