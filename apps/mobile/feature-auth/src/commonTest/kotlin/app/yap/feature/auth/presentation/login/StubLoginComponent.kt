package app.yap.feature.auth.presentation.login

import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import io.github.rasulmingazov.stubcall.StubCall1
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

internal class StubLoginComponent(
    uiState: LoginComponent.UiState = LoginStubs.stubUiState(),
) : LoginComponent {

    val dispatchCall = StubCall1.unit<LoginComponent.Event>()
    val mutableUiState = MutableValue(uiState)

    /** Observes the moment [dispatch] is invoked so a test can inspect the state of its collaborators. */
    var onDispatch: (LoginComponent.Event) -> Unit = {}

    /** Set by the factory so a test can emit an [LoginComponent.Output] as the real component would. */
    var output: (LoginComponent.Output) -> Unit = {}

    override val news: Flow<LoginComponent.News> = emptyFlow()
    override val uiState: Value<LoginComponent.UiState> = mutableUiState

    override fun dispatch(event: LoginComponent.Event) {
        onDispatch(event)
        dispatchCall.invoke(event)
    }
}
