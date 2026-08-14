package app.yap.shared.app

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeUIViewController
import app.yap.app.root.App
import platform.UIKit.UIViewController

@OptIn(ExperimentalComposeUiApi::class)
fun mainViewController(): UIViewController = ComposeUIViewController(
    configure = { opaque = false },
) { App() }
