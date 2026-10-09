package app.yap.app.root.di

import app.yap.app.root.LaunchSessionRefresh
import app.yap.app.root.navigation.MainScaffold
import app.yap.app.root.navigation.RootBackStack
import app.yap.app.root.navigation.RootNavKey
import app.yap.core.common.navigation.Navigator
import app.yap.core.common.navigation.TabReselects
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.dsl.navigation3.navigation

internal fun appRootModule(): Module = module {
    single { RootBackStack(observeAuthSessionStateUseCase = get()) }

    single<Navigator> { get<RootBackStack>() }

    single<TabReselects> { get<RootBackStack>() }

    factory {
        LaunchSessionRefresh(
            observeAuthSessionStateUseCase = get(),
            refreshSessionUseCase = get(),
        )
    }

    navigation<RootNavKey.Main> { MainScaffold() }
}
