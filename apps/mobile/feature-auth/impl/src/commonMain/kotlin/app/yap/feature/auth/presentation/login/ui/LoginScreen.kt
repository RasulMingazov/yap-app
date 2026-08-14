package app.yap.feature.auth.presentation.login.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.yap.feature.auth.generated.resources.Res
import app.yap.feature.auth.generated.resources.login_body
import app.yap.feature.auth.generated.resources.login_caption
import app.yap.feature.auth.generated.resources.login_hero
import app.yap.feature.auth.generated.resources.login_marquee
import app.yap.feature.auth.generated.resources.login_primary_action
import app.yap.feature.auth.generated.resources.login_primary_action_semantics
import app.yap.feature.auth.presentation.login.LoginViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

private const val BANNER_MILLIS = 4_000L
private const val TOPIC_INTERVAL_MILLIS = 2_000L
private const val TOPIC_ROLL_MILLIS = 420
private const val MARQUEE_CYCLE_MILLIS = 7_500
private const val MARQUEE_REPETITIONS = 4

private const val MARQUEE_TILT_DEGREES = -2.6f
private val MarqueeTiltRoom = 10.dp
private val MarqueeOverhang = 24.dp

private val ActionMinHeight = 52.dp
private val ActionToCaptionGap = 14.dp
private val BottomInset = 16.dp

@Composable
internal fun LoginScreen() {
    val viewModel: LoginViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LoginScreenContent(uiState = uiState, onEvent = viewModel::onEvent, news = viewModel.news)
}

@Composable
internal fun LoginScreenContent(
    uiState: LoginViewModel.UiState,
    onEvent: (LoginViewModel.Event) -> Unit,
    news: Flow<LoginViewModel.News> = emptyFlow(),
) {
    val colors = loginColors()
    val message = rememberTransientMessage(news)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .clipToBounds(),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding(),
        ) {
            val bandWidth = maxWidth + MarqueeOverhang
            val viewportHeight = maxHeight
            val metrics = loginMetrics(maxWidth)

            Column(
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .heightIn(min = viewportHeight),
            ) {
                MarqueeBand(
                    isMotionReduced = uiState.isMotionReduced,
                    bandWidth = bandWidth,
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(metrics.sectionGap),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = metrics.contentPadding)
                        .padding(vertical = metrics.sectionGap),
                ) {
                    Text(
                        text = stringResource(Res.string.login_hero),
                        style = metrics.heroStyle,
                        color = colors.onBackground,
                        modifier = Modifier.testTag(LoginTestTags.HERO),
                    )

                    RotatingTopic(uiState = uiState, style = metrics.topicStyle)

                    Text(
                        text = stringResource(Res.string.login_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.muted,
                        modifier = Modifier.testTag(LoginTestTags.BODY),
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(ActionToCaptionGap),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = metrics.contentPadding)
                        .padding(bottom = BottomInset),
                ) {
                    PrimaryAction(
                        isLoggingIn = uiState.isLoggingIn,
                        onClick = { onEvent(LoginViewModel.Event.PrimaryActionClicked) },
                    )

                    Text(
                        text = stringResource(Res.string.login_caption),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.caption,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.testTag(LoginTestTags.CAPTION),
                    )

                    LegalLine(privacyUrl = uiState.privacyUrl, termsUrl = uiState.termsUrl)
                }
            }
        }

        if (message != null) {
            TransientBanner(
                message = message,
                modifier = Modifier.align(Alignment.TopCenter),
            )
        }
    }

    if (uiState.isProviderSheetVisible) {
        AuthProviderSheet(
            providers = uiState.providers,
            onDismiss = { onEvent(LoginViewModel.Event.ProviderSheetDismissed) },
            onProviderChosen = { row ->
                onEvent(LoginViewModel.Event.ProviderChosen(row.provider))
            },
        )
    }
}

private data class LoginMetrics(
    val contentPadding: Dp,
    val heroStyle: TextStyle,
    val sectionGap: Dp,
    val topicStyle: TextStyle,
)

@Composable
private fun loginMetrics(width: Dp): LoginMetrics {
    val typography = MaterialTheme.typography
    val isNarrow = width < 340.dp

    return LoginMetrics(
        contentPadding = if (isNarrow) 20.dp else 24.dp,
        heroStyle = if (isNarrow) typography.displayMedium else typography.displayLarge,
        sectionGap = if (isNarrow) 12.dp else 16.dp,
        topicStyle = if (isNarrow) typography.displaySmall else typography.displayMedium,
    )
}

@Composable
private fun rememberTransientMessage(news: Flow<LoginViewModel.News>): StringResource? {
    var message by remember { mutableStateOf<StringResource?>(null) }
    var shownAt by remember { mutableIntStateOf(0) }

    LaunchedEffect(news) {
        news.collect { item ->
            when (item) {
                is LoginViewModel.News.ShowMessage -> {
                    message = item.message
                    shownAt += 1
                }
            }
        }
    }

    LaunchedEffect(shownAt) {
        if (shownAt == 0) return@LaunchedEffect
        delay(BANNER_MILLIS)
        message = null
    }

    return message
}

@Composable
private fun TransientBanner(
    message: StringResource,
    modifier: Modifier = Modifier,
) {
    val colors = loginColors()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .safeDrawingPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .testTag(LoginTestTags.BANNER),
    ) {
        Text(
            text = stringResource(message),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onBanner,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .background(color = colors.bannerBackground, shape = MaterialTheme.shapes.medium)
                .padding(horizontal = 18.dp, vertical = 12.dp),
        )
    }
}

@Composable
private fun MarqueeBand(
    isMotionReduced: Boolean,
    bandWidth: Dp,
) {
    val colors = loginColors()

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .clipToBounds()
            .padding(vertical = MarqueeTiltRoom)
            .testTag(LoginTestTags.MARQUEE),
    ) {
        Box(
            contentAlignment = Alignment.CenterStart,
            modifier = Modifier
                .requiredWidth(bandWidth)
                .graphicsLayer { rotationZ = MARQUEE_TILT_DEGREES }
                .background(colors.marqueeBackground)
                .clipToBounds()
                .padding(vertical = 7.dp),
        ) {
            if (isMotionReduced) {
                MarqueeText(offsetFraction = 0f)
            } else {
                AnimatedMarqueeText()
            }
        }
    }
}

@Composable
private fun AnimatedMarqueeText() {
    val transition = rememberInfiniteTransition(label = "marquee")
    val offsetFraction by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = MARQUEE_CYCLE_MILLIS, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "marqueeOffset",
    )
    MarqueeText(offsetFraction = offsetFraction)
}

@Composable
private fun MarqueeText(offsetFraction: Float) {
    val text = stringResource(Res.string.login_marquee)

    Text(
        text = List(MARQUEE_REPETITIONS) { text }.joinToString(separator = "   ✦   "),
        style = MaterialTheme.typography.labelMedium,
        color = loginColors().onMarquee,
        maxLines = 1,
        softWrap = false,
        modifier = Modifier
            .wrapContentWidth(align = Alignment.Start, unbounded = true)
            .graphicsLayer {
                translationX = -offsetFraction * size.width / MARQUEE_REPETITIONS
            },
    )
}

@Composable
private fun RotatingTopic(
    uiState: LoginViewModel.UiState,
    style: TextStyle,
) {
    var index by remember { mutableIntStateOf(0) }

    if (!uiState.isMotionReduced) {
        LaunchedEffect(uiState.topics) {
            while (true) {
                delay(TOPIC_INTERVAL_MILLIS)
                index = (index + 1) % uiState.topics.size
            }
        }
    }

    val topic = uiState.topics[index.coerceIn(0, uiState.topics.lastIndex)]

    Box(
        modifier = Modifier
            .clipToBounds()
            .testTag(LoginTestTags.TOPIC),
    ) {
        if (uiState.isMotionReduced) {
            TopicWord(topic = topic, style = style)
        } else {
            AnimatedContent(
                targetState = topic,
                transitionSpec = {
                    val spec = tween<Float>(TOPIC_ROLL_MILLIS)
                    val slideIn = slideInVertically(tween(TOPIC_ROLL_MILLIS)) { height -> height }
                    val slideOut = slideOutVertically(tween(TOPIC_ROLL_MILLIS)) { height -> -height }
                    (slideIn + fadeIn(spec)) togetherWith (slideOut + fadeOut(spec)) using
                        SizeTransform(clip = false)
                },
                label = "topic",
            ) { word -> TopicWord(topic = word, style = style) }
        }
    }
}

@Composable
private fun TopicWord(
    topic: StringResource,
    style: TextStyle,
) {
    Text(
        text = stringResource(topic),
        style = style,
        color = loginColors().accent,
        maxLines = 1,
    )
}

@Composable
private fun PrimaryAction(
    isLoggingIn: Boolean,
    onClick: () -> Unit,
) {
    val colors = loginColors()
    val spokenName = stringResource(Res.string.login_primary_action_semantics)

    Button(
        onClick = onClick,
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = colors.actionBackground,
            contentColor = colors.onAction,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = ActionMinHeight)
            .testTag(LoginTestTags.PRIMARY_ACTION)
            .semantics { contentDescription = spokenName },
    ) {
        if (isLoggingIn) {
            CircularProgressIndicator(
                color = colors.onAction,
                strokeWidth = 2.dp,
                modifier = Modifier
                    .size(20.dp)
                    .testTag(LoginTestTags.PRIMARY_ACTION_PROGRESS),
            )
        } else {
            Text(
                text = stringResource(Res.string.login_primary_action),
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}
