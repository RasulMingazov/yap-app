package app.yap.feature.scenario.presentation.scenarios.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.result.ResultEffect
import app.yap.core.common.navigation.TabReselects
import app.yap.core.design.theme.YapTheme
import app.yap.feature.scenario.api.ScenariosNavKey
import app.yap.feature.scenario.api.entity.ScenarioId
import app.yap.feature.scenario.presentation.common.ScenarioResultKeys
import app.yap.feature.scenario.presentation.common.ui.CtaButton
import app.yap.feature.scenario.presentation.common.ui.ScenarioSnackbarHost
import app.yap.feature.scenario.presentation.common.ui.clickableNoIndication
import app.yap.feature.scenario.presentation.scenarios.ScenariosViewModel
import app.yap.feature.scenario.presentation.scenarios.ScenariosViewModel.Event
import app.yap.feature.scenario.presentation.scenarios.ScenariosViewModel.UiState
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

private val ScreenPadding = 20.dp
private val BottomContentPadding = 110.dp

@Composable
internal fun ScenariosScreen() {
    val viewModel = koinViewModel<ScenariosViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val tabReselects = koinInject<TabReselects>()
    val listState = rememberLazyListState()

    LaunchedEffect(viewModel) {
        viewModel.onEvent(Event.ScreenShown)
    }
    ResultEffect<String>(resultKey = ScenarioResultKeys.REPEAT_CONFIRMATION) { scenarioId ->
        viewModel.onEvent(Event.RepeatConfirmed(ScenarioId(scenarioId)))
    }
    LaunchedEffect(viewModel) {
        viewModel.news.collect { news ->
            when (news) {
                is ScenariosViewModel.News.ShowMessage -> snackbarHostState.showSnackbar(news.text)
            }
        }
    }
    LaunchedEffect(tabReselects) {
        tabReselects.reselects.collect { key ->
            if (key == ScenariosNavKey) listState.animateScrollToItem(0)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (val content = uiState.content) {
            is UiState.Content.Loading -> ScenariosLoading()
            is UiState.Content.Unavailable -> ScenariosRetry(
                onRetry = { viewModel.onEvent(Event.RetryClicked) },
            )
            is UiState.Content.Ready -> Column(modifier = Modifier.fillMaxSize()) {
                FilterRail(filters = content.filters, onEvent = viewModel::onEvent)
                LazyColumn(
                    contentPadding = PaddingValues(bottom = BottomContentPadding),
                    state = listState,
                ) {
                    content.groups.forEach { group ->
                        if (content.showHeaders) {
                            item(key = "header-${group.title}") {
                                GroupHeader(group = group)
                            }
                        }
                        items(count = group.rows.size, key = { index -> group.rows[index].id }) { index ->
                            ScenarioRow(
                                row = group.rows[index],
                                onClick = { id -> viewModel.onEvent(Event.RowClicked(ScenarioId(id))) },
                            )
                        }
                        item(key = "gap-${group.title}") {
                            Box(modifier = Modifier.padding(bottom = 26.dp))
                        }
                    }
                }
            }
        }

        ScenarioSnackbarHost(
            snackbarHostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = BottomContentPadding - ScreenPadding),
        )
    }
}

@Composable
private fun ScenariosLoading() {
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
private fun ScenariosRetry(onRetry: () -> Unit) {
    val colors = YapTheme.colors
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize().padding(horizontal = ScreenPadding),
    ) {
        Text(
            text = "Не получилось загрузить сценарии",
            color = colors.onBackground,
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
        )
        Text(
            text = "Проверьте подключение и попробуйте ещё раз.",
            color = colors.muted,
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 8.dp),
        )
        CtaButton(label = "Повторить", onClick = onRetry, modifier = Modifier.padding(top = 20.dp))
    }
}

@Composable
private fun FilterRail(filters: List<UiState.FilterUi>, onEvent: (Event) -> Unit) {
    val colors = YapTheme.colors
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        contentPadding = PaddingValues(start = ScreenPadding, end = ScreenPadding, top = 6.dp, bottom = 12.dp),
    ) {
        items(filters.size, key = { index -> filters[index].filter.name }) { index ->
            val filter = filters[index]
            val background = if (filter.isSelected) colors.onBackground else Color.Transparent
            val ink = if (filter.isSelected) colors.background else colors.onBackground
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .heightIn(min = 38.dp)
                    .background(background, CircleShape)
                    .border(
                        width = 1.dp,
                        color = if (filter.isSelected) colors.onBackground else colors.hairlineStrong,
                        shape = CircleShape,
                    )
                    .clickableNoIndication { onEvent(Event.FilterSelected(filter.filter)) }
                    .padding(horizontal = 14.dp),
            ) {
                Text(
                    text = filter.label,
                    color = ink,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    text = filter.count,
                    color = ink,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.alpha(0.6f),
                )
            }
        }
    }
}

@Composable
private fun GroupHeader(group: UiState.GroupUi) {
    val colors = YapTheme.colors
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = ScreenPadding, end = ScreenPadding, bottom = 4.dp),
    ) {
        Text(
            text = group.title.uppercase(),
            color = colors.faint,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.1.sp,
        )
        Text(
            text = group.countLabel.uppercase(),
            color = colors.faint,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.7.sp,
        )
    }
}

@Composable
private fun ScenarioRow(row: UiState.RowUi, onClick: (String) -> Unit) {
    val colors = YapTheme.colors
    val titleColor = if (row.isDimmed) colors.muted else colors.onBackground
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 58.dp)
            .clickableNoIndication { onClick(row.id) }
            .padding(horizontal = ScreenPadding, vertical = 11.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
            Text(
                text = row.title.uppercase(),
                color = titleColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                lineHeight = 19.sp,
            )
            Text(
                text = row.meta.uppercase(),
                color = if (row.isMetaAccented) colors.accent else colors.muted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.7.sp,
                lineHeight = 14.sp,
            )
        }
        RowBadge(badge = row.badge)
    }
}

@Composable
private fun RowBadge(badge: UiState.RowBadge) {
    val colors = YapTheme.colors
    when (badge) {
        UiState.RowBadge.Chevron -> Text(
            text = "›",
            color = colors.onBackground,
            fontSize = 22.sp,
            fontWeight = FontWeight.Black,
        )
        UiState.RowBadge.Lock -> Text(text = "🔒", fontSize = 13.sp, modifier = Modifier.alpha(0.7f))
        UiState.RowBadge.Done -> Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(22.dp).background(colors.accentFill, CircleShape),
        ) {
            Text(
                text = "✓",
                color = colors.onAccentFill,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
            )
        }
    }
}
