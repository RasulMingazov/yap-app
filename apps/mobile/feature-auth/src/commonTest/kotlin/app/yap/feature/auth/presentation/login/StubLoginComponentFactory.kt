package app.yap.feature.auth.presentation.login

import com.arkivanov.decompose.ComponentContext
import io.github.rasulmingazov.stubcall.StubCall1

internal class StubLoginComponentFactory(
    val component: StubLoginComponent = StubLoginComponent(),
) : LoginComponent.Factory {

    val createCall = StubCall1.returns<ComponentContext, LoginComponent>(component)

    override fun create(
        componentContext: ComponentContext,
        output: (LoginComponent.Output) -> Unit,
    ): LoginComponent {
        component.output = output
        return createCall.invoke(componentContext)
    }
}
