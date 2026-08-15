package app.yap.feature.auth.di

import app.yap.core.common.network.AccessTokenProvider
import androidx.compose.material3.ExperimentalMaterial3Api
import app.yap.core.design.navigation.bottomSheetScene
import app.yap.feature.auth.api.AuthNavKey
import app.yap.feature.auth.api.entity.LegalLinks
import app.yap.feature.auth.api.usecase.GetLegalLinksUseCase
import app.yap.feature.auth.api.usecase.ObserveAuthProvidersUseCase
import app.yap.feature.auth.api.usecase.ObserveAuthSessionStateUseCase
import app.yap.feature.auth.api.usecase.LoginUseCase
import app.yap.feature.auth.api.usecase.RefreshSessionUseCase
import app.yap.feature.auth.data.CurrentTime
import app.yap.feature.auth.data.DefaultAccessTokenProvider
import app.yap.feature.auth.data.SessionStore
import app.yap.feature.auth.data.SystemCurrentTime
import app.yap.feature.auth.data.identity.NonceGenerator
import app.yap.feature.auth.data.identity.RandomNonceGenerator
import app.yap.feature.auth.data.local.SessionStorage
import app.yap.feature.auth.data.local.createSessionStorage
import app.yap.feature.auth.data.remote.AuthRemoteDataSource
import app.yap.feature.auth.data.remote.DefaultAuthRemoteDataSource
import app.yap.feature.auth.data.repository.DefaultAuthSessionRepository
import app.yap.feature.auth.data.repository.DefaultGoogleAuthRepository
import app.yap.feature.auth.domain.provider.GoogleProviderLogin
import app.yap.feature.auth.domain.provider.ProviderLogin
import app.yap.feature.auth.domain.repository.AuthSessionRepository
import app.yap.feature.auth.domain.repository.GoogleAuthRepository
import app.yap.feature.auth.domain.usecase.DefaultGetLegalLinksUseCase
import app.yap.feature.auth.domain.usecase.DefaultLoginUseCase
import app.yap.feature.auth.domain.usecase.DefaultObserveAuthProvidersUseCase
import app.yap.feature.auth.domain.usecase.DefaultObserveAuthSessionStateUseCase
import app.yap.feature.auth.domain.usecase.DefaultRefreshSessionUseCase
import app.yap.feature.auth.presentation.common.AuthProviderUiMapper
import app.yap.feature.auth.presentation.login.LoginNewsMapper
import app.yap.feature.auth.presentation.login.LoginUiStateMapper
import app.yap.feature.auth.presentation.login.LoginViewModel
import app.yap.feature.auth.presentation.login.ui.LoginScreen
import app.yap.feature.auth.presentation.selectprovider.SelectAuthProviderUiStateMapper
import app.yap.feature.auth.presentation.selectprovider.SelectAuthProviderViewModel
import app.yap.feature.auth.presentation.selectprovider.ui.SelectAuthProviderScreen
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.dsl.navigation3.navigation

@OptIn(ExperimentalMaterial3Api::class)
fun featureAuthModule(
    googleServerClientId: String,
    privacyUrl: String?,
    termsUrl: String?,
    googleAndroidClientId: String = "",
    googleRedirectUri: String = "",
): Module = module {
    single<SessionStorage> { createSessionStorage() }

    single<AuthRemoteDataSource> { DefaultAuthRemoteDataSource(apiClient = get()) }

    factory<NonceGenerator> { RandomNonceGenerator() }

    bindGoogleCredentialProvider(
        googleAndroidClientId = googleAndroidClientId,
        googleRedirectUri = googleRedirectUri,
        googleServerClientId = googleServerClientId,
    )

    single<CurrentTime> { SystemCurrentTime() }

    bindSessionGraph()

    factory<ObserveAuthSessionStateUseCase> { DefaultObserveAuthSessionStateUseCase(authSessionRepository = get()) }

    factory<RefreshSessionUseCase> { DefaultRefreshSessionUseCase(authSessionRepository = get()) }

    single { GoogleProviderLogin(googleAuthRepository = get()) } bind ProviderLogin::class

    factory<LoginUseCase> { DefaultLoginUseCase(providerLogins = getAll<ProviderLogin>()) }

    factory<ObserveAuthProvidersUseCase> { DefaultObserveAuthProvidersUseCase(platform = get()) }

    factory<GetLegalLinksUseCase> {
        DefaultGetLegalLinksUseCase(LegalLinks(privacyUrl = privacyUrl, termsUrl = termsUrl))
    }

    bindPresentationMappers()

    viewModel {
        LoginViewModel(
            getLegalLinksUseCase = get(),
            loginUseCase = get(),
            motionPreferences = get(),
            navigator = get(),
            newsMapper = get(),
            uiStateMapper = get(),
        )
    }

    viewModel {
        SelectAuthProviderViewModel(
            navigator = get(),
            observeAuthProvidersUseCase = get(),
            uiStateMapper = get(),
        )
    }

    navigation<AuthNavKey.Login> { LoginScreen() }

    navigation<AuthNavKey.SelectAuthProvider>(metadata = bottomSheetScene()) { SelectAuthProviderScreen() }
}

private fun Module.bindSessionGraph() {
    single { SessionStore(currentTime = get(), sessionStorage = get()) }

    single<AccessTokenProvider> {
        DefaultAccessTokenProvider(
            authRemoteDataSource = lazy { get<AuthRemoteDataSource>() },
            sessionStore = get(),
        )
    }

    single<AuthSessionRepository> {
        DefaultAuthSessionRepository(
            accessTokenProvider = get(),
            currentTime = get(),
            sessionStore = get(),
        )
    }

    single<GoogleAuthRepository> {
        DefaultGoogleAuthRepository(
            authRemoteDataSource = get(),
            googleCredentialProvider = get(),
            nonceGenerator = get(),
            sessionStore = get(),
        )
    }
}

private fun Module.bindPresentationMappers() {
    factory { AuthProviderUiMapper() }

    factory { LoginNewsMapper(authProviderUiMapper = get()) }

    factory { LoginUiStateMapper() }

    factory { SelectAuthProviderUiStateMapper(authProviderUiMapper = get()) }
}

internal expect fun Module.bindGoogleCredentialProvider(
    googleAndroidClientId: String,
    googleRedirectUri: String,
    googleServerClientId: String,
)
