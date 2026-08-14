package app.yap.feature.auth.presentation.selectprovider.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import app.yap.core.design.theme.YapTheme
import app.yap.feature.auth.api.entity.AuthProvider
import app.yap.feature.auth.presentation.ComposeUiTestCase
import app.yap.feature.auth.presentation.selectprovider.SelectAuthProviderUiStateMapper
import app.yap.feature.auth.presentation.selectprovider.SelectAuthProviderViewModel
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
internal class SelectAuthProviderScreenTest : ComposeUiTestCase() {

    @Test
    fun `GIVEN the roster WHEN the screen is shown THEN it is headed and names every provider`() = runComposeUiTest {
        setContent { TestHost(uiState = stubUiState()) }

        onNodeWithText("СПОСОБЫ ВХОДА").assertIsDisplayed()
        onNodeWithText("Google").assertIsDisplayed()
        onNodeWithText("T-ID").assertIsDisplayed()
    }

    @Test
    fun `GIVEN the roster WHEN it is read aloud THEN each row names the provider it starts`() = runComposeUiTest {
        setContent { TestHost(uiState = stubUiState()) }

        onNodeWithContentDescription("Войти через Google").assertIsDisplayed()
        onNodeWithContentDescription("Войти через T-ID").assertIsDisplayed()
    }

    @Test
    fun `GIVEN a provider that may not be chosen WHEN its row is tapped THEN the choice is still reported`() =
        runComposeUiTest {
            val chosen = mutableListOf<AuthProvider>()
            setContent { TestHost(uiState = stubUiState(), onProviderChosen = { provider -> chosen += provider }) }

            onNodeWithTag(SelectAuthProviderTestTags.rowOf(T_ID)).performClick()

            assertEquals(expected = listOf(T_ID), actual = chosen)
        }

    @Test
    fun `GIVEN a provider that may be chosen WHEN its row is tapped THEN the choice is reported`() = runComposeUiTest {
        val chosen = mutableListOf<AuthProvider>()
        setContent { TestHost(uiState = stubUiState(), onProviderChosen = { provider -> chosen += provider }) }

        onNodeWithTag(SelectAuthProviderTestTags.rowOf(GOOGLE)).performClick()

        assertEquals(expected = listOf(GOOGLE), actual = chosen)
    }

    @androidx.compose.runtime.Composable
    private fun TestHost(
        uiState: SelectAuthProviderViewModel.UiState,
        onProviderChosen: (AuthProvider) -> Unit = {},
    ) {
        YapTheme {
            SelectAuthProviderContent(uiState = uiState, onProviderChosen = onProviderChosen)
        }
    }

    private fun stubUiState(): SelectAuthProviderViewModel.UiState = SelectAuthProviderUiStateMapper(
        SelectAuthProviderViewModel.DataState(providers = listOf(GOOGLE, APPLE, T_ID)),
    )

    private companion object {
        val APPLE: AuthProvider = AuthProvider.Apple(isEnabled = false, isVisible = false)
        val GOOGLE: AuthProvider = AuthProvider.Google(isEnabled = true, isVisible = true)
        val T_ID: AuthProvider = AuthProvider.TId(isEnabled = false, isVisible = true)
    }
}
