package app.yap.app.root

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.runtime.result.rememberResultEventBusNavEntryDecorator
import androidx.navigation3.scene.SinglePaneSceneStrategy
import androidx.navigation3.ui.NavDisplay
import app.yap.app.root.navigation.RootBackStack
import app.yap.core.design.navigation.BottomSheetSceneStrategy
import app.yap.core.design.theme.YapTheme
import org.koin.compose.koinInject
import org.koin.compose.navigation3.koinEntryProvider

@Composable
fun App() {
    YapTheme {
        val rootBackStack = koinInject<RootBackStack>()
        val keys by rootBackStack.keys.collectAsStateWithLifecycle(initialValue = emptyList())

        val launchRenewal = koinInject<LaunchRenewal>()
        LaunchedEffect(launchRenewal) { launchRenewal.run() }

        val saveableState = rememberSaveableStateHolderNavEntryDecorator<NavKey>()
        val viewModelStore = rememberViewModelStoreNavEntryDecorator<NavKey>()
        val resultEventBus = rememberResultEventBusNavEntryDecorator<NavKey>()
        val entryDecorators = remember(saveableState, viewModelStore, resultEventBus) {
            listOf(saveableState, viewModelStore, resultEventBus)
        }
        val sceneStrategies = remember {
            listOf(BottomSheetSceneStrategy<NavKey>(), SinglePaneSceneStrategy<NavKey>())
        }

        if (keys.isNotEmpty()) {
            NavDisplay(
                backStack = keys,
                onBack = rootBackStack::back,
                entryDecorators = entryDecorators,
                sceneStrategies = sceneStrategies,
                entryProvider = koinEntryProvider<NavKey>(),
            )
        }
    }
}
