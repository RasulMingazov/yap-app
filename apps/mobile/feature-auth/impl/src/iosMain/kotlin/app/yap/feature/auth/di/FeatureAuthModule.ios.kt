package app.yap.feature.auth.di

import app.yap.feature.auth.data.identity.GoogleCredentialProvider
import app.yap.feature.auth.data.identity.IosSdkGoogleCredentialProvider
import org.koin.core.module.Module

internal actual fun Module.bindGoogleCredentialProvider(
    googleAndroidClientId: String,
    googleRedirectUri: String,
    googleServerClientId: String,
    iosGoogleIdTokenRequester: (suspend (nonce: String) -> String?)?,
) {
    val requestIdToken = requireNotNull(iosGoogleIdTokenRequester) {
        "The iOS host must supply GoogleSignIn"
    }

    single<GoogleCredentialProvider> {
        IosSdkGoogleCredentialProvider(requestIdToken = requestIdToken)
    }
}
