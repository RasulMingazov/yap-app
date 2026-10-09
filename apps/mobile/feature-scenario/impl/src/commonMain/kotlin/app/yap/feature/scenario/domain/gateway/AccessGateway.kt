package app.yap.feature.scenario.domain.gateway

import kotlinx.coroutines.flow.Flow

/** `feature-subscription` adapts this later; until then `FreeAccessGateway` (research R6). */
internal interface AccessGateway {

    fun observe(): Flow<Access>
}

internal enum class Access { Free, Subscribed }
