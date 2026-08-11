package app.yap.feature.auth.presentation.login

import kotlin.test.Test
import kotlin.test.assertEquals

internal class LoginUiStateMapperTest {

    @Test
    fun `GIVEN an idle screen WHEN mapping it THEN the design copy and the login label are shown`() {
        val dataState = StubLoginDataState.stubLoginDataState(isLoading = false)

        val uiState = dataState.toUiState()

        assertEquals(
            expected = StubLoginUiState.stubLoginUiState(button = StubLoginUiState.stubLabelButton()),
            actual = uiState,
        )
    }

    @Test
    fun `GIVEN a running attempt WHEN mapping it THEN the loading state replaces the button label`() {
        val dataState = StubLoginDataState.stubLoginDataState(isLoading = true)

        val uiState = dataState.toUiState()

        assertEquals(expected = StubLoginUiState.stubLoadingUiState(), actual = uiState)
    }
}
