package app.yap.feature.auth.data.identity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import app.yap.core.common.platform.ActivityProvider
import app.yap.feature.auth.api.GoogleCredential
import app.yap.feature.auth.api.LoginCancelledException
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationService
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.ResponseTypeValues

private const val AUTHORIZATION_ENDPOINT = "https://accounts.google.com/o/oauth2/v2/auth"
private const val TOKEN_ENDPOINT = "https://oauth2.googleapis.com/token"
private const val LAUNCHER_KEY = "app.yap.feature.auth.googleBrowserAuth"
private val SCOPES = listOf("openid", "email", "profile")

internal fun interface GoogleBrowserAuthFlow {

    suspend fun requestAuthorizationCode(): GoogleCredential.AuthorizationCode
}

internal class AppAuthGoogleBrowserAuthFlow(
    private val activityProvider: ActivityProvider,
    private val context: Context,
    private val googleAndroidClientId: String,
    private val redirectUri: String,
) : GoogleBrowserAuthFlow {

    override suspend fun requestAuthorizationCode(): GoogleCredential.AuthorizationCode {
        val activity = activityProvider.current() as? ComponentActivity
        requireNotNull(activity) { "No resumed Activity to present the browser confirmation" }

        val service = AuthorizationService(context)
        val request = buildRequest()
        try {
            val result = activity.launchForResult(service.getAuthorizationRequestIntent(request))
            val response = result?.let(AuthorizationResponse::fromIntent)
                ?: throw LoginCancelledException()

            return GoogleCredential.AuthorizationCode(
                code = requireNotNull(response.authorizationCode) { "No authorization code returned" },
                codeVerifier = requireNotNull(request.codeVerifier) { "No PKCE verifier generated" },
                redirectUri = redirectUri,
            )
        } finally {
            service.dispose()
        }
    }

    private fun buildRequest(): AuthorizationRequest = AuthorizationRequest.Builder(
        AuthorizationServiceConfiguration(
            Uri.parse(AUTHORIZATION_ENDPOINT),
            Uri.parse(TOKEN_ENDPOINT),
        ),
        googleAndroidClientId,
        ResponseTypeValues.CODE,
        Uri.parse(redirectUri),
    ).setScopes(SCOPES).build()

    private suspend fun ComponentActivity.launchForResult(intent: Intent): Intent? =
        suspendCancellableCoroutine { continuation ->
            lateinit var launcher: androidx.activity.result.ActivityResultLauncher<Intent>
            launcher = activityResultRegistry.register(
                LAUNCHER_KEY,
                ActivityResultContracts.StartActivityForResult(),
            ) { result ->
                launcher.unregister()
                continuation.resume(result.data.takeIf { result.resultCode == Activity.RESULT_OK })
            }
            continuation.invokeOnCancellation { launcher.unregister() }
            launcher.launch(intent)
        }
}
