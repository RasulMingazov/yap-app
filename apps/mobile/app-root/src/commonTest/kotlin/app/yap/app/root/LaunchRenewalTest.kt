package app.yap.app.root

import app.yap.app.root.navigation.RootBackStack
import app.yap.app.root.navigation.RootNavKey
import app.yap.app.root.navigation.StubObserveAuthStateUseCase
import app.yap.feature.auth.api.entity.AuthState
import app.yap.feature.auth.api.entity.UserId
import androidx.navigation3.runtime.NavKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

internal class LaunchRenewalTest {

    @Test
    fun `GIVEN auth state resolves to logged in WHEN the launch renewal runs THEN it renews exactly once`() =
        runTest {
            val env = Environment(authState = AuthState.LoggedIn(userId = UserId(USER_ID)))

            env.launchRenewal.run()

            env.renewSessionUseCase.invokeCall.called(times = 1)
        }

    @Test
    fun `GIVEN auth state is still unknown WHEN the launch renewal runs THEN nothing is renewed`() = runTest {
        val env = Environment(authState = AuthState.Unknown)

        val renewal = launch { env.launchRenewal.run() }
        advanceUntilIdle()

        env.renewSessionUseCase.invokeCall.notCalled()
        renewal.cancel()
    }

    @Test
    fun `GIVEN auth state resolves to logged out WHEN the launch renewal runs THEN nothing is renewed`() = runTest {
        val env = Environment(authState = AuthState.LoggedOut)

        env.launchRenewal.run()

        env.renewSessionUseCase.invokeCall.notCalled()
    }

    @Test
    fun `GIVEN the launch decision has not been made WHEN it later arrives THEN the renewal waits for it`() =
        runTest {
            val env = Environment(authState = AuthState.Unknown)

            launch { env.launchRenewal.run() }
            runCurrent()
            env.renewSessionUseCase.invokeCall.notCalled()

            env.observeAuthStateUseCase.authStates.value = AuthState.LoggedIn(userId = UserId(USER_ID))
            advanceUntilIdle()

            env.renewSessionUseCase.invokeCall.called(times = 1)
        }

    @Test
    fun `GIVEN auth state changes again WHEN the launch renewal has already run THEN it does not run twice`() =
        runTest {
            val env = Environment(authState = AuthState.LoggedIn(userId = UserId(USER_ID)))

            launch { env.launchRenewal.run() }
            advanceUntilIdle()
            env.observeAuthStateUseCase.authStates.value = AuthState.LoggedOut
            env.observeAuthStateUseCase.authStates.value = AuthState.LoggedIn(userId = UserId(USER_ID))
            advanceUntilIdle()

            env.renewSessionUseCase.invokeCall.called(times = 1)
        }

    @Test
    fun `GIVEN a renewal that never returns WHEN the root is observed THEN the main screen still arrives`() =
        runTest {
            val hangingRenewal = CompletableDeferred<Unit>()
            val env = Environment(
                authState = AuthState.LoggedIn(userId = UserId(USER_ID)),
                renewalGate = hangingRenewal,
            )
            val renewal = launch { env.launchRenewal.run() }
            runCurrent()

            var keys: List<NavKey> = emptyList()
            val rootKeysCollection = launch {
                env.rootBackStack.keys.collect { rootKeys -> keys = rootKeys }
            }
            advanceUntilIdle()

            assertEquals(expected = listOf(RootNavKey.Main), actual = keys)
            env.renewSessionUseCase.invokeCall.notCalled()
            renewal.cancel()
            rootKeysCollection.cancel()
        }

    private class Environment(
        authState: AuthState,
        renewalGate: CompletableDeferred<Unit>? = null,
    ) {

        val observeAuthStateUseCase = StubObserveAuthStateUseCase(authState = authState)
        val renewSessionUseCase = StubRenewSessionUseCase(gate = renewalGate)
        val rootBackStack = RootBackStack(observeAuthStateUseCase = observeAuthStateUseCase)
        val launchRenewal = LaunchRenewal(
            observeAuthStateUseCase = observeAuthStateUseCase,
            renewSessionUseCase = renewSessionUseCase,
        )
    }

    private companion object {
        const val USER_ID = "user-1"
    }
}
