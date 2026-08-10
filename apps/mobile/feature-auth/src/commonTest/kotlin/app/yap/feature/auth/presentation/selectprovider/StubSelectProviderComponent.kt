package app.yap.feature.auth.presentation.selectprovider

import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import io.github.rasulmingazov.stubcall.StubCall1

internal class StubSelectProviderComponent(
    /** Emits an [SelectProviderComponent.Output] as the real component would. */
    val output: (SelectProviderComponent.Output) -> Unit = {},
    uiState: SelectProviderComponent.UiState = SelectProviderStubs.stubEmptyUiState(),
) : SelectProviderComponent {

    val dispatchCall = StubCall1.unit<SelectProviderComponent.Event>()
    val mutableUiState = MutableValue(uiState)

    override val uiState: Value<SelectProviderComponent.UiState> = mutableUiState

    override fun dispatch(event: SelectProviderComponent.Event) = dispatchCall.invoke(event)
}
