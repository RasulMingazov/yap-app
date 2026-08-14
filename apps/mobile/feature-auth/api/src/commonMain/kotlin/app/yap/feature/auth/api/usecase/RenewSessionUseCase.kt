package app.yap.feature.auth.api.usecase

fun interface RenewSessionUseCase {

    suspend operator fun invoke()
}
