package app.yap.feature.auth.di

import app.yap.feature.auth.data.identity.AndroidGoogleCredentialProvider
import app.yap.feature.auth.data.identity.AppAuthGoogleBrowserAuthFlow
import app.yap.feature.auth.data.identity.CredentialManagerRequester
import app.yap.feature.auth.data.identity.GoogleCredentialProvider
import org.koin.core.module.Module

internal actual fun Module.bindGoogleCredentialProvider(
    googleAndroidClientId: String,
    googleRedirectUri: String,
    googleServerClientId: String,
    iosGoogleIdTokenRequester: (suspend (nonce: String) -> String?)?,
) {
    single<GoogleCredentialProvider> {
        AndroidGoogleCredentialProvider(
            credentialRequester = CredentialManagerRequester(
                activityProvider = get(),
                context = get(),
            ),
            googleBrowserAuthFlow = AppAuthGoogleBrowserAuthFlow(
                activityProvider = get(),
                context = get(),
                googleAndroidClientId = googleAndroidClientId,
                redirectUri = googleRedirectUri,
            ),
            googleServerClientId = googleServerClientId,
        )
    }
}
