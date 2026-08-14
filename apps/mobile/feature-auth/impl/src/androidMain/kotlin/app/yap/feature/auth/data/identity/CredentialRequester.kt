package app.yap.feature.auth.data.identity

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import app.yap.core.common.platform.ActivityProvider

internal fun interface CredentialRequester {

    suspend fun request(request: GetCredentialRequest): GetCredentialResponse
}

internal class CredentialManagerRequester(
    private val activityProvider: ActivityProvider,
    private val context: Context,
) : CredentialRequester {

    override suspend fun request(request: GetCredentialRequest): GetCredentialResponse {
        val activity = requireNotNull(activityProvider.current()) {
            "No resumed Activity to present the Google confirmation"
        }
        return CredentialManager.create(context).getCredential(activity, request)
    }
}
