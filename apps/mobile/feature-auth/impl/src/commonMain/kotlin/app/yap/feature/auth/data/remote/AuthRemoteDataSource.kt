package app.yap.feature.auth.data.remote

import app.yap.contract.auth.GoogleAuthorizationCodeDto
import app.yap.contract.auth.GoogleCredentialsDto
import app.yap.contract.auth.RefreshCredentialsDto
import app.yap.contract.auth.SessionDto
import app.yap.core.network.ApiClient
import app.yap.core.network.ApiResult
import app.yap.core.network.post
import io.ktor.client.request.setBody

private const val GOOGLE_PATH = "/v1/auth/google"
private const val GOOGLE_CODE_PATH = "/v1/auth/google/code"
private const val REFRESH_PATH = "/v1/auth/refresh"

internal interface AuthRemoteDataSource {

    suspend fun refresh(credentials: RefreshCredentialsDto): ApiResult<SessionDto>

    suspend fun loginWithGoogleAuthorizationCode(code: GoogleAuthorizationCodeDto): ApiResult<SessionDto>

    suspend fun loginWithGoogleIdToken(credentials: GoogleCredentialsDto): ApiResult<SessionDto>
}

internal class DefaultAuthRemoteDataSource(private val apiClient: ApiClient) : AuthRemoteDataSource {

    override suspend fun refresh(credentials: RefreshCredentialsDto): ApiResult<SessionDto> =
        apiClient.post(REFRESH_PATH, authenticated = false) { setBody(credentials) }

    override suspend fun loginWithGoogleAuthorizationCode(
        code: GoogleAuthorizationCodeDto,
    ): ApiResult<SessionDto> = apiClient.post(GOOGLE_CODE_PATH, authenticated = false) { setBody(code) }

    override suspend fun loginWithGoogleIdToken(credentials: GoogleCredentialsDto): ApiResult<SessionDto> =
        apiClient.post(GOOGLE_PATH, authenticated = false) { setBody(credentials) }
}
