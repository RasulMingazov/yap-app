package app.yap.feature.auth.presentation.login.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import app.yap.core.design.theme.LocalIsDarkTheme

internal data class LoginColors(
    val accent: Color,
    val actionBackground: Color,
    val background: Color,
    val bannerBackground: Color,
    val caption: Color,
    val link: Color,
    val marqueeBackground: Color,
    val muted: Color,
    val onAction: Color,
    val onBackground: Color,
    val onBanner: Color,
    val onMarquee: Color,
    val onSurface: Color,
    val sheetLabel: Color,
    val surface: Color,
)

private val MarqueeBackground = Color(0xFFD9FF57)
private val OnMarquee = Color(0xFF0B0A0D)

private val LightLoginColors = LoginColors(
    accent = Color(0xFF5E3689),
    actionBackground = Color(0xFF0B0A0D),
    background = Color(0xFFFFFEF7),
    bannerBackground = Color(0xFF5E3689),
    caption = Color(0xFF8B8496),
    link = Color(0xFF0B0A0D),
    marqueeBackground = MarqueeBackground,
    muted = Color(0xFF5F5A6B),
    onAction = Color(0xFFFFFAFC),
    onBackground = Color(0xFF0B0A0D),
    onBanner = Color(0xFFFFFAFC),
    onMarquee = OnMarquee,
    onSurface = Color(0xFF0B0A0D),
    sheetLabel = Color(0xFF8B8496),
    surface = Color(0xFFFFFEF7),
)

private val DarkLoginColors = LoginColors(
    accent = Color(0xFFD9FF57),
    actionBackground = Color(0xFFD9FF57),
    background = Color(0xFF08070A),
    bannerBackground = Color(0xFF26232C),
    caption = Color(0xFF5B5765),
    link = Color(0xFFFAF9F6),
    marqueeBackground = MarqueeBackground,
    muted = Color(0xFF8F8899),
    onAction = Color(0xFF0B0A0D),
    onBackground = Color(0xFFFAF9F6),
    onBanner = Color(0xFFFAF9F6),
    onMarquee = OnMarquee,
    onSurface = Color(0xFFFAF9F6),
    sheetLabel = Color(0xFF7C7787),
    surface = Color(0xFF15141A),
)

@Composable
internal fun loginColors(): LoginColors =
    if (LocalIsDarkTheme.current) DarkLoginColors else LightLoginColors
