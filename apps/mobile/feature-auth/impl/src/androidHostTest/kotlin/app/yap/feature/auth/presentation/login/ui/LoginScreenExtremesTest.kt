package app.yap.feature.auth.presentation.login.ui

import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.FontScale
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.test.then
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import app.yap.feature.auth.presentation.ComposeUiTestCase
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
internal class LoginScreenExtremesTest : ComposeUiTestCase() {

    @Test
    fun `GIVEN the narrowest guaranteed width WHEN the screen is shown THEN every element is reachable`() =
        runComposeUiTest {
            setContent {
                DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(NARROW)) {
                    LoginScreenTestHost(uiState = stubLoginUiState())
                }
            }

            assertEveryLoginElementReachable()
        }

    @Test
    fun `GIVEN the largest guaranteed font scale WHEN the screen is shown THEN every element is reachable`() =
        runComposeUiTest {
            setContent {
                DeviceConfigurationOverride(DeviceConfigurationOverride.FontScale(LARGEST_FONT_SCALE)) {
                    LoginScreenTestHost(uiState = stubLoginUiState())
                }
            }

            assertEveryLoginElementReachable()
        }

    @Test
    fun `GIVEN both extremes at once WHEN the screen is shown THEN every element is reachable`() = runComposeUiTest {
        setContent {
            DeviceConfigurationOverride(
                DeviceConfigurationOverride.ForcedSize(NARROW) then
                    DeviceConfigurationOverride.FontScale(LARGEST_FONT_SCALE),
            ) {
                LoginScreenTestHost(uiState = stubLoginUiState())
            }
        }

        assertEveryLoginElementReachable()
    }

    @Test
    fun `GIVEN the dark theme at both extremes WHEN the screen is shown THEN every element is reachable`() =
        runComposeUiTest {
            setContent {
                DeviceConfigurationOverride(
                    DeviceConfigurationOverride.ForcedSize(NARROW) then
                        DeviceConfigurationOverride.FontScale(LARGEST_FONT_SCALE),
                ) {
                    LoginScreenTestHost(uiState = stubLoginUiState(), isDarkTheme = true)
                }
            }

            assertEveryLoginElementReachable()
        }

    private companion object {
        val NARROW = DpSize(width = 320.dp, height = 640.dp)
        const val LARGEST_FONT_SCALE = 2.0f
    }
}
