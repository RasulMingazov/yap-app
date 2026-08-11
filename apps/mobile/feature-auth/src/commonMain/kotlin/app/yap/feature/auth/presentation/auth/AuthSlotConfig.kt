package app.yap.feature.auth.presentation.auth

import kotlinx.serialization.Serializable

/** Navigation configurations for [DefaultAuthComponent]; serialized so the sheet survives process death. */
@Serializable
internal sealed interface AuthSlotConfig {

    @Serializable
    data object SelectProviderConfig : AuthSlotConfig
}
