package app.yap.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import app.yap.app.root.App
import app.yap.core.common.platform.ActivityProvider
import app.yap.feature.auth.api.entity.AuthSessionState
import app.yap.feature.auth.api.usecase.ObserveAuthSessionStateUseCase
import app.yap.shared.app.initAndroidKoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var activityProvider: ActivityProvider

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val koin = initAndroidKoin(
            baseUrl = AppConfiguration.apiBaseUrl,
            context = applicationContext,
            googleAndroidClientId = AppConfiguration.googleAndroidClientId,
            googleRedirectUri = AppConfiguration.googleRedirectUri,
            googleServerClientId = AppConfiguration.googleWebClientId,
            privacyUrl = AppConfiguration.privacyUrl,
            termsUrl = AppConfiguration.termsUrl,
        )
        activityProvider = koin.get()

        holdSplashUntilAuthSessionStateResolves(splashScreen, koin.get())

        setContent { App() }
    }

    private fun holdSplashUntilAuthSessionStateResolves(
        splashScreen: SplashScreen,
        observeAuthSessionStateUseCase: ObserveAuthSessionStateUseCase,
    ) {
        var isResolved = false
        splashScreen.setKeepOnScreenCondition { !isResolved }

        lifecycleScope.launch {
            observeAuthSessionStateUseCase().first { authSessionState -> authSessionState !is AuthSessionState.Unknown }
            isResolved = true
        }
    }

    override fun onResume() {
        super.onResume()
        activityProvider.onActivityResumed(this)
    }

    override fun onPause() {
        activityProvider.onActivityPaused(this)
        super.onPause()
    }
}
