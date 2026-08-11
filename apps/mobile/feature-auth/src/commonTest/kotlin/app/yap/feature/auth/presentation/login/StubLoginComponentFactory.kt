package app.yap.feature.auth.presentation.login

import com.arkivanov.decompose.ComponentContext
import io.github.rasulmingazov.stubcall.StubCall1

internal class StubLoginComponentFactory(
    val component: StubLoginComponent = StubLoginComponent(),
) : LoginComponent.Factory {

    val invokeCall = StubCall1.returns<ComponentContext, LoginComponent>(component)

    override fun invoke(
        componentContext: ComponentContext,
        output: (LoginOutput) -> Unit,
    ): LoginComponent {
        component.output = output
        return invokeCall.invoke(componentContext)
    }
}
