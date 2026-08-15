package app.yap.app.root

import app.yap.app.root.navigation.RootBackStack
import app.yap.app.root.navigation.RootNavKey
import app.yap.app.root.navigation.StubObserveAuthSessionStateUseCase
import app.yap.feature.auth.api.entity.AuthSessionState
import app.yap.feature.auth.api.entity.UserId
import androidx.navigation3.runtime.NavKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

internal class LaunchSessionRefreshTest {

    @Test
    fun `GIVEN auth state resolves to logged in WHEN the launch refresh runs THEN it refreshes exactly once`() =
        runTest {
            val env = Environment(authSessionState = AuthSessionState.LoggedIn(userId = UserId(USER_ID)))

            env.launchSessionRefresh.run()

            env.refreshSessionUseCase.invokeCall.called(times = 1)
        }

    @Test
    fun `GIVEN auth state is still unknown WHEN the launch refresh runs THEN nothing is refreshed`() = runTest {
        val env = Environment(authSessionState = AuthSessionState.Unknown)

        val refresh = launch { env.launchSessionRefresh.run() }
        advanceUntilIdle()

        env.refreshSessionUseCase.invokeCall.notCalled()
        refresh.cancel()
    }

    @Test
    fun `GIVEN auth state resolves to logged out WHEN the launch refresh runs THEN nothing is refreshed`() = runTest {
        val env = Environment(authSessionState = AuthSessionState.LoggedOut)

        env.launchSessionRefresh.run()

        env.refreshSessionUseCase.invokeCall.notCalled()
    }

    @Test
    fun `GIVEN the launch decision has not been made WHEN it later arrives THEN the refresh waits for it`() =
        runTest {
            val env = Environment(authSessionState = AuthSessionState.Unknown)

            launch { env.launchSessionRefresh.run() }
            runCurrent()
            env.refreshSessionUseCase.invokeCall.notCalled()

            env.observeAuthSessionStateUseCase.authSessionStates.value = AuthSessionState.LoggedIn(userId = UserId(USER_ID))
            advanceUntilIdle()

            env.refreshSessionUseCase.invokeCall.called(times = 1)
        }

    @Test
    fun `GIVEN auth state changes again WHEN the launch refresh has already run THEN it does not run twice`() =
        runTest {
            val env = Environment(authSessionState = AuthSessionState.LoggedIn(userId = UserId(USER_ID)))

            launch { env.launchSessionRefresh.run() }
            advanceUntilIdle()
            env.observeAuthSessionStateUseCase.authSessionStates.value = AuthSessionState.LoggedOut
            env.observeAuthSessionStateUseCase.authSessionStates.value = AuthSessionState.LoggedIn(userId = UserId(USER_ID))
            advanceUntilIdle()

            env.refreshSessionUseCase.invokeCall.called(times = 1)
        }

    @Test
    fun `GIVEN a refresh that never returns WHEN the root is observed THEN the main screen still arrives`() =
        runTest {
            val hangingRefresh = CompletableDeferred<Unit>()
            val env = Environment(
                authSessionState = AuthSessionState.LoggedIn(userId = UserId(USER_ID)),
                refreshGate = hangingRefresh,
            )
            val refresh = launch { env.launchSessionRefresh.run() }
            runCurrent()

            var keys: List<NavKey> = emptyList()
            val rootKeysCollection = launch {
                env.rootBackStack.keys.collect { rootKeys -> keys = rootKeys }
            }
            advanceUntilIdle()

            assertEquals(expected = listOf(RootNavKey.Main), actual = keys)
            env.refreshSessionUseCase.invokeCall.notCalled()
            refresh.cancel()
            rootKeysCollection.cancel()
        }

    private class Environment(
        authSessionState: AuthSessionState,
        refreshGate: CompletableDeferred<Unit>? = null,
    ) {

        val observeAuthSessionStateUseCase = StubObserveAuthSessionStateUseCase(authSessionState = authSessionState)
        val refreshSessionUseCase = StubRefreshSessionUseCase(gate = refreshGate)
        val rootBackStack = RootBackStack(observeAuthSessionStateUseCase = observeAuthSessionStateUseCase)
        val launchSessionRefresh = LaunchSessionRefresh(
            observeAuthSessionStateUseCase = observeAuthSessionStateUseCase,
            refreshSessionUseCase = refreshSessionUseCase,
        )
    }

    private companion object {
        const val USER_ID = "user-1"
    }
}
