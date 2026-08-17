package app.yap.shared.app

interface IosGoogleSignInBridge {

    /** Returns `null` only when the user dismisses Google Sign-In. */
    suspend fun requestIdToken(nonce: String): String?
}
