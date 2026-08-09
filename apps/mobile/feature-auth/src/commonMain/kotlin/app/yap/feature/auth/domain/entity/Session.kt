package app.yap.feature.auth.domain.entity

/**
 * The signed-in identity as the product understands it. Session credentials stay in the data
 * layer and never reach domain, presentation, logs, or analytics (R-051).
 */
internal data class Session(
    val accountId: AccountId,
)
