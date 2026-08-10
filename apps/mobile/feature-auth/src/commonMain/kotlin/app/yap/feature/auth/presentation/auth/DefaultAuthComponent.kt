package app.yap.feature.auth.presentation.auth

import app.yap.feature.auth.domain.entity.LoginProviderId
import app.yap.feature.auth.presentation.login.LoginComponent
import app.yap.feature.auth.presentation.selectprovider.SelectProviderComponent
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.childContext
import com.arkivanov.decompose.router.slot.ChildSlot
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.operator.map
import com.arkivanov.decompose.value.update
import com.arkivanov.essenty.instancekeeper.InstanceKeeper
import com.arkivanov.essenty.instancekeeper.getOrCreate

private const val LOGIN_KEY = "login"
private const val SELECT_PROVIDER_KEY = "select-provider"

internal class DefaultAuthComponent(
    componentContext: ComponentContext,
    loginComponentFactory: LoginComponent.Factory,
    modelFactory: AuthModel.Factory,
    selectProviderComponentFactory: SelectProviderComponent.Factory,
) : AuthComponent, ComponentContext by componentContext {

    private val model = instanceKeeper.getOrCreate(modelFactory::create)
    private val slotNavigation = SlotNavigation<SelectProviderConfig>()

    override val login: LoginComponent = loginComponentFactory.create(
        componentContext = childContext(key = LOGIN_KEY),
        output = ::onLoginOutput,
    )

    /**
     * The sheet is deliberately not serialized: after process death the retained duplicate-action
     * guard is gone too, and restoring only one of the two would leave them disagreeing. `Login`
     * keeps its own state either way (R-092, R-093).
     */
    override val selectProviderSlot: Value<ChildSlot<*, SelectProviderComponent>> = childSlot(
        source = slotNavigation,
        serializer = null,
        key = SELECT_PROVIDER_KEY,
    ) { _, childComponentContext ->
        selectProviderComponentFactory.create(
            componentContext = childComponentContext,
            output = ::onSelectProviderOutput,
        )
    }

    override val uiState: Value<AuthComponent.UiState> = model.dataState.map(AuthModel.DataState::toUiState)

    private fun onLoginOutput(output: LoginComponent.Output) {
        when (output) {
            is LoginComponent.Output.OpenProviderSelection -> onOpenProviderSelection()
        }
    }

    private fun onSelectProviderOutput(output: SelectProviderComponent.Output) {
        when (output) {
            is SelectProviderComponent.Output.Dismissed -> onDismissed()
            is SelectProviderComponent.Output.ProviderSelected -> onProviderSelected(providerId = output.providerId)
        }
    }

    private fun onOpenProviderSelection() {
        if (!model.startProviderSelection()) return

        slotNavigation.activate(SelectProviderConfig)
    }

    private fun onDismissed() {
        if (!model.finishProviderSelection()) return

        slotNavigation.dismiss()
    }

    /** The sheet is dismissed before `Login` learns about the selection (R-089, AC-057, AC-058). */
    private fun onProviderSelected(providerId: LoginProviderId) {
        if (!model.finishProviderSelection()) return

        slotNavigation.dismiss()
        login.dispatch(LoginComponent.Event.ProviderSelected(providerId = providerId))
    }

    class Factory(
        private val loginComponentFactory: LoginComponent.Factory,
        private val modelFactory: AuthModel.Factory,
        private val selectProviderComponentFactory: SelectProviderComponent.Factory,
    ) : AuthComponent.Factory {

        override fun create(componentContext: ComponentContext): AuthComponent = DefaultAuthComponent(
            componentContext = componentContext,
            loginComponentFactory = loginComponentFactory,
            modelFactory = modelFactory,
            selectProviderComponentFactory = selectProviderComponentFactory,
        )
    }
}

/**
 * Owns the cross-screen duplicate-action guard. Presentation is allowed only while the sheet is
 * absent and dismissal only while it is present, so repeated taps on either side of the transition
 * collapse into a single presentation and a single login attempt (R-090, AC-059).
 */
internal class AuthModel : InstanceKeeper.Instance {

    private val mutableDataState = MutableValue(DataState(isProviderSelectionPresented = false))

    val dataState: Value<DataState> = mutableDataState

    fun startProviderSelection(): Boolean {
        if (mutableDataState.value.isProviderSelectionPresented) return false

        mutableDataState.update { state -> state.copy(isProviderSelectionPresented = true) }
        return true
    }

    fun finishProviderSelection(): Boolean {
        if (!mutableDataState.value.isProviderSelectionPresented) return false

        mutableDataState.update { state -> state.copy(isProviderSelectionPresented = false) }
        return true
    }

    data class DataState(
        val isProviderSelectionPresented: Boolean,
    )

    class Factory {

        fun create(): AuthModel = AuthModel()
    }
}

private fun AuthModel.DataState.toUiState(): AuthComponent.UiState =
    AuthComponent.UiState(isProviderSelectionPresented = isProviderSelectionPresented)

private data object SelectProviderConfig
