package app.yap.shared.app

import android.content.Context
import app.yap.app.root.di.initKoin
import app.yap.core.common.platform.ActivityProvider
import app.yap.core.common.platform.AndroidMotionPreferences
import app.yap.core.common.platform.MotionPreferences
import app.yap.core.common.platform.currentPlatform
import org.koin.core.Koin
import org.koin.mp.KoinPlatformTools
import org.koin.dsl.module

@Suppress("LongParameterList")
fun initAndroidKoin(
    baseUrl: String,
    context: Context,
    googleAndroidClientId: String,
    googleRedirectUri: String,
    googleServerClientId: String,
    privacyUrl: String?,
    termsUrl: String?,
): Koin = KoinPlatformTools.defaultContext().getOrNull() ?: initKoin(
    baseUrl = baseUrl,
    googleServerClientId = googleServerClientId,
    privacyUrl = privacyUrl,
    termsUrl = termsUrl,
    googleAndroidClientId = googleAndroidClientId,
    googleRedirectUri = googleRedirectUri,
) {
    modules(
        module {
            single { ActivityProvider() }

            single { context.applicationContext }

            single<MotionPreferences> { AndroidMotionPreferences(context = get()) }

            single { currentPlatform() }
        },
    )
}.koin
