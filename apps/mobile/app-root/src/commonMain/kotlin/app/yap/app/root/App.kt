package app.yap.app.root

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.ui.NavDisplay
import app.yap.app.root.navigation.RootBackStack
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

        if (keys.isNotEmpty()) {
            NavDisplay(
                backStack = keys,
                entryProvider = koinEntryProvider<NavKey>(),
            )
        }
    }
}
