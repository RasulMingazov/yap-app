package app.yap.feature.auth.data

import app.yap.contract.auth.RefreshCredentialsDto
import app.yap.core.common.network.AccessTokenProvider
import app.yap.feature.auth.api.entity.AuthState
import app.yap.feature.auth.data.local.SessionLocal
import app.yap.feature.auth.data.local.SessionStorage
import app.yap.feature.auth.data.mapper.toDomain
import app.yap.feature.auth.data.mapper.toLocal
import app.yap.feature.auth.data.remote.AuthRemoteDataSource
import app.yap.feature.auth.data.remote.AuthRemoteFailure
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class DefaultAccessTokenProvider(
    private val authRemoteDataSource: Lazy<AuthRemoteDataSource>,
    private val authStateSource: AuthStateSource,
    private val sessionStorage: SessionStorage,
) : AccessTokenProvider {

    private val rotationMutex = Mutex()

    override suspend fun getAccessToken(rejectedAccessToken: String?): String? {
        val stored = sessionStorage.read() ?: return null

        return if (rejectedAccessToken == null) {
            stored.accessToken
        } else {
            rotationMutex.withLock { rotateUnlessAlreadyDone(rejectedAccessToken) }
        }
    }

    private suspend fun rotateUnlessAlreadyDone(rejectedAccessToken: String): String? {
        val current = sessionStorage.read() ?: return null

        return if (current.accessToken == rejectedAccessToken) rotate(current) else current.accessToken
    }

    private suspend fun rotate(current: SessionLocal): String? = try {
        val rotated = authRemoteDataSource.value
            .refresh(RefreshCredentialsDto(refreshToken = current.refreshToken))
            .toLocal()

        sessionStorage.write(rotated)
        authStateSource.publish(rotated.toDomain())
        rotated.accessToken
    } catch (_: AuthRemoteFailure.Rejected) {
        sessionStorage.clear()
        authStateSource.publish(AuthState.LoggedOut)
        null
    } catch (_: AuthRemoteFailure.Unavailable) {
        null
    }
}
