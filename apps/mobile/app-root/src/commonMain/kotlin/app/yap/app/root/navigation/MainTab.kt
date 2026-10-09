package app.yap.app.root.navigation

import androidx.navigation3.runtime.NavKey
import app.yap.feature.scenario.api.HomeNavKey
import app.yap.feature.scenario.api.ScenariosNavKey

internal enum class MainTab(val rootKey: NavKey) {
    Home(HomeNavKey),
    Scenarios(ScenariosNavKey),
    Profile(RootNavKey.Profile),
    ;

    companion object {

        fun ofKey(key: NavKey): MainTab? = entries.firstOrNull { tab -> tab.rootKey == key }
    }
}
