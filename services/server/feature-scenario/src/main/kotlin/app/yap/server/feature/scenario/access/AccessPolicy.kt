package app.yap.server.feature.scenario.access

import java.util.UUID

fun interface AccessPolicy {

    fun hasAccess(userId: UUID): Boolean
}

class FreeOnlyAccessPolicy : AccessPolicy {

    override fun hasAccess(userId: UUID): Boolean = false
}
