package app.yap.feature.auth.presentation.selectprovider

import app.yap.core.test.runViewModelTest
import app.yap.feature.auth.api.entity.AuthProvider
import app.yap.feature.auth.domain.usecase.StubObserveAuthProvidersUseCase
import app.yap.feature.auth.presentation.StubNavigator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runCurrent

internal class SelectAuthProviderViewModelTest {

    @Test
    fun `GIVEN the roster WHEN the screen is opened THEN every offered provider is listed`() = runViewModelTest {
        val env = Environment(providers = listOf(GOOGLE, T_ID))

        runCurrent()

        assertEquals(
            expected = listOf(GOOGLE, T_ID),
            actual = env.viewModel.uiState.value.providers.map { row -> row.provider },
        )
    }

    @Test
    fun `GIVEN the roster changes WHEN it is observed THEN the listing follows`() = runViewModelTest {
        val env = Environment(providers = listOf(GOOGLE))
        runCurrent()

        env.observeAuthProvidersUseCase.providers.value = listOf(GOOGLE, T_ID)
        runCurrent()

        assertEquals(
            expected = listOf(GOOGLE, T_ID),
            actual = env.viewModel.uiState.value.providers.map { row -> row.provider },
        )
    }

    @Test
    fun `GIVEN a provider is chosen WHEN the choice is reported THEN the screen leaves`() = runViewModelTest {
        val env = Environment(providers = listOf(GOOGLE))
        runCurrent()

        env.viewModel.onEvent(SelectAuthProviderViewModel.Event.ProviderChosen)
        runCurrent()

        env.navigator.backCall.called(times = 1)
        env.navigator.navigateCall.notCalled()
    }

    private class Environment(
        providers: List<AuthProvider>,
    ) {

        val navigator = StubNavigator()
        val observeAuthProvidersUseCase = StubObserveAuthProvidersUseCase(providers = providers)
        val viewModel = SelectAuthProviderViewModel(
            navigator = navigator,
            observeAuthProvidersUseCase = observeAuthProvidersUseCase,
        )
    }

    private companion object {
        val GOOGLE: AuthProvider = AuthProvider.Google(isEnabled = true, isVisible = true)
        val T_ID: AuthProvider = AuthProvider.TId(isEnabled = false, isVisible = true)
    }
}
