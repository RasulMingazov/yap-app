package app.yap.feature.auth.presentation.auth

import app.yap.feature.auth.domain.entity.LoginProviderId
import app.yap.feature.auth.presentation.login.LoginComponent
import app.yap.feature.auth.presentation.login.LoginStubs
import app.yap.feature.auth.presentation.login.StubLoginComponent
import app.yap.feature.auth.presentation.login.StubLoginComponentFactory
import app.yap.feature.auth.presentation.selectprovider.SelectProviderComponent
import app.yap.feature.auth.presentation.selectprovider.StubSelectProviderComponent
import app.yap.feature.auth.presentation.selectprovider.StubSelectProviderComponentFactory
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.resume
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame

internal class AuthComponentTest {

    @Test
    fun `GIVEN Login asks to open provider selection WHEN Auth handles the output THEN SelectProvider is presented`() {
        val env = Environment()

        env.login.output(LoginComponent.Output.OpenProviderSelection)

        assertNotNull(env.presentedSelectProvider)
    }

    @Test
    fun `GIVEN SelectProvider is presented WHEN it is dismissed THEN the same Login child is revealed`() {
        val env = Environment()
        env.login.output(LoginComponent.Output.OpenProviderSelection)
        val selectProvider = requireNotNull(env.presentedSelectProvider)

        selectProvider.output(SelectProviderComponent.Output.Dismissed)

        assertNull(env.auth.selectProviderSlot.value.child)
        assertSame(expected = env.login, actual = env.auth.login)
        env.loginFactory.createCall.called(times = 1)
    }

    @Test
    fun `GIVEN SelectProvider is presented WHEN a provider is selected THEN Auth dispatches it to Login once`() {
        val env = Environment()
        env.login.output(LoginComponent.Output.OpenProviderSelection)
        val selectProvider = requireNotNull(env.presentedSelectProvider)

        selectProvider.output(
            SelectProviderComponent.Output.ProviderSelected(providerId = LoginProviderId.Google),
        )

        env.login.dispatchCall.called(times = 1)
        env.login.dispatchCall.calledWith(
            LoginComponent.Event.ProviderSelected(providerId = LoginProviderId.Google),
        )
    }

    @Test
    fun `GIVEN a provider is selected WHEN Auth dispatches it to Login THEN SelectProvider is already dismissed`() {
        val env = Environment()
        val slotStates = mutableListOf<Boolean>()
        env.login.onDispatch = { slotStates += env.auth.selectProviderSlot.value.child != null }
        env.login.output(LoginComponent.Output.OpenProviderSelection)
        val selectProvider = requireNotNull(env.presentedSelectProvider)

        selectProvider.output(
            SelectProviderComponent.Output.ProviderSelected(providerId = LoginProviderId.Google),
        )

        assertEquals(expected = listOf(false), actual = slotStates)
    }

    @Test
    fun `GIVEN SelectProvider is being presented WHEN Login asks to open it again THEN it is presented once`() {
        val env = Environment()

        env.login.output(LoginComponent.Output.OpenProviderSelection)
        env.login.output(LoginComponent.Output.OpenProviderSelection)

        env.selectProviderFactory.createCall.called(times = 1)
    }

    @Test
    fun `GIVEN SelectProvider is being dismissed WHEN a provider is selected again THEN Login receives one event`() {
        val env = Environment()
        env.login.output(LoginComponent.Output.OpenProviderSelection)
        val selectProvider = requireNotNull(env.presentedSelectProvider)

        selectProvider.output(SelectProviderComponent.Output.ProviderSelected(providerId = LoginProviderId.Google))
        selectProvider.output(SelectProviderComponent.Output.ProviderSelected(providerId = LoginProviderId.Google))

        env.login.dispatchCall.called(times = 1)
    }

    @Test
    fun `GIVEN Login carries state WHEN SelectProvider is presented and dismissed THEN that state is unchanged`() {
        val env = Environment()
        val loadingState = LoginStubs.stubUiState(button = LoginComponent.UiState.Button.Loading)
        env.login.mutableUiState.value = loadingState
        env.login.output(LoginComponent.Output.OpenProviderSelection)
        val selectProvider = requireNotNull(env.presentedSelectProvider)

        selectProvider.output(SelectProviderComponent.Output.Dismissed)

        assertEquals(expected = loadingState, actual = env.auth.login.uiState.value)
    }

    private class Environment {

        val loginFactory = StubLoginComponentFactory()
        val login: StubLoginComponent = loginFactory.component
        val selectProviderFactory = StubSelectProviderComponentFactory()
        val auth: AuthComponent = DefaultAuthComponent(
            componentContext = DefaultComponentContext(lifecycle = LifecycleRegistry().apply { resume() }),
            loginComponentFactory = loginFactory,
            modelFactory = AuthModel.Factory(),
            selectProviderComponentFactory = selectProviderFactory,
        )

        val presentedSelectProvider: StubSelectProviderComponent?
            get() = auth.selectProviderSlot.value.child?.instance as StubSelectProviderComponent?
    }
}
