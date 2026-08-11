package app.yap.feature.auth.presentation.selectprovider

import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import io.github.rasulmingazov.stubcall.StubCall1

internal class StubSelectProviderComponent(
    /** Emits a [SelectProviderOutput] as the real component would. */
    val output: (SelectProviderOutput) -> Unit = {},
    uiState: SelectProviderUiState = StubSelectProviderUiState.stubEmptyUiState(),
) : SelectProviderComponent {

    val dispatchCall = StubCall1.unit<SelectProviderEvent>()
    val mutableUiState = MutableValue(uiState)

    override val uiState: Value<SelectProviderUiState> = mutableUiState

    override fun dispatch(event: SelectProviderEvent) = dispatchCall.invoke(event)
}
