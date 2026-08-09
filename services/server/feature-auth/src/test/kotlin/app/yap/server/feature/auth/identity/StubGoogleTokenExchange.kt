package app.yap.server.feature.auth.identity

import io.github.rasulmingazov.stubcall.StubCall3

internal class StubGoogleTokenExchange(
    idToken: String = StubGoogleIdToken.stubIdToken(),
) : GoogleTokenExchange {

    val exchangeCall = StubCall3.returns<String, String, String, String>(idToken)

    override suspend fun exchange(
        code: String,
        codeVerifier: String,
        redirectUri: String,
    ): String = exchangeCall.invoke(code, codeVerifier, redirectUri)
}
