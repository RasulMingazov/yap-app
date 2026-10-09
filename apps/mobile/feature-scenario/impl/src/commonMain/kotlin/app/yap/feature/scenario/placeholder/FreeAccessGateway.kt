package app.yap.feature.scenario.placeholder

import app.yap.feature.scenario.domain.gateway.Access
import app.yap.feature.scenario.domain.gateway.AccessGateway
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** Everyone is a free user until `feature-subscription` provides the real status (research R6). */
internal class FreeAccessGateway : AccessGateway {

    private val access = MutableStateFlow(Access.Free)

    override fun observe(): Flow<Access> = access
}
