package app.yap.feature.auth.di

import app.yap.core.common.network.AccessTokenProvider
import app.yap.core.network.NetworkClient
import app.yap.feature.auth.api.AuthNavKey
import app.yap.feature.auth.api.entity.AuthProvider
import app.yap.feature.auth.api.usecase.ObserveAuthStateUseCase
import app.yap.feature.auth.api.usecase.LoginUseCase
import app.yap.feature.auth.api.usecase.RenewSessionUseCase
import app.yap.feature.auth.data.AuthStateSource
import app.yap.feature.auth.data.CurrentTime
import app.yap.feature.auth.data.DefaultAccessTokenProvider
import app.yap.feature.auth.data.SystemCurrentTime
import app.yap.feature.auth.data.identity.NonceGenerator
import app.yap.feature.auth.data.identity.RandomNonceGenerator
import app.yap.feature.auth.data.local.SessionStorage
import app.yap.feature.auth.data.local.createSessionStorage
import app.yap.feature.auth.data.remote.AuthRemoteDataSource
import app.yap.feature.auth.data.remote.DefaultAuthRemoteDataSource
import app.yap.feature.auth.data.repository.DefaultAuthRepository
import app.yap.feature.auth.domain.repository.AuthRepository
import app.yap.feature.auth.domain.usecase.DefaultObserveAuthStateUseCase
import app.yap.feature.auth.domain.usecase.DefaultRenewSessionUseCase
import app.yap.feature.auth.domain.usecase.GoogleLoginUseCase
import app.yap.feature.auth.presentation.login.LoginViewModel
import app.yap.feature.auth.presentation.login.ui.LoginScreen
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import org.koin.dsl.navigation3.navigation

fun featureAuthModule(
    googleServerClientId: String,
    privacyUrl: String?,
    termsUrl: String?,
    googleAndroidClientId: String = "",
    googleRedirectUri: String = "",
): Module = module {
    single<SessionStorage> { createSessionStorage() }

    single<AuthRemoteDataSource> {
        val networkClient = get<NetworkClient>()
        DefaultAuthRemoteDataSource(
            baseUrl = networkClient.baseUrl,
            httpClient = networkClient.httpClient,
        )
    }

    factory<NonceGenerator> { RandomNonceGenerator() }

    bindGoogleCredentialProvider(
        googleAndroidClientId = googleAndroidClientId,
        googleRedirectUri = googleRedirectUri,
        googleServerClientId = googleServerClientId,
    )

    single<CurrentTime> { SystemCurrentTime() }

    single { AuthStateSource() }

    single<AccessTokenProvider> {
        DefaultAccessTokenProvider(
            authRemoteDataSource = lazy { get<AuthRemoteDataSource>() },
            authStateSource = get(),
            sessionStorage = get(),
        )
    }

    single<AuthRepository> {
        DefaultAuthRepository(
            accessTokenProvider = get(),
            authRemoteDataSource = get(),
            authStateSource = get(),
            currentTime = get(),
            googleCredentialProvider = get(),
            nonceGenerator = get(),
            sessionStorage = get(),
        )
    }

    factory<ObserveAuthStateUseCase> { DefaultObserveAuthStateUseCase(authRepository = get()) }

    factory<RenewSessionUseCase> { DefaultRenewSessionUseCase(authRepository = get()) }

    factory<Map<AuthProvider, LoginUseCase>> {
        mapOf(AuthProvider.GOOGLE to GoogleLoginUseCase(authRepository = get()))
    }

    viewModel {
        LoginViewModel(
            loginUseCases = get(),
            motionPreferences = get(),
            platform = get(),
            privacyUrl = privacyUrl,
            termsUrl = termsUrl,
        )
    }

    navigation<AuthNavKey.Login> { LoginScreen() }
}

internal expect fun Module.bindGoogleCredentialProvider(
    googleAndroidClientId: String,
    googleRedirectUri: String,
    googleServerClientId: String,
)
