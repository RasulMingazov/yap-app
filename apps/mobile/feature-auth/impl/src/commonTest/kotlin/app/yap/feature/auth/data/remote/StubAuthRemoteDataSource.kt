package app.yap.feature.auth.data.remote

import app.yap.contract.auth.GoogleAuthorizationCodeDto
import app.yap.contract.auth.GoogleCredentialsDto
import app.yap.contract.auth.RefreshCredentialsDto
import app.yap.contract.auth.SessionDto
import app.yap.core.network.ApiResult
import io.github.rasulmingazov.stubcall.StubCall1

internal class StubAuthRemoteDataSource(
    session: SessionDto = StubSessionDto.stubSessionDto(),
) : AuthRemoteDataSource {

    private val result: ApiResult<SessionDto> = ApiResult.Success(session)

    val refreshCall = StubCall1.returns<RefreshCredentialsDto, ApiResult<SessionDto>>(result)
    val loginWithGoogleAuthorizationCodeCall =
        StubCall1.returns<GoogleAuthorizationCodeDto, ApiResult<SessionDto>>(result)
    val loginWithGoogleIdTokenCall = StubCall1.returns<GoogleCredentialsDto, ApiResult<SessionDto>>(result)

    override suspend fun refresh(credentials: RefreshCredentialsDto): ApiResult<SessionDto> =
        refreshCall.invoke(credentials)

    override suspend fun loginWithGoogleAuthorizationCode(
        code: GoogleAuthorizationCodeDto,
    ): ApiResult<SessionDto> = loginWithGoogleAuthorizationCodeCall.invoke(code)

    override suspend fun loginWithGoogleIdToken(credentials: GoogleCredentialsDto): ApiResult<SessionDto> =
        loginWithGoogleIdTokenCall.invoke(credentials)
}

internal object StubSessionDto {

    fun stubSessionDto(
        accessToken: String = app.yap.feature.auth.data.local.StubSession.ACCESS_TOKEN,
        refreshToken: String = app.yap.feature.auth.data.local.StubSession.REFRESH_TOKEN,
        accessTokenExpiresAtEpochSeconds: Long =
            app.yap.feature.auth.data.local.StubSession.ACCESS_TOKEN_EXPIRES_AT_EPOCH_SECONDS,
        refreshTokenExpiresAtEpochSeconds: Long =
            app.yap.feature.auth.data.local.StubSession.REFRESH_TOKEN_EXPIRES_AT_EPOCH_SECONDS,
    ): SessionDto = SessionDto(
        accessToken = accessToken,
        refreshToken = refreshToken,
        accessTokenExpiresAtEpochSeconds = accessTokenExpiresAtEpochSeconds,
        refreshTokenExpiresAtEpochSeconds = refreshTokenExpiresAtEpochSeconds,
    )
}
