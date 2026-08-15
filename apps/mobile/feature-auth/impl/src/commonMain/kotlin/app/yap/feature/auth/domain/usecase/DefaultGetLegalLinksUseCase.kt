package app.yap.feature.auth.domain.usecase

import app.yap.feature.auth.api.entity.LegalLinks
import app.yap.feature.auth.api.usecase.GetLegalLinksUseCase

internal class DefaultGetLegalLinksUseCase(
    private val legalLinks: LegalLinks,
) : GetLegalLinksUseCase {

    override suspend fun invoke(): LegalLinks = legalLinks
}
