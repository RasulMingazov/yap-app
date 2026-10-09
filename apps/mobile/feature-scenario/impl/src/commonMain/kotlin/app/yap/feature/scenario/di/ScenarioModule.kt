package app.yap.feature.scenario.di

import app.yap.core.design.navigation.bottomSheetScene
import app.yap.feature.scenario.api.HomeNavKey
import app.yap.feature.scenario.api.ScenariosNavKey
import app.yap.feature.scenario.api.usecase.ObserveScenarioOverviewUseCase
import app.yap.feature.scenario.data.CurrentDate
import app.yap.feature.scenario.data.SystemCurrentDate
import app.yap.feature.scenario.data.local.OverviewSnapshotStore
import app.yap.feature.scenario.data.local.createOverviewSnapshotStore
import app.yap.feature.scenario.data.remote.DefaultScenarioRemoteDataSource
import app.yap.feature.scenario.data.remote.ScenarioRemoteDataSource
import app.yap.feature.scenario.data.repository.DefaultScenarioRepository
import app.yap.feature.scenario.domain.gateway.AccessGateway
import app.yap.feature.scenario.domain.gateway.PaywallGateway
import app.yap.feature.scenario.domain.gateway.SessionGateway
import app.yap.feature.scenario.domain.repository.ScenarioRepository
import app.yap.feature.scenario.domain.usecase.DefaultObserveScenarioOverviewUseCase
import app.yap.feature.scenario.domain.usecase.DefaultOpenPaywallUseCase
import app.yap.feature.scenario.domain.usecase.DefaultOpenScenarioUseCase
import app.yap.feature.scenario.domain.usecase.DefaultRefreshOverviewUseCase
import app.yap.feature.scenario.domain.usecase.OpenPaywallUseCase
import app.yap.feature.scenario.domain.usecase.OpenScenarioUseCase
import app.yap.feature.scenario.domain.usecase.DefaultRepeatScenarioUseCase
import app.yap.feature.scenario.domain.usecase.RefreshOverviewUseCase
import app.yap.feature.scenario.domain.usecase.RepeatScenarioUseCase
import app.yap.feature.scenario.presentation.home.HomeUiStateMapper
import app.yap.feature.scenario.presentation.home.HomeViewModel
import app.yap.feature.scenario.presentation.common.RepeatConfirmationNavKey
import app.yap.feature.scenario.presentation.common.ui.RepeatConfirmationSheet
import app.yap.feature.scenario.presentation.scenarios.ScenariosUiStateMapper
import app.yap.feature.scenario.presentation.scenarios.ScenariosViewModel
import app.yap.feature.scenario.placeholder.ComingSoonSessionGateway
import app.yap.feature.scenario.placeholder.FreeAccessGateway
import app.yap.feature.scenario.placeholder.PlaceholderPaywall
import app.yap.feature.scenario.placeholder.PlaceholderPaywallNavKey
import app.yap.feature.scenario.placeholder.PlaceholderPaywallSheet
import app.yap.feature.scenario.placeholder.SessionComingSoonNavKey
import app.yap.feature.scenario.placeholder.SessionComingSoonSheet
import app.yap.feature.scenario.presentation.home.ui.HomeScreen
import app.yap.feature.scenario.presentation.scenarios.ui.ScenariosScreen
import androidx.compose.material3.ExperimentalMaterial3Api
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import org.koin.dsl.onClose
import org.koin.dsl.navigation3.navigation

@OptIn(ExperimentalMaterial3Api::class)
fun featureScenarioModule(): Module = module {
    single<OverviewSnapshotStore> { createOverviewSnapshotStore() }

    single<ScenarioRemoteDataSource> { DefaultScenarioRemoteDataSource(apiClient = get()) }

    single<CurrentDate> { SystemCurrentDate() }

    single { CoroutineScope(SupervisorJob() + Dispatchers.Default) } onClose { scope -> (scope as? CoroutineScope)?.cancel() }

    single<ScenarioRepository> {
        DefaultScenarioRepository(
            currentDate = get(),
            observeAuthSessionStateUseCase = get(),
            remoteDataSource = get(),
            scope = get(),
            snapshotStore = get(),
        )
    }

    single<SessionGateway> { ComingSoonSessionGateway(navigator = get()) }

    single<AccessGateway> { FreeAccessGateway() }

    single<PaywallGateway> { PlaceholderPaywall(navigator = get()) }

    bindUseCases()

    factory { HomeUiStateMapper() }

    viewModel {
        HomeViewModel(
            analyticsTracker = get(),
            navigator = get(),
            observeScenarioOverviewUseCase = get(),
            openPaywallUseCase = get(),
            openScenarioUseCase = get(),
            refreshOverviewUseCase = get(),
            uiStateMapper = get(),
        )
    }

    factory { ScenariosUiStateMapper() }

    viewModel {
        ScenariosViewModel(
            analyticsTracker = get(),
            navigator = get(),
            observeScenarioOverviewUseCase = get(),
            openScenarioUseCase = get(),
            refreshOverviewUseCase = get(),
            repeatScenarioUseCase = get(),
            uiStateMapper = get(),
        )
    }

    navigation<HomeNavKey> { HomeScreen() }

    navigation<ScenariosNavKey> { ScenariosScreen() }

    navigation<RepeatConfirmationNavKey>(metadata = bottomSheetScene()) { key ->
        RepeatConfirmationSheet(key = key)
    }

    navigation<SessionComingSoonNavKey>(metadata = bottomSheetScene()) { SessionComingSoonSheet() }

    navigation<PlaceholderPaywallNavKey>(metadata = bottomSheetScene()) { PlaceholderPaywallSheet() }
}

private fun Module.bindUseCases() {
    factory<ObserveScenarioOverviewUseCase> { DefaultObserveScenarioOverviewUseCase(scenarioRepository = get()) }

    factory<RefreshOverviewUseCase> { DefaultRefreshOverviewUseCase(scenarioRepository = get()) }

    factory<OpenScenarioUseCase> {
        DefaultOpenScenarioUseCase(
            analyticsTracker = get(),
            paywallGateway = get(),
            scenarioRepository = get(),
            sessionGateway = get(),
        )
    }

    factory<OpenPaywallUseCase> { DefaultOpenPaywallUseCase(paywallGateway = get()) }

    factory<RepeatScenarioUseCase> {
        DefaultRepeatScenarioUseCase(
            analyticsTracker = get(),
            paywallGateway = get(),
            scenarioRepository = get(),
            sessionGateway = get(),
        )
    }
}
