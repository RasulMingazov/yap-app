package app.yap.feature.auth.presentation.selectprovider

import app.yap.feature.auth.domain.entity.LoginProviderId
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.Value
import org.jetbrains.compose.resources.StringResource

/**
 * The modal provider sheet. It owns the provider list only; dismissal and the consequences of a
 * selection belong to its parent (R-088, R-090, R-091).
 */
interface SelectProviderComponent {

    val uiState: Value<UiState>

    fun dispatch(event: Event)

    data class UiState(
        val emptyMessage: StringResource?,
        val providers: List<Provider>,
        val title: StringResource,
    ) {

        /** One row, carrying every repeatable fact the sheet needs to render it (R-069, AC-041). */
        data class Provider(
            val displayName: String,
            val iconToken: String,
            val id: LoginProviderId,
            val isEnabled: Boolean,
            val key: String,
        )
    }

    sealed interface Event {

        data object DismissRequested : Event

        data class ProviderClicked(val providerId: LoginProviderId) : Event
    }

    sealed interface Output {

        data object Dismissed : Output

        data class ProviderSelected(val providerId: LoginProviderId) : Output
    }

    interface Factory {

        fun create(
            componentContext: ComponentContext,
            output: (Output) -> Unit,
        ): SelectProviderComponent
    }
}
