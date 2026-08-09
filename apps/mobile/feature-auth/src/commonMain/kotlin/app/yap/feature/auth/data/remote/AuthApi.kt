package app.yap.feature.auth.data.remote

import app.yap.contract.auth.LoginChallengeDto
import app.yap.contract.auth.LoginChallengeRequestDto
import app.yap.contract.auth.LoginRequestDto
import app.yap.contract.auth.RefreshRequestDto
import app.yap.contract.auth.SessionDto

internal interface AuthApi {

    suspend fun challenge(request: LoginChallengeRequestDto): AuthApiResult<LoginChallengeDto>

    suspend fun login(request: LoginRequestDto): AuthApiResult<SessionDto>

    suspend fun refresh(request: RefreshRequestDto): AuthApiResult<SessionDto>
}

internal sealed interface AuthApiResult<out T> {

    data class Failure(val kind: AuthApiFailureKind) : AuthApiResult<Nothing>

    data class Success<out T>(val value: T) : AuthApiResult<T>
}

/**
 * [Rejected] is a definitive server answer, [Unavailable] a transient one. The distinction decides
 * whether the stored session is cleared or preserved (R-059, R-060).
 */
internal enum class AuthApiFailureKind {
    Rejected,
    Unavailable,
}
