package app.yap.feature.auth.presentation.login

import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import io.github.rasulmingazov.stubcall.StubCall1
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

internal class StubLoginComponent(
    uiState: LoginUiState = StubLoginUiState.stubLoginUiState(),
) : LoginComponent {

    val dispatchCall = StubCall1.unit<LoginEvent>()
    val mutableUiState = MutableValue(uiState)

    /** Observes the moment [dispatch] is invoked so a test can inspect the state of its collaborators. */
    var onDispatch: (LoginEvent) -> Unit = {}

    /** Set by the factory so a test can emit a [LoginOutput] as the real component would. */
    var output: (LoginOutput) -> Unit = {}

    override val news: Flow<LoginNews> = emptyFlow()
    override val uiState: Value<LoginUiState> = mutableUiState

    override fun dispatch(event: LoginEvent) {
        onDispatch(event)
        dispatchCall.invoke(event)
    }
}
