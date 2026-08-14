package app.yap.app.root.di

import app.yap.feature.auth.api.usecase.ObserveAuthStateUseCase
import app.yap.feature.auth.api.usecase.RenewSessionUseCase
import kotlin.test.Test
import org.koin.test.verify.verify

internal class AppRootModuleTest {

    @Test
    fun `GIVEN the app root module WHEN it is verified THEN every binding resolves`() {
        appRootModule().verify(
            extraTypes = listOf(
                ObserveAuthStateUseCase::class,
                RenewSessionUseCase::class,
            ),
        )
    }
}
