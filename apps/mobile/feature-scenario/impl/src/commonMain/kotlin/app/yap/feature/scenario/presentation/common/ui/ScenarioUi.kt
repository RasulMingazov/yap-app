package app.yap.feature.scenario.presentation.common.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.yap.core.design.theme.YapTheme

private val CtaHeight = 54.dp
private val SnackbarPadding = 20.dp

@Composable
internal fun PipRow(
    pips: List<Boolean>,
    doneColor: Color,
    trackColor: Color,
    height: Dp = 6.dp,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.fillMaxWidth()) {
        pips.forEach { done ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(height)
                    .background(if (done) doneColor else trackColor, RoundedCornerShape(3.dp)),
            )
        }
    }
}

@Composable
internal fun CtaButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = YapTheme.colors
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(CtaHeight)
            .background(colors.cta, CircleShape)
            .clickableNoIndication(onClick)
            .padding(horizontal = 28.dp),
    ) {
        Text(
            text = label.uppercase(),
            color = colors.onCta,
            fontSize = 17.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = (-0.4).sp,
        )
    }
}

@Composable
internal fun ScenarioSnackbarHost(snackbarHostState: SnackbarHostState, modifier: Modifier = Modifier) {
    val colors = YapTheme.colors
    SnackbarHost(hostState = snackbarHostState, modifier = modifier) { data ->
        Snackbar(
            containerColor = colors.accentFill,
            contentColor = colors.onAccentFill,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.padding(horizontal = SnackbarPadding),
        ) {
            Text(
                text = data.visuals.message,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
internal fun Modifier.clickableNoIndication(onClick: () -> Unit): Modifier = clickable(
    indication = null,
    interactionSource = remember { MutableInteractionSource() },
    onClick = onClick,
)
