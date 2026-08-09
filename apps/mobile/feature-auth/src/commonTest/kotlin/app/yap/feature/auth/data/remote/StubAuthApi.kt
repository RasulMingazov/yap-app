package app.yap.feature.auth.data.remote

import app.yap.contract.auth.LoginChallengeDto
import app.yap.contract.auth.LoginChallengeRequestDto
import app.yap.contract.auth.LoginRequestDto
import app.yap.contract.auth.RefreshRequestDto
import app.yap.contract.auth.SessionDto
import io.github.rasulmingazov.stubcall.StubCall1

internal class StubAuthApi(
    challenge: LoginChallengeDto = StubAuthDto.stubLoginChallengeDto(),
    session: SessionDto = StubAuthDto.stubSessionDto(),
    private val journal: MutableList<String> = mutableListOf(),
) : AuthApi {

    val challengeCall = StubCall1.returns<LoginChallengeRequestDto, AuthApiResult<LoginChallengeDto>>(
        AuthApiResult.Success(challenge),
    )
    val loginCall = StubCall1.returns<LoginRequestDto, AuthApiResult<SessionDto>>(
        AuthApiResult.Success(session),
    )
    val refreshCall = StubCall1.returns<RefreshRequestDto, AuthApiResult<SessionDto>>(
        AuthApiResult.Success(session),
    )

    override suspend fun challenge(
        request: LoginChallengeRequestDto,
    ): AuthApiResult<LoginChallengeDto> {
        journal += "challenge"
        return challengeCall.invoke(request)
    }

    override suspend fun login(request: LoginRequestDto): AuthApiResult<SessionDto> {
        journal += "login"
        return loginCall.invoke(request)
    }

    override suspend fun refresh(request: RefreshRequestDto): AuthApiResult<SessionDto> {
        journal += "refresh"
        return refreshCall.invoke(request)
    }
}
