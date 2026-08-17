package app.yap.shared.app

import app.yap.app.root.di.initKoin
import app.yap.core.common.platform.IosMotionPreferences
import app.yap.core.common.platform.MotionPreferences
import app.yap.core.common.platform.currentPlatform
import org.koin.core.Koin
import org.koin.mp.KoinPlatformTools
import org.koin.dsl.module

fun initIosKoin(
    baseUrl: String,
    googleSignInBridge: IosGoogleSignInBridge,
    googleServerClientId: String,
    privacyUrl: String?,
    termsUrl: String?,
): Koin = KoinPlatformTools.defaultContext().getOrNull() ?: initKoin(
    baseUrl = baseUrl,
    googleServerClientId = googleServerClientId,
    privacyUrl = privacyUrl,
    termsUrl = termsUrl,
    iosGoogleIdTokenRequester = googleSignInBridge::requestIdToken,
) {
    modules(
        module {
            single<MotionPreferences> { IosMotionPreferences() }

            single { currentPlatform() }
        },
    )
}.koin
