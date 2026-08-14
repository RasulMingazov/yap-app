package app.yap.feature.auth.di

import org.koin.core.module.Module

internal actual fun Module.bindGoogleCredentialProvider(
    googleAndroidClientId: String,
    googleRedirectUri: String,
    googleServerClientId: String,
) = Unit
