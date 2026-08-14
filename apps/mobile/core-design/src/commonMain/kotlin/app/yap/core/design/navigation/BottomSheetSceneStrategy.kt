@file:OptIn(ExperimentalMaterial3Api::class)

package app.yap.core.design.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavMetadataKey
import androidx.navigation3.runtime.get
import androidx.navigation3.runtime.metadata
import androidx.navigation3.scene.OverlayScene
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope
import app.yap.core.design.theme.YapTheme

private val HandleBottomGap = 8.dp
private val HandleHeight = 4.dp
private val HandleWidth = 36.dp
private val SheetBorderThickness = 1.dp
private val SheetBottomPadding = 20.dp
private val SheetCornerRadius = 24.dp
private val SheetSidePadding = 20.dp
private val SheetTopPadding = 10.dp

private val SheetShape = RoundedCornerShape(topStart = SheetCornerRadius, topEnd = SheetCornerRadius)
private val SheetInnerShape = RoundedCornerShape(
    topStart = SheetCornerRadius - SheetBorderThickness,
    topEnd = SheetCornerRadius - SheetBorderThickness,
)

private object BottomSheetKey : NavMetadataKey<ModalBottomSheetProperties>

class BottomSheetSceneStrategy<T : Any> : SceneStrategy<T> {

    override fun SceneStrategyScope<T>.calculateScene(entries: List<NavEntry<T>>): Scene<T>? {
        val lastEntry = entries.lastOrNull()
        val properties = lastEntry?.metadata?.get(BottomSheetKey)
        val overlaid = entries.dropLast(1)

        if (lastEntry == null || properties == null || overlaid.isEmpty()) return null

        return BottomSheetScene(
            entry = lastEntry,
            key = lastEntry.contentKey,
            onBack = onBack,
            overlaidEntries = overlaid,
            previousEntries = overlaid,
            properties = properties,
        )
    }
}

fun bottomSheetScene(
    properties: ModalBottomSheetProperties = ModalBottomSheetProperties(),
): Map<String, Any> = metadata { put(BottomSheetKey, properties) }

@OptIn(ExperimentalMaterial3Api::class)
private class BottomSheetScene<T : Any>(
    private val entry: NavEntry<T>,
    override val key: Any,
    private val onBack: () -> Unit,
    override val overlaidEntries: List<NavEntry<T>>,
    override val previousEntries: List<NavEntry<T>>,
    private val properties: ModalBottomSheetProperties,
) : OverlayScene<T> {

    private var sheetState: SheetState? = null

    override val entries: List<NavEntry<T>> = listOf(entry)

    override val content: @Composable () -> Unit = {
        val colors = YapTheme.colors
        val state = rememberModalBottomSheetState()
        sheetState = state

        ModalBottomSheet(
            onDismissRequest = onBack,
            sheetState = state,
            shape = SheetShape,
            containerColor = colors.surface,
            scrimColor = colors.scrim,
            dragHandle = null,
            contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
            properties = properties,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.outline, SheetShape)
                    .padding(top = SheetBorderThickness)
                    .background(colors.surface, SheetInnerShape),
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(
                            bottom = SheetBottomPadding,
                            end = SheetSidePadding,
                            start = SheetSidePadding,
                            top = SheetTopPadding,
                        ),
                ) {
                    Box(
                        modifier = Modifier
                            .padding(bottom = HandleBottomGap)
                            .size(width = HandleWidth, height = HandleHeight)
                            .background(colors.handle, RoundedCornerShape(percent = 50)),
                    )

                    entry.Content()
                }
            }
        }
    }

    override suspend fun onRemove() {
        sheetState?.hide()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as BottomSheetScene<*>

        return key == other.key &&
            entry == other.entry &&
            overlaidEntries == other.overlaidEntries &&
            previousEntries == other.previousEntries &&
            properties == other.properties
    }

    override fun hashCode(): Int {
        var result = key.hashCode()
        result = HASH_FACTOR * result + entry.hashCode()
        result = HASH_FACTOR * result + overlaidEntries.hashCode()
        result = HASH_FACTOR * result + previousEntries.hashCode()
        result = HASH_FACTOR * result + properties.hashCode()
        return result
    }

    private companion object {
        const val HASH_FACTOR = 31
    }
}
