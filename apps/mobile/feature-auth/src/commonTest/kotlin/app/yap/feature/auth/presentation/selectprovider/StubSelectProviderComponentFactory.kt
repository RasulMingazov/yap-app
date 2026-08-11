package app.yap.feature.auth.presentation.selectprovider

import com.arkivanov.decompose.ComponentContext
import io.github.rasulmingazov.stubcall.StubCall1

internal class StubSelectProviderComponentFactory : SelectProviderComponent.Factory {

    val invokeCall = StubCall1.unit<ComponentContext>()

    override fun invoke(
        componentContext: ComponentContext,
        output: (SelectProviderOutput) -> Unit,
    ): SelectProviderComponent {
        invokeCall.invoke(componentContext)
        return StubSelectProviderComponent(output = output)
    }
}
