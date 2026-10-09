package app.yap.server.feature.scenario.access

import java.util.UUID

fun interface AccessPolicy {

    fun hasAccess(userId: UUID): Boolean
}

/** Answers false for everyone until `feature-subscription` provides the real policy (research R6). */
class FreeOnlyAccessPolicy : AccessPolicy {

    override fun hasAccess(userId: UUID): Boolean = false
}
