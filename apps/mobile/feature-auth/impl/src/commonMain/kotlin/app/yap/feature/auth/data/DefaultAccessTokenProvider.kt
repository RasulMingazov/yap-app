package app.yap.feature.auth.data

import app.yap.contract.auth.RefreshCredentialsDto
import app.yap.core.common.network.AccessTokenProvider
import app.yap.core.network.ApiError
import app.yap.core.network.ApiResult
import app.yap.feature.auth.data.local.SessionLocal
import app.yap.feature.auth.data.remote.AuthRemoteDataSource
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class DefaultAccessTokenProvider(
    private val authRemoteDataSource: Lazy<AuthRemoteDataSource>,
    private val sessionStore: SessionStore,
) : AccessTokenProvider {

    private val rotationMutex = Mutex()

    override suspend fun getAccessToken(rejectedAccessToken: String?): String? {
        val stored = sessionStore.read() ?: return null
        if (rejectedAccessToken == null) return stored.accessToken

        return rotationMutex.withLock { rotateUnlessAlreadyDone(rejectedAccessToken) }
    }

    private suspend fun rotateUnlessAlreadyDone(rejectedAccessToken: String): String? {
        val current = sessionStore.read() ?: return null
        if (current.accessToken != rejectedAccessToken) return current.accessToken

        return rotate(current)
    }

    private suspend fun rotate(current: SessionLocal): String? {
        val credentials = RefreshCredentialsDto(refreshToken = current.refreshToken)

        return when (val result = authRemoteDataSource.value.refresh(credentials)) {
            is ApiResult.Success -> sessionStore.write(result.value).accessToken
            is ApiResult.Failure -> {
                forgetSessionIfRefused(result.error)
                null
            }
        }
    }

    private suspend fun forgetSessionIfRefused(error: ApiError) {
        when (error) {
            is ApiError.Rejected, is ApiError.Unauthorized -> sessionStore.forget()
            is ApiError.Malformed, is ApiError.Unavailable -> Unit
        }
    }
}
