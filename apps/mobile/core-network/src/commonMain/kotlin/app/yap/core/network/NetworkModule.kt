package app.yap.core.network

import app.yap.core.common.network.AccessTokenProvider
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.dsl.onClose

fun coreNetworkModule(baseUrl: String): Module = module {
    single {
        createNetworkClient(
            baseUrl = baseUrl,
            engine = platformHttpClientEngine(),
        ).apply {
            val accessTokenProvider = getOrNull<AccessTokenProvider>()
            if (accessTokenProvider != null) {
                installAccessTokenModifier(accessTokenProvider::getAccessToken)
            }
        }
    } onClose { networkClient -> networkClient?.close() }

    single { ApiClient(networkClient = get()) }
}
