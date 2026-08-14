package app.yap.app.root.navigation

import androidx.navigation3.runtime.NavKey
import app.yap.feature.auth.api.AuthNavKey
import app.yap.feature.auth.api.entity.AuthState
import app.yap.feature.auth.api.entity.UserId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

internal class RootBackStackTest {

    @Test
    fun `GIVEN auth state is unknown WHEN the root is observed THEN no destination is rooted`() = runTest {
        val env = Environment(authState = AuthState.Unknown)

        val keys = env.rootBackStack.keys.first()

        assertEquals(expected = emptyList<NavKey>(), actual = keys)
    }

    @Test
    fun `GIVEN the user is logged out WHEN the root is observed THEN the login destination is rooted`() = runTest {
        val env = Environment(authState = AuthState.LoggedOut)

        val keys = env.rootBackStack.keys.first()

        assertEquals(expected = listOf(AuthNavKey.Login), actual = keys)
    }

    @Test
    fun `GIVEN the user is logged in WHEN the root is observed THEN the main destination is rooted`() = runTest {
        val env = Environment(authState = AuthState.LoggedIn(userId = UserId("user-1")))

        val keys = env.rootBackStack.keys.first()

        assertEquals(expected = listOf(RootNavKey.Main), actual = keys)
    }

    private class Environment(
        authState: AuthState,
    ) {

        val observeAuthStateUseCase = StubObserveAuthStateUseCase(authState = authState)
        val rootBackStack = RootBackStack(observeAuthStateUseCase = observeAuthStateUseCase)
    }
}
