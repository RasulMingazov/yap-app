package app.yap.app.root.navigation

import androidx.navigation3.runtime.NavKey
import app.yap.feature.auth.api.AuthNavKey
import app.yap.feature.auth.api.entity.AuthSessionState
import app.yap.feature.auth.api.entity.UserId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

internal class RootBackStackTest {

    @Test
    fun `GIVEN auth state is unknown WHEN the root is observed THEN no destination is rooted`() = runTest {
        val env = Environment(authSessionState = AuthSessionState.Unknown)

        val keys = env.rootBackStack.keys.first()

        assertEquals(expected = emptyList<NavKey>(), actual = keys)
    }

    @Test
    fun `GIVEN the user is logged out WHEN the root is observed THEN the login destination is rooted`() = runTest {
        val env = Environment(authSessionState = AuthSessionState.LoggedOut)

        val keys = env.rootBackStack.keys.first()

        assertEquals(expected = listOf(AuthNavKey.Login), actual = keys)
    }

    @Test
    fun `GIVEN the user is logged in WHEN the root is observed THEN the main destination is rooted`() = runTest {
        val env = Environment(authSessionState = AuthSessionState.LoggedIn(userId = UserId("user-1")))

        val keys = env.rootBackStack.keys.first()

        assertEquals(expected = listOf(RootNavKey.Main), actual = keys)
    }

    @Test
    fun `GIVEN the login destination WHEN a destination is navigated to THEN it sits above the root`() = runTest {
        val env = Environment(authSessionState = AuthSessionState.LoggedOut)
        val observed = mutableListOf<List<NavKey>>()
        val collection = launch { env.rootBackStack.keys.toList(observed) }
        runCurrent()

        env.rootBackStack.navigate(AuthNavKey.SelectAuthProvider)
        runCurrent()

        assertEquals(
            expected = listOf(AuthNavKey.Login, AuthNavKey.SelectAuthProvider),
            actual = observed.last(),
        )
        collection.cancel()
    }

    @Test
    fun `GIVEN a pushed destination WHEN back is asked for THEN only that destination leaves`() = runTest {
        val env = Environment(authSessionState = AuthSessionState.LoggedOut)
        val observed = mutableListOf<List<NavKey>>()
        val collection = launch { env.rootBackStack.keys.toList(observed) }
        runCurrent()
        env.rootBackStack.navigate(AuthNavKey.SelectAuthProvider)
        runCurrent()

        env.rootBackStack.back()
        runCurrent()

        assertEquals(expected = listOf(AuthNavKey.Login), actual = observed.last())
        collection.cancel()
    }

    @Test
    fun `GIVEN nothing is pushed WHEN back is asked for THEN the root stands and nothing is chosen`() = runTest {
        val env = Environment(authSessionState = AuthSessionState.LoggedOut)
        val observed = mutableListOf<List<NavKey>>()
        val collection = launch { env.rootBackStack.keys.toList(observed) }
        runCurrent()

        env.rootBackStack.back()
        runCurrent()

        assertEquals(expected = listOf(AuthNavKey.Login), actual = observed.last())
        assertEquals(expected = listOf(listOf<NavKey>(AuthNavKey.Login)), actual = observed)
        collection.cancel()
    }

    @Test
    fun `GIVEN a destination is already on top WHEN it is navigated to again THEN it is not stacked twice`() =
        runTest {
            val env = Environment(authSessionState = AuthSessionState.LoggedOut)
            val observed = mutableListOf<List<NavKey>>()
            val collection = launch { env.rootBackStack.keys.toList(observed) }
            runCurrent()
            env.rootBackStack.navigate(AuthNavKey.SelectAuthProvider)
            runCurrent()

            env.rootBackStack.navigate(AuthNavKey.SelectAuthProvider)
            runCurrent()

            assertEquals(
                expected = listOf(AuthNavKey.Login, AuthNavKey.SelectAuthProvider),
                actual = observed.last(),
            )
            collection.cancel()
        }

    @Test
    fun `GIVEN a pushed destination WHEN the root is observed afresh THEN it survives the new subscription`() =
        runTest {
            val env = Environment(authSessionState = AuthSessionState.LoggedOut)
            val first = mutableListOf<List<NavKey>>()
            val firstCollection = launch { env.rootBackStack.keys.toList(first) }
            runCurrent()
            env.rootBackStack.navigate(AuthNavKey.SelectAuthProvider)
            runCurrent()
            firstCollection.cancel()

            val second = mutableListOf<List<NavKey>>()
            val secondCollection = launch { env.rootBackStack.keys.toList(second) }
            runCurrent()

            assertEquals(
                expected = listOf(AuthNavKey.Login, AuthNavKey.SelectAuthProvider),
                actual = second.last(),
            )
            secondCollection.cancel()
        }

    @Test
    fun `GIVEN a pushed destination WHEN the auth state changes THEN it does not survive the new root`() = runTest {
        val env = Environment(authSessionState = AuthSessionState.LoggedOut)
        val observed = mutableListOf<List<NavKey>>()
        val collection = launch { env.rootBackStack.keys.toList(observed) }
        runCurrent()
        env.rootBackStack.navigate(AuthNavKey.SelectAuthProvider)
        runCurrent()

        env.observeAuthSessionStateUseCase.authSessionStates.value = AuthSessionState.LoggedIn(userId = UserId("user-1"))
        runCurrent()

        assertEquals(expected = listOf(RootNavKey.Main), actual = observed.last())
        collection.cancel()
    }

    private class Environment(
        authSessionState: AuthSessionState,
    ) {

        val observeAuthSessionStateUseCase = StubObserveAuthSessionStateUseCase(authSessionState = authSessionState)
        val rootBackStack = RootBackStack(observeAuthSessionStateUseCase = observeAuthSessionStateUseCase)
    }
}
