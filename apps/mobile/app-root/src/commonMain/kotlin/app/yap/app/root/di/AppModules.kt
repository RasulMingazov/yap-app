package app.yap.app.root.di

import app.yap.core.network.coreNetworkModule
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration

fun appModules(baseUrl: String): List<Module> = listOf(
    coreNetworkModule(baseUrl),
)

fun initKoin(
    baseUrl: String,
    appDeclaration: KoinAppDeclaration = {},
): KoinApplication = startKoin {
    appDeclaration()
    modules(appModules(baseUrl))
}
