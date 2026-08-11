package app.yap.feature.auth.presentation.auth

import app.yap.feature.auth.presentation.login.LoginComponent
import app.yap.feature.auth.presentation.login.LoginEvent
import app.yap.feature.auth.presentation.login.LoginOutput
import app.yap.feature.auth.presentation.selectprovider.SelectProviderComponent
import app.yap.feature.auth.presentation.selectprovider.SelectProviderOutput
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.childContext
import com.arkivanov.decompose.router.slot.ChildSlot
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import com.arkivanov.decompose.value.Value

internal class DefaultAuthComponent(
    componentContext: ComponentContext,
    loginComponentFactory: LoginComponent.Factory,
    selectProviderComponentFactory: SelectProviderComponent.Factory,
) : AuthComponent, ComponentContext by componentContext {

    private val slotNavigation = SlotNavigation<AuthSlotConfig>()

    override val login: LoginComponent = loginComponentFactory.invoke(
        componentContext = childContext(key = LOGIN_KEY),
        output = ::onLoginOutput,
    )

    /**
     * The sheet survives process death: [AuthSlotConfig] is serialized, so a restored process
     * reopens the sheet it was showing. `SelectProvider` rebuilds its list from the use case and
     * `Login` keeps its own state (R-092, R-093).
     */
    override val selectProvider: Value<ChildSlot<*, SelectProviderComponent>> = childSlot(
        source = slotNavigation,
        serializer = AuthSlotConfig.serializer(),
        key = SELECT_PROVIDER_KEY,
    ) { _, childComponentContext ->
        selectProviderComponentFactory.invoke(
            componentContext = childComponentContext,
            output = ::onSelectProviderOutput,
        )
    }

    private fun onLoginOutput(output: LoginOutput) {
        when (output) {
            is LoginOutput.OpenProviderSelection -> {
                slotNavigation.activate(AuthSlotConfig.SelectProviderConfig)
            }
        }
    }

    private fun onSelectProviderOutput(output: SelectProviderOutput) {
        // The presented slot is the duplicate-action guard: a dismissed sheet has no say any more.
        if (selectProvider.value.child == null) return

        when (output) {
            is SelectProviderOutput.Dismissed -> {
                slotNavigation.dismiss()
            }
            is SelectProviderOutput.ProviderSelected -> {
                slotNavigation.dismiss()
                login.dispatch(LoginEvent.ProviderSelected(providerId = output.providerId))
            }
        }
    }

    class Factory(
        private val loginComponentFactory: LoginComponent.Factory,
        private val selectProviderComponentFactory: SelectProviderComponent.Factory,
    ) : AuthComponent.Factory {

        override fun invoke(componentContext: ComponentContext): AuthComponent = DefaultAuthComponent(
            componentContext = componentContext,
            loginComponentFactory = loginComponentFactory,
            selectProviderComponentFactory = selectProviderComponentFactory,
        )
    }

    companion object {
        private const val LOGIN_KEY = "login"
        private const val SELECT_PROVIDER_KEY = "select-provider"
    }
}
