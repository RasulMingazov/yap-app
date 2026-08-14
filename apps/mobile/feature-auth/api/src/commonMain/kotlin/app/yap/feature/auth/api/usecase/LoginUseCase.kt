package app.yap.feature.auth.api.usecase

import app.yap.feature.auth.api.entity.LoginOutcome

fun interface LoginUseCase {

    suspend operator fun invoke(): LoginOutcome
}
