package app.yap.feature.scenario

import app.yap.feature.auth.api.entity.AuthSessionState
import app.yap.feature.auth.api.entity.UserId
import app.yap.feature.auth.api.usecase.ObserveAuthSessionStateUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

internal const val STUB_ACCOUNT_ID = "account-1"

internal class StubObserveAuthSessionStateUseCase(
    authSessionState: AuthSessionState = AuthSessionState.LoggedIn(userId = UserId(STUB_ACCOUNT_ID)),
) : ObserveAuthSessionStateUseCase {

    val authSessionStates = MutableStateFlow(authSessionState)

    override fun invoke(): Flow<AuthSessionState> = authSessionStates
}
