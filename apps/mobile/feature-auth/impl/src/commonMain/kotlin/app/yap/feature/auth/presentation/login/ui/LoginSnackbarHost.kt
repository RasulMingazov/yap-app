package app.yap.feature.auth.presentation.login.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.yap.core.design.theme.YapTheme
import kotlinx.coroutines.delay

private const val ENTER_MILLIS = 220
private const val EXIT_MILLIS = 220
private const val MESSAGE_MILLIS = 2_600L
private const val MESSAGE_TEXT_SIZE = 14

private val MessageCornerRadius = 14.dp
private val MessageHorizontalPadding = 18.dp
private val MessageSideMargin = 20.dp
private val MessageTopMargin = 10.dp
private val MessageVerticalPadding = 12.dp
private val SlideDistance = 8.dp

@Composable
internal fun LoginSnackbarHost(
    hostState: SnackbarHostState,
    isMotionReduced: Boolean,
    modifier: Modifier = Modifier,
) {
    val current = hostState.currentSnackbarData
    var shown by remember { mutableStateOf<SnackbarData?>(null) }
    var isVisible by remember { mutableStateOf(false) }
    if (current != null) shown = current

    LaunchedEffect(current) {
        if (current == null) return@LaunchedEffect
        isVisible = true
        delay(MESSAGE_MILLIS)
        isVisible = false
        if (!isMotionReduced) delay(EXIT_MILLIS.toLong())
        current.dismiss()
    }

    val slideOffset = with(LocalDensity.current) { SlideDistance.roundToPx() }

    AnimatedVisibility(
        visible = isVisible,
        enter = if (isMotionReduced) {
            EnterTransition.None
        } else {
            fadeIn(tween(ENTER_MILLIS, easing = LinearOutSlowInEasing)) +
                slideInVertically(tween(ENTER_MILLIS, easing = LinearOutSlowInEasing)) { slideOffset }
        },
        exit = if (isMotionReduced) {
            ExitTransition.None
        } else {
            fadeOut(tween(EXIT_MILLIS)) +
                slideOutVertically(tween(EXIT_MILLIS)) { height -> -height }
        },
        modifier = modifier,
    ) {
        val message = shown?.visuals?.message.orEmpty()

        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            fontSize = MESSAGE_TEXT_SIZE.sp,
            fontWeight = FontWeight.SemiBold,
            color = YapTheme.colors.onNotice,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .safeDrawingPadding()
                .padding(horizontal = MessageSideMargin)
                .padding(top = MessageTopMargin)
                .background(
                    color = YapTheme.colors.notice,
                    shape = RoundedCornerShape(MessageCornerRadius),
                )
                .padding(
                    horizontal = MessageHorizontalPadding,
                    vertical = MessageVerticalPadding,
                )
                .testTag(LoginTestTags.SNACKBAR),
        )
    }
}
