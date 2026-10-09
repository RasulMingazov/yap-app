package app.yap.feature.scenario.domain.gateway

import kotlinx.coroutines.flow.Flow

internal interface AccessGateway {

    fun observe(): Flow<Access>
}

internal enum class Access { Free, Subscribed }
