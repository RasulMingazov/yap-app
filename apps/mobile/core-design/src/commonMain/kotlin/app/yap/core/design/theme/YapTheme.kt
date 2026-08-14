package app.yap.core.design.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

val LocalYapColors = staticCompositionLocalOf { LightYapColors }

object YapTheme {

    val colors: YapColors
        @Composable
        @ReadOnlyComposable
        get() = LocalYapColors.current
}

@Composable
fun YapTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkYapColors else LightYapColors

    CompositionLocalProvider(LocalYapColors provides colors) {
        MaterialTheme(
            colorScheme = colors.toColorScheme(isDark = darkTheme),
            typography = YapTypography,
            content = content,
        )
    }
}

private fun YapColors.toColorScheme(isDark: Boolean): ColorScheme {
    val base = if (isDark) darkColorScheme() else lightColorScheme()

    return base.copy(
        background = background,
        onBackground = onBackground,
        surface = surface,
        onSurface = onSurface,
        primary = action,
        onPrimary = onAction,
        secondary = accent,
        onSecondary = onAction,
        inverseSurface = notice,
        inverseOnSurface = onNotice,
        outline = outline,
        outlineVariant = outline,
        scrim = scrim,
    )
}
