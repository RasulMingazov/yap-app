package app.yap.feature.scenario.presentation.home.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.yap.core.common.navigation.TabReselects
import app.yap.core.design.theme.YapTheme
import app.yap.feature.scenario.api.HomeNavKey
import app.yap.feature.scenario.api.entity.ScenarioId
import app.yap.feature.scenario.presentation.common.ui.CtaButton
import app.yap.feature.scenario.presentation.common.ui.PipRow
import app.yap.feature.scenario.presentation.common.ui.ScenarioSnackbarHost
import app.yap.feature.scenario.presentation.common.ui.clickableNoIndication
import app.yap.feature.scenario.presentation.home.HomeViewModel
import app.yap.feature.scenario.presentation.home.HomeViewModel.Event
import app.yap.feature.scenario.presentation.home.HomeViewModel.UiState
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

private val ScreenPadding = 20.dp
private val BottomContentPadding = 110.dp
private val CardCorner = 22.dp
private val CtaHeight = 54.dp

@Composable
internal fun HomeScreen() {
    val viewModel = koinViewModel<HomeViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val tabReselects = koinInject<TabReselects>()
    val scrollState = rememberScrollState()

    LaunchedEffect(viewModel) {
        viewModel.onEvent(Event.ScreenShown)
    }
    LaunchedEffect(viewModel) {
        viewModel.news.collect { news ->
            when (news) {
                is HomeViewModel.News.ShowMessage -> snackbarHostState.showSnackbar(news.text)
            }
        }
    }
    LaunchedEffect(tabReselects) {
        tabReselects.reselects.collect { key ->
            if (key == HomeNavKey) scrollState.animateScrollTo(0)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (val content = uiState.content) {
            is UiState.Content.Loading -> HomeLoading()
            is UiState.Content.Unavailable -> HomeRetry(onRetry = { viewModel.onEvent(Event.RetryClicked) })
            is UiState.Content.Ready -> HomeContent(
                content = content,
                onEvent = viewModel::onEvent,
                modifier = Modifier.verticalScroll(scrollState),
            )
        }

        ScenarioSnackbarHost(
            snackbarHostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = BottomContentPadding - ScreenPadding),
        )
    }
}

/** The FR-006 plain progress indicator: the accent — lime in dark, black in light. */
@Composable
private fun HomeLoading() {
    val colors = YapTheme.colors
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
        LinearProgressIndicator(
            color = colors.cta,
            trackColor = colors.track,
            modifier = Modifier.width(160.dp),
        )
    }
}

@Composable
internal fun HomeRetry(onRetry: () -> Unit) {
    val colors = YapTheme.colors
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize().padding(horizontal = ScreenPadding),
    ) {
        Text(
            text = "Не получилось загрузить прогресс",
            color = colors.onBackground,
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "Проверьте подключение и попробуйте ещё раз.",
            color = colors.muted,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
        CtaButton(label = "Повторить", onClick = onRetry, modifier = Modifier.padding(top = 20.dp))
    }
}

@Composable
private fun HomeContent(
    content: UiState.Content.Ready,
    onEvent: (Event) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize().padding(bottom = BottomContentPadding)) {
        HeroSection(hero = content.hero, onPrimary = { onEvent(Event.PrimaryClicked) })

        if (content.activeCards.isNotEmpty()) {
            ActiveSection(content = content, onEvent = onEvent)
        }

        content.lockedPreview?.let { preview ->
            LockedPreviewSection(preview = preview, onEvent = onEvent)
        }

        StreakSection(streak = content.streak)
    }
}

@Composable
private fun HeroSection(hero: UiState.Hero, onPrimary: () -> Unit) {
    val colors = YapTheme.colors
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.padding(start = ScreenPadding, end = ScreenPadding, top = 16.dp, bottom = 22.dp),
    ) {
        hero.eyebrow?.let { eyebrow ->
            Text(
                text = eyebrow.uppercase(),
                color = colors.onPill,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.1.sp,
                modifier = Modifier
                    .background(colors.pill, CircleShape)
                    .padding(horizontal = 10.dp, vertical = 5.dp),
            )
        }
        Text(
            text = hero.title.uppercase(),
            color = colors.onBackground,
            fontSize = 33.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = (-1.5).sp,
            lineHeight = 33.sp,
        )
        hero.meta?.takeIf(String::isNotEmpty)?.let { meta ->
            Text(text = meta, color = colors.muted, fontSize = 14.sp, lineHeight = 20.sp)
        }
        hero.pips?.let { pips -> PipRow(pips = pips, doneColor = colors.pipDone, trackColor = colors.track) }
        CtaButton(label = hero.cta, onClick = onPrimary, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun ActiveSection(content: UiState.Content.Ready, onEvent: (Event) -> Unit) {
    val colors = YapTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(bottom = 24.dp)) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth().padding(horizontal = ScreenPadding),
        ) {
            SectionLabel(text = "Активные сценарии")
            content.slotsLabel?.let { label ->
                Text(
                    text = label.uppercase(),
                    color = if (content.showFullSlotsNotice) colors.accent else colors.muted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.7.sp,
                )
            }
        }
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = ScreenPadding),
            state = rememberLazyListState(),
        ) {
            items(content.activeCards.size) { index ->
                val card = content.activeCards[index]
                ActiveCard(card = card, onClick = { onEvent(Event.CardClicked(ScenarioId(card.id))) })
            }
            if (content.slotAdd != null) {
                item {
                    AddSlotCard(slotAdd = content.slotAdd, onClick = { onEvent(Event.AddSlotClicked) })
                }
            }
        }
        if (content.showFullSlotsNotice) {
            Text(
                text = app.yap.feature.scenario.presentation.home.HomeCopy.FULL_SLOTS_NOTICE,
                color = colors.muted,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                modifier = Modifier.padding(horizontal = ScreenPadding),
            )
        }
    }
}

@Composable
private fun ActiveCard(card: UiState.ActiveCard, onClick: () -> Unit) {
    val colors = YapTheme.colors
    val background = if (card.isHighlighted) colors.accentFill else colors.card
    val ink = if (card.isHighlighted) colors.onAccentFill else colors.onCard
    val trackColor = if (card.isHighlighted) colors.onAccentFill.copy(alpha = 0.18f) else colors.cardTrack
    val doneColor = if (card.isHighlighted) colors.onAccentFill else colors.accentFill

    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .width(220.dp)
            .background(background, RoundedCornerShape(CardCorner))
            .clickableNoIndication(onClick)
            .padding(16.dp),
    ) {
        Text(
            text = card.meta.uppercase(),
            color = ink,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.9.sp,
            modifier = Modifier.alpha(0.7f),
        )
        Text(
            text = card.title.uppercase(),
            color = ink,
            fontSize = 18.sp,
            fontWeight = FontWeight.Black,
            lineHeight = 20.sp,
            modifier = Modifier.height(40.dp),
        )
        PipRow(pips = card.pips, doneColor = doneColor, trackColor = trackColor, height = 5.dp)
    }
}

@Composable
private fun AddSlotCard(slotAdd: UiState.SlotAdd, onClick: () -> Unit) {
    val colors = YapTheme.colors
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .width(120.dp)
            .border(1.5.dp, colors.hairlineStrong, RoundedCornerShape(CardCorner))
            .clickableNoIndication(onClick)
            .padding(16.dp),
    ) {
        Text(
            text = slotAdd.count,
            color = colors.onBackground,
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
        )
        Text(
            text = slotAdd.label.uppercase(),
            color = colors.muted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.7.sp,
            lineHeight = 14.sp,
        )
    }
}

@Composable
private fun LockedPreviewSection(preview: UiState.LockedPreview, onEvent: (Event) -> Unit) {
    val colors = YapTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(bottom = 24.dp)) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth().padding(horizontal = ScreenPadding),
        ) {
            SectionLabel(text = "Откроется с подпиской")
            Text(
                text = preview.moreLabel.uppercase(),
                color = colors.onBackground,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.7.sp,
                modifier = Modifier.clickableNoIndication { onEvent(Event.LockedPreviewAllClicked) },
            )
        }
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = ScreenPadding),
        ) {
            items(preview.cards.size) { index ->
                val card = preview.cards[index]
                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier
                        .width(168.dp)
                        .background(colors.card, RoundedCornerShape(CardCorner))
                        .clickableNoIndication { onEvent(Event.LockedPreviewClicked(ScenarioId(card.id))) }
                        .padding(16.dp),
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = card.meta.uppercase(),
                            color = colors.onCard,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.9.sp,
                            modifier = Modifier.alpha(0.7f),
                        )
                        Text(text = "🔒", fontSize = 11.sp, modifier = Modifier.alpha(0.7f))
                    }
                    Text(
                        text = card.title.uppercase(),
                        color = colors.onCard,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        lineHeight = 20.sp,
                        modifier = Modifier.height(40.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun StreakSection(streak: UiState.StreakBlock) {
    val colors = YapTheme.colors
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .padding(start = ScreenPadding, end = ScreenPadding, bottom = 24.dp)
            .fillMaxWidth()
            .background(colors.chip, RoundedCornerShape(24.dp))
            .padding(18.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = streak.number,
                    color = colors.onBackground,
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Black,
                    lineHeight = 40.sp,
                )
                Text(
                    text = streak.unit.uppercase(),
                    color = colors.muted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.9.sp,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
            }
            SectionLabel(text = "Эта неделя")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            streak.weekDays.forEach { day -> WeekDayCell(day = day, modifier = Modifier.weight(1f)) }
        }
        Text(text = streak.note, color = colors.muted, fontSize = 13.sp, lineHeight = 18.sp)
    }
}

@Composable
private fun WeekDayCell(day: UiState.WeekDayUi, modifier: Modifier = Modifier) {
    val colors = YapTheme.colors
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(if (day.practised) colors.accentFill else Color.Transparent, CircleShape)
                .border(
                    width = 1.5.dp,
                    color = when {
                        day.practised -> colors.accentFill
                        day.isToday -> colors.onBackground
                        else -> colors.hairlineStrong
                    },
                    shape = CircleShape,
                ),
        ) {
            if (day.practised) {
                Text(
                    text = "✓",
                    color = colors.onAccentFill,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                )
            }
        }
        Text(
            text = day.label.uppercase(),
            color = if (day.isToday) colors.onBackground else colors.faint,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.6.sp,
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        color = YapTheme.colors.faint,
        fontSize = 11.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = 1.1.sp,
    )
}

