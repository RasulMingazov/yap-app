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
import app.yap.feature.auth.api.entity.AuthState
import app.yap.feature.auth.api.usecase.ObserveAuthStateUseCase
import app.yap.shared.app.initAndroidKoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private const val BASE_URL = "http://10.0.2.2:8080"
private const val GOOGLE_SERVER_CLIENT_ID = "REPLACE_WITH_WEB_CLIENT_ID.apps.googleusercontent.com"
private const val GOOGLE_ANDROID_CLIENT_ID = "REPLACE_WITH_ANDROID_CLIENT_ID.apps.googleusercontent.com"
private const val GOOGLE_REDIRECT_URI = "app.yap.oauth:/oauth2redirect"
private val TERMS_URL: String? = null
private val PRIVACY_URL: String? = null

class MainActivity : ComponentActivity() {

    private lateinit var activityProvider: ActivityProvider

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val koin = initAndroidKoin(
            baseUrl = BASE_URL,
            context = applicationContext,
            googleAndroidClientId = GOOGLE_ANDROID_CLIENT_ID,
            googleRedirectUri = GOOGLE_REDIRECT_URI,
            googleServerClientId = GOOGLE_SERVER_CLIENT_ID,
            privacyUrl = PRIVACY_URL,
            termsUrl = TERMS_URL,
        )
        activityProvider = koin.get()

        holdSplashUntilAuthStateResolves(splashScreen, koin.get())

        setContent { App() }
    }

    private fun holdSplashUntilAuthStateResolves(
        splashScreen: SplashScreen,
        observeAuthStateUseCase: ObserveAuthStateUseCase,
    ) {
        var isResolved = false
        splashScreen.setKeepOnScreenCondition { !isResolved }

        lifecycleScope.launch {
            observeAuthStateUseCase().first { authState -> authState !is AuthState.Unknown }
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
