package app.yap.feature.auth.api.usecase

import app.yap.feature.auth.api.entity.LegalLinks

fun interface GetLegalLinksUseCase {

    suspend operator fun invoke(): LegalLinks
}
