package app.yap.core.design.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.yap.core.design.generated.resources.Res
import app.yap.core.design.generated.resources.inter_black
import app.yap.core.design.generated.resources.inter_bold
import app.yap.core.design.generated.resources.inter_regular
import app.yap.core.design.generated.resources.inter_semibold
import org.jetbrains.compose.resources.Font

private const val DISPLAY_TRACKING = -0.05
private const val DISPLAY_SMALL_TRACKING = -0.04
private const val HEADLINE_LARGE_TRACKING = -0.03
private const val HEADLINE_MEDIUM_TRACKING = -0.02
private const val HEADLINE_SMALL_TRACKING = -0.01
private const val BUTTON_TRACKING = -0.025
private const val MARQUEE_TRACKING = -0.005

val YapFontFamily: FontFamily
    @Composable
    get() {
        val regular = Font(Res.font.inter_regular, FontWeight.Normal, FontStyle.Normal)
        val semiBold = Font(Res.font.inter_semibold, FontWeight.SemiBold, FontStyle.Normal)
        val bold = Font(Res.font.inter_bold, FontWeight.Bold, FontStyle.Normal)
        val black = Font(Res.font.inter_black, FontWeight.Black, FontStyle.Normal)

        return remember(regular, semiBold, bold, black) {
            FontFamily(regular, semiBold, bold, black)
        }
    }

val YapTypography: Typography
    @Composable
    get() {
        val fontFamily = YapFontFamily
        return remember(fontFamily) { createYapTypography(fontFamily) }
    }

private fun createYapTypography(fontFamily: FontFamily): Typography = Typography(
    displayLarge = yapTextStyle(fontFamily, FontWeight.Black, 44.sp, 44.sp, DISPLAY_TRACKING.em),
    displayMedium = yapTextStyle(fontFamily, FontWeight.Black, 40.sp, 40.sp, DISPLAY_TRACKING.em),
    displaySmall = yapTextStyle(fontFamily, FontWeight.Black, 36.sp, 40.sp, DISPLAY_SMALL_TRACKING.em),
    headlineLarge = yapTextStyle(fontFamily, FontWeight.Black, 32.sp, 36.sp, HEADLINE_LARGE_TRACKING.em),
    headlineMedium = yapTextStyle(fontFamily, FontWeight.Bold, 28.sp, 32.sp, HEADLINE_MEDIUM_TRACKING.em),
    headlineSmall = yapTextStyle(fontFamily, FontWeight.Bold, 24.sp, 28.sp, HEADLINE_SMALL_TRACKING.em),
    titleLarge = yapTextStyle(fontFamily, FontWeight.Bold, 22.sp, 28.sp),
    titleMedium = yapTextStyle(fontFamily, FontWeight.SemiBold, 16.sp, 24.sp),
    titleSmall = yapTextStyle(fontFamily, FontWeight.SemiBold, 14.sp, 20.sp),
    bodyLarge = yapTextStyle(fontFamily, FontWeight.Normal, 16.sp, 24.sp),
    bodyMedium = yapTextStyle(fontFamily, FontWeight.Normal, 15.sp, 21.sp),
    bodySmall = yapTextStyle(fontFamily, FontWeight.Normal, 13.sp, 18.sp),
    labelLarge = yapTextStyle(fontFamily, FontWeight.Black, 17.sp, 20.sp, BUTTON_TRACKING.em),
    labelMedium = yapTextStyle(fontFamily, FontWeight.Black, 13.sp, 16.sp, MARQUEE_TRACKING.em),
    labelSmall = yapTextStyle(fontFamily, FontWeight.Bold, 12.sp, 16.sp),
)

private fun yapTextStyle(
    fontFamily: FontFamily,
    fontWeight: FontWeight,
    fontSize: TextUnit,
    lineHeight: TextUnit,
    letterSpacing: TextUnit = TextUnit.Unspecified,
): TextStyle = TextStyle(
    fontFamily = fontFamily,
    fontWeight = fontWeight,
    fontSize = fontSize,
    lineHeight = lineHeight,
    letterSpacing = letterSpacing,
)
