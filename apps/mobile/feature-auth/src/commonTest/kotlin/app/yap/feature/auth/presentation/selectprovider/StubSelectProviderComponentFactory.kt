package app.yap.feature.auth.presentation.selectprovider

import com.arkivanov.decompose.ComponentContext
import io.github.rasulmingazov.stubcall.StubCall1

internal class StubSelectProviderComponentFactory : SelectProviderComponent.Factory {

    val createCall = StubCall1.unit<ComponentContext>()

    override fun create(
        componentContext: ComponentContext,
        output: (SelectProviderComponent.Output) -> Unit,
    ): SelectProviderComponent {
        createCall.invoke(componentContext)
        return StubSelectProviderComponent(output = output)
    }
}
