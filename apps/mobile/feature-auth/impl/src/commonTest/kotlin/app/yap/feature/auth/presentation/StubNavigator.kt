package app.yap.feature.auth.presentation

import androidx.navigation3.runtime.NavKey
import app.yap.core.common.navigation.Navigator
import io.github.rasulmingazov.stubcall.StubCall0
import io.github.rasulmingazov.stubcall.StubCall1

internal class StubNavigator : Navigator {

    val backCall = StubCall0.unit()
    val navigateCall = StubCall1.unit<NavKey>()

    override fun navigate(key: NavKey) = navigateCall.invoke(key)

    override fun back() = backCall.invoke()
}
