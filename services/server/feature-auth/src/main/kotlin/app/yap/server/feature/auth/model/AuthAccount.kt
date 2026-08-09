package app.yap.server.feature.auth.model

import java.time.Instant

/**
 * A Yap account. [id] is the stable ownership key for learning progress: the same identity from the
 * same provider always resolves the same value. The account itself stores product-owned fields
 * only; everything the provider owns lives in [ProviderIdentity].
 */
internal data class AuthAccount(
    val createdAt: Instant,
    val id: String,
)
