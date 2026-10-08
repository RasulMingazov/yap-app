package app.yap.core.design.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class YapColors(
    val accent: Color,
    val action: Color,
    val background: Color,
    val bodyMuted: Color,
    val caption: Color,
    val handle: Color,
    val highlight: Color,
    val link: Color,
    val notice: Color,
    val onAction: Color,
    val onBackground: Color,
    val onHighlight: Color,
    val onNotice: Color,
    val onSurface: Color,
    val outline: Color,
    val pressed: Color,
    val scrim: Color,
    val sectionLabel: Color,
    val surface: Color,
)

internal val LightYapColors = YapColors(
    accent = Color(0xFF0B0A0D),
    action = Color(0xFF0B0A0D),
    background = Color(0xFFFFFFFF),
    bodyMuted = Color(0xFF5F5A6B),
    caption = Color(0xFF77717F),
    handle = Color(0xFF0B0A0D).copy(alpha = 0.20f),
    highlight = Color(0xFFD9FF57),
    link = Color(0xFF0B0A0D),
    notice = Color(0xFFD9FF57),
    onAction = Color(0xFFD9FF57),
    onBackground = Color(0xFF0B0A0D),
    onHighlight = Color(0xFF0B0A0D),
    onNotice = Color(0xFF0B0A0D),
    onSurface = Color(0xFF0B0A0D),
    outline = Color(0xFF0B0A0D).copy(alpha = 0.10f),
    pressed = Color(0xFF5F5A6B),
    scrim = Color(0xFF0B0A0D).copy(alpha = 0.35f),
    sectionLabel = Color(0xFF77717F),
    surface = Color(0xFFFFFFFF),
)

internal val DarkYapColors = YapColors(
    accent = Color(0xFFD9FF57),
    action = Color(0xFFD9FF57),
    background = Color(0xFF08070A),
    bodyMuted = Color(0xFF8F8899),
    caption = Color(0xFF5B5765),
    handle = Color(0xFFE2E2E2).copy(alpha = 0.25f),
    highlight = Color(0xFFD9FF57),
    link = Color(0xFFFAF9F6),
    notice = Color(0xFFD9FF57),
    onAction = Color(0xFF0B0A0D),
    onBackground = Color(0xFFFAF9F6),
    onHighlight = Color(0xFF0B0A0D),
    onNotice = Color(0xFF0B0A0D),
    onSurface = Color(0xFFFAF9F6),
    outline = Color(0xFFE2E2E2).copy(alpha = 0.14f),
    pressed = Color(0xFFD9FF57),
    scrim = Color(0xFF050406).copy(alpha = 0.55f),
    sectionLabel = Color(0xFF7C7787),
    surface = Color(0xFF15141A),
)
