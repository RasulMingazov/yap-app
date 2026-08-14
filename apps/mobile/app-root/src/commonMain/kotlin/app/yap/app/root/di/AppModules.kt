package app.yap.app.root.di

import app.yap.core.network.coreNetworkModule
import app.yap.feature.auth.di.featureAuthModule
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration

internal fun appModules(
    baseUrl: String,
    googleServerClientId: String,
    privacyUrl: String?,
    termsUrl: String?,
    googleAndroidClientId: String = "",
    googleRedirectUri: String = "",
): List<Module> = listOf(
    appRootModule(),
    featureAuthModule(
        googleServerClientId = googleServerClientId,
        privacyUrl = privacyUrl,
        termsUrl = termsUrl,
        googleAndroidClientId = googleAndroidClientId,
        googleRedirectUri = googleRedirectUri,
    ),
    coreNetworkModule(baseUrl),
)

fun initKoin(
    baseUrl: String,
    googleServerClientId: String,
    privacyUrl: String?,
    termsUrl: String?,
    googleAndroidClientId: String = "",
    googleRedirectUri: String = "",
    appDeclaration: KoinAppDeclaration = {},
): KoinApplication = startKoin {
    appDeclaration()
    modules(
        appModules(
            baseUrl = baseUrl,
            googleServerClientId = googleServerClientId,
            privacyUrl = privacyUrl,
            termsUrl = termsUrl,
            googleAndroidClientId = googleAndroidClientId,
            googleRedirectUri = googleRedirectUri,
        ),
    )
}
