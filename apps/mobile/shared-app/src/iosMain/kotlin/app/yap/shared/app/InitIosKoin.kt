package app.yap.shared.app

import app.yap.app.root.di.initKoin
import app.yap.core.common.platform.IosMotionPreferences
import app.yap.core.common.platform.MotionPreferences
import app.yap.core.common.platform.currentPlatform
import app.yap.feature.auth.api.GoogleCredentialProvider
import org.koin.core.Koin
import org.koin.mp.KoinPlatformTools
import org.koin.dsl.module

fun initIosKoin(
    baseUrl: String,
    googleCredentialProvider: GoogleCredentialProvider,
    googleServerClientId: String,
    privacyUrl: String?,
    termsUrl: String?,
): Koin = KoinPlatformTools.defaultContext().getOrNull() ?: initKoin(
    baseUrl = baseUrl,
    googleServerClientId = googleServerClientId,
    privacyUrl = privacyUrl,
    termsUrl = termsUrl,
) {
    modules(
        module {
            single { googleCredentialProvider }

            single<MotionPreferences> { IosMotionPreferences() }

            single { currentPlatform() }
        },
    )
}.koin
