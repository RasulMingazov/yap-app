package app.yap.feature.auth.presentation.login

import kotlin.test.Test
import kotlin.test.assertEquals

internal class LoginUiStateMapperTest {

    @Test
    fun `GIVEN an idle screen WHEN mapping it THEN the design copy and the login label are shown`() {
        val dataState = LoginStubs.stubDataState(isLoading = false)

        val uiState = dataState.toUiState()

        assertEquals(
            expected = LoginStubs.stubUiState(button = LoginStubs.stubLabelButton()),
            actual = uiState,
        )
    }

    @Test
    fun `GIVEN a running attempt WHEN mapping it THEN the loading state replaces the button label`() {
        val dataState = LoginStubs.stubDataState(isLoading = true)

        val uiState = dataState.toUiState()

        assertEquals(
            expected = LoginStubs.stubUiState(button = LoginComponent.UiState.Button.Loading),
            actual = uiState,
        )
    }
}
