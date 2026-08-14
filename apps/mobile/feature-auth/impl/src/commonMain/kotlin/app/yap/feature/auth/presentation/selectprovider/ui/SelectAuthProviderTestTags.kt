package app.yap.feature.auth.presentation.selectprovider.ui

import app.yap.feature.auth.api.entity.AuthProvider
import app.yap.feature.auth.presentation.AuthProviderResources

internal object SelectAuthProviderTestTags {

    const val SECTION_LABEL = "login_provider_section_label"

    fun rowOf(provider: AuthProvider): String = AuthProviderResources.testTagOf(provider)
}
