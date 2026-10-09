package app.yap.feature.scenario.placeholder

import app.yap.feature.scenario.domain.gateway.Access
import app.yap.feature.scenario.domain.gateway.AccessGateway
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

internal class FreeAccessGateway : AccessGateway {

    private val access = MutableStateFlow(Access.Free)

    override fun observe(): Flow<Access> = access
}
