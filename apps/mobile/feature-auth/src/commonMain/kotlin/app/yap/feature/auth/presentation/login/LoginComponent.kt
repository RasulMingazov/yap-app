package app.yap.feature.auth.presentation.login

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.Value
import kotlinx.coroutines.flow.Flow

/**
 * The entry screen. It owns its own copy, the login attempt, loading, failure, and cancellation
 * feedback, and its own duplicate-attempt guard. It knows nothing about the provider sheet: it only
 * asks its parent to open provider selection (R-088, R-090, R-091, R-095).
 */
interface LoginComponent {

    val news: Flow<LoginNews>
    val uiState: Value<LoginUiState>

    fun dispatch(event: LoginEvent)

    interface Factory {

        operator fun invoke(
            componentContext: ComponentContext,
            output: (LoginOutput) -> Unit,
        ): LoginComponent
    }
}
