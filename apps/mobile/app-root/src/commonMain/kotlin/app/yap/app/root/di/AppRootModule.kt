package app.yap.app.root.di

import app.yap.app.root.LaunchRenewal
import app.yap.app.root.navigation.MainPlaceholderScreen
import app.yap.app.root.navigation.RootBackStack
import app.yap.app.root.navigation.RootNavKey
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.dsl.navigation3.navigation

internal fun appRootModule(): Module = module {
    factory { RootBackStack(observeAuthStateUseCase = get()) }

    factory {
        LaunchRenewal(
            observeAuthStateUseCase = get(),
            renewSessionUseCase = get(),
        )
    }

    navigation<RootNavKey.Main> { MainPlaceholderScreen() }
}
