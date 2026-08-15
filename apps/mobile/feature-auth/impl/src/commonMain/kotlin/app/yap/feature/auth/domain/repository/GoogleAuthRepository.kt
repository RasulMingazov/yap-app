package app.yap.feature.auth.domain.repository

import app.yap.feature.auth.api.entity.LoginOutcome

internal interface GoogleAuthRepository {

    suspend fun login(): LoginOutcome
}
