package app.yap.feature.auth.api.usecase

fun interface RefreshSessionUseCase {

    suspend operator fun invoke()
}
