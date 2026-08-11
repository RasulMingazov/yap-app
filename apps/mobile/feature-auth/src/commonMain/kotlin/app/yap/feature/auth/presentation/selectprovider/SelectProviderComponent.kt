package app.yap.feature.auth.presentation.selectprovider

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.Value

/**
 * The modal provider sheet. It owns the provider list only; dismissal and the consequences of a
 * selection belong to its parent (R-088, R-090, R-091).
 */
interface SelectProviderComponent {

    val uiState: Value<SelectProviderUiState>

    fun dispatch(event: SelectProviderEvent)

    interface Factory {

        operator fun invoke(
            componentContext: ComponentContext,
            output: (SelectProviderOutput) -> Unit,
        ): SelectProviderComponent
    }
}
