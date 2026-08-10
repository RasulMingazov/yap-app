package app.yap.feature.auth.di

import app.yap.feature.auth.domain.entity.LoginProvider

/**
 * Injected login-provider configuration. [providers] is used verbatim: membership, order,
 * visibility, enabled state, and display names all come from here, and no layer above re-sorts or
 * branches on a concrete provider (R-011, R-012, R-015, AC-040).
 */
internal data class LoginProviderConfig(
    val providers: List<LoginProvider>,
)

/**
 * Platform default configuration. Every platform enables Google only and keeps Apple and T-ID
 * visible-but-disabled where they are supported at all (R-016, R-017).
 */
internal expect fun defaultLoginProviderConfig(): LoginProviderConfig
