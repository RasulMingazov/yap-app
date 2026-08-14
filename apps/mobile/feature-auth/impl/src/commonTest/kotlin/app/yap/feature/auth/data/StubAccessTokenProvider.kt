package app.yap.feature.auth.data

import app.yap.core.common.network.AccessTokenProvider
import app.yap.feature.auth.data.local.StubSession
import io.github.rasulmingazov.stubcall.StubCall1

internal class StubAccessTokenProvider(
    accessToken: String? = StubSession.ACCESS_TOKEN,
) : AccessTokenProvider {

    val getAccessTokenCall = StubCall1.returns<String?, String?>(accessToken)

    override suspend fun getAccessToken(rejectedAccessToken: String?): String? =
        getAccessTokenCall.invoke(rejectedAccessToken)
}
