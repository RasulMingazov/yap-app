package app.yap.app.root.navigation

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp as lerpUnit
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import app.yap.core.design.theme.YapTheme
import org.koin.compose.koinInject
import org.koin.compose.navigation3.koinEntryProvider
import org.koin.core.annotation.KoinExperimentalAPI

private val TabBarPadding = 6.dp
private val TabSize = 52.dp
private val TabGap = 8.dp
private val TabBarBottomOffset = 18.dp
private val BottomFadeHeight = 96.dp
private const val COLLAPSE_RANGE_PX = 60f
private const val TITLE_MIN_SCALE = 0.56f
private const val TOOLBAR_MAX_DP = 64f
private const val TOOLBAR_MIN_DP = 44f

@OptIn(KoinExperimentalAPI::class)
@Composable
internal fun MainScaffold() {
    val rootBackStack = koinInject<RootBackStack>()
    val selectedTab by rootBackStack.selectedTab.collectAsStateWithLifecycle()
    val entryProvider = koinEntryProvider<NavKey>()
    val stateHolder = rememberSaveableStateHolder()
    val colors = YapTheme.colors

    Box(modifier = Modifier.fillMaxSize().background(colors.background)) {
        stateHolder.SaveableStateProvider(selectedTab.name) {
            Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
                when (selectedTab) {
                    MainTab.Home -> entryProvider(MainTab.Home.rootKey).Content()
                    MainTab.Scenarios -> CollapsingTitle(title = "Сценарии") {
                        entryProvider(MainTab.Scenarios.rootKey).Content()
                    }
                    MainTab.Profile -> CollapsingTitle(title = "Профиль") {
                        ProfilePlaceholderScreen()
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(BottomFadeHeight)
                .background(
                    Brush.verticalGradient(colors = listOf(Color.Transparent, colors.background)),
                ),
        )

        MainTabBar(
            onSelect = rootBackStack::selectTab,
            selected = selectedTab,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = TabBarBottomOffset),
        )
    }
}

@Composable
private fun CollapsingTitle(title: String, content: @Composable () -> Unit) {
    val collapsed = remember { mutableFloatStateOf(0f) }
    val connection = remember {
        object : NestedScrollConnection {

            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                collapsed.value = (collapsed.value - available.y).coerceIn(0f, COLLAPSE_RANGE_PX)
                return Offset.Zero
            }
        }
    }
    val progress = collapsed.value / COLLAPSE_RANGE_PX
    val scale = 1f - (1f - TITLE_MIN_SCALE) * progress

    Column(modifier = Modifier.fillMaxSize().nestedScroll(connection)) {
        Box(
            contentAlignment = Alignment.CenterStart,
            modifier = Modifier
                .fillMaxWidth()
                .height(lerpUnit(TOOLBAR_MAX_DP.dp, TOOLBAR_MIN_DP.dp, progress))
                .padding(horizontal = 20.dp),
        ) {
            Text(
                text = title,
                color = YapTheme.colors.onBackground,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-1).sp,
                modifier = Modifier.graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0.5f)
                },
            )
        }
        content()
    }
}

@Composable
private fun MainTabBar(
    onSelect: (MainTab) -> Unit,
    selected: MainTab,
    modifier: Modifier = Modifier,
) {
    val colors = YapTheme.colors
    val selectedIndex = MainTab.entries.indexOf(selected)
    val indicatorOffset by animateDpAsState(
        targetValue = TabBarPadding + (TabSize + TabGap) * selectedIndex,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 380f),
    )

    Box(
        modifier = modifier
            .background(colors.navBar, CircleShape)
            .border(1.dp, colors.navBorder, CircleShape),
    ) {
        Box(
            modifier = Modifier
                .padding(top = TabBarPadding)
                .offset(x = indicatorOffset)
                .size(TabSize)
                .background(colors.accentFill, CircleShape),
        )
        Row(modifier = Modifier.padding(TabBarPadding)) {
            MainTab.entries.forEachIndexed { index, tab ->
                if (index > 0) Box(modifier = Modifier.size(TabGap))
                TabButton(
                    isSelected = tab == selected,
                    onClick = { onSelect(tab) },
                    tab = tab,
                )
            }
        }
    }
}

@Composable
private fun TabButton(
    isSelected: Boolean,
    onClick: () -> Unit,
    tab: MainTab,
) {
    val colors = YapTheme.colors
    val label = when (tab) {
        MainTab.Home -> "Главная"
        MainTab.Scenarios -> "Сценарии"
        MainTab.Profile -> "Профиль"
    }
    val icon = when (tab) {
        MainTab.Home -> HomeTabIcon
        MainTab.Scenarios -> ScenariosTabIcon
        MainTab.Profile -> ProfileTabIcon
    }
    val tint = if (isSelected) colors.onAccentFill else colors.navIdle

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(TabSize)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick,
            )
            .semantics { contentDescription = label },
    ) {
        androidx.compose.foundation.Image(
            painter = rememberVectorPainter(icon),
            contentDescription = null,
            colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(tint),
        )
    }
}
