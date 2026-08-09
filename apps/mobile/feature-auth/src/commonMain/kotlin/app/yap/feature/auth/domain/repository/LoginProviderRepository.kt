package app.yap.feature.auth.domain.repository

import app.yap.feature.auth.domain.entity.LoginProvider
import kotlinx.coroutines.flow.Flow

internal interface LoginProviderRepository {

    fun observeAll(): Flow<List<LoginProvider>>
}
