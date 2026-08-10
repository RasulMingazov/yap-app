package app.yap.feature.auth.presentation.login

import app.yap.feature.auth.domain.entity.LoginProviderId
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.Value
import kotlinx.coroutines.flow.Flow
import org.jetbrains.compose.resources.StringResource

/**
 * The entry screen. It owns its own copy, the login attempt, loading, failure, and cancellation
 * feedback, and its own duplicate-attempt guard. It knows nothing about the provider sheet: it only
 * asks its parent to open provider selection (R-088, R-090, R-091, R-095).
 */
interface LoginComponent {

    val news: Flow<News>
    val uiState: Value<UiState>

    fun dispatch(event: Event)

    data class UiState(
        val body: StringResource,
        val button: Button,
        val caption: StringResource,
        val hero: StringResource,
        val marquee: StringResource,
        val topics: List<StringResource>,
    ) {

        /** The action button is either labelled or loading; loading replaces the label (R-027, AC-044). */
        sealed interface Button {

            data class Label(val text: StringResource) : Button

            data object Loading : Button
        }
    }

    sealed interface Event {

        data object LoginClicked : Event

        data class ProviderSelected(val providerId: LoginProviderId) : Event
    }

    sealed interface News {

        data class ShowSnackbar(
            val formatArgs: List<String>,
            val message: StringResource,
        ) : News
    }

    sealed interface Output {

        data object OpenProviderSelection : Output
    }

    interface Factory {

        fun create(
            componentContext: ComponentContext,
            output: (Output) -> Unit,
        ): LoginComponent
    }
}
