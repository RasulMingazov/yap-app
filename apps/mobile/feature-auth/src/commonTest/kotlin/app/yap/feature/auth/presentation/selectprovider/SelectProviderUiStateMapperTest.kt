package app.yap.feature.auth.presentation.selectprovider

import app.yap.feature.auth.domain.entity.StubLoginProvider
import kotlin.test.Test
import kotlin.test.assertEquals

internal class SelectProviderUiStateMapperTest {

    @Test
    fun `GIVEN the Android configuration WHEN mapping it THEN Google and T-ID keep their order without Apple`() {
        val dataState = SelectProviderStubs.stubDataState(
            providers = StubLoginProvider.stubAndroidProviders(),
        )

        val uiState = dataState.toUiState()

        assertEquals(
            expected = SelectProviderStubs.stubUiState(
                emptyMessage = null,
                providers = listOf(
                    SelectProviderStubs.stubGoogleRow(),
                    SelectProviderStubs.stubTidRow(),
                ),
            ),
            actual = uiState,
        )
    }

    @Test
    fun `GIVEN the iOS configuration WHEN mapping it THEN all three providers keep their order`() {
        val dataState = SelectProviderStubs.stubDataState(
            providers = StubLoginProvider.stubIosProviders(),
        )

        val uiState = dataState.toUiState()

        assertEquals(
            expected = SelectProviderStubs.stubUiState(
                emptyMessage = null,
                providers = listOf(
                    SelectProviderStubs.stubGoogleRow(),
                    SelectProviderStubs.stubAppleRow(),
                    SelectProviderStubs.stubTidRow(),
                ),
            ),
            actual = uiState,
        )
    }

    @Test
    fun `GIVEN a configuration order of T-ID before Google WHEN mapping it THEN that order is preserved`() {
        val dataState = SelectProviderStubs.stubDataState(
            providers = listOf(StubLoginProvider.stubTid(), StubLoginProvider.stubGoogle()),
        )

        val uiState = dataState.toUiState()

        assertEquals(
            expected = SelectProviderStubs.stubUiState(
                emptyMessage = null,
                providers = listOf(
                    SelectProviderStubs.stubTidRow(),
                    SelectProviderStubs.stubGoogleRow(),
                ),
            ),
            actual = uiState,
        )
    }

    @Test
    fun `GIVEN no visible provider WHEN mapping it THEN the sheet keeps its title and shows the empty message`() {
        val dataState = SelectProviderStubs.stubDataState(
            providers = listOf(
                StubLoginProvider.stubGoogle(isVisible = false),
                StubLoginProvider.stubApple(isVisible = false),
                StubLoginProvider.stubTid(isVisible = false),
            ),
        )

        val uiState = dataState.toUiState()

        assertEquals(expected = SelectProviderStubs.stubEmptyUiState(), actual = uiState)
    }
}
