import GoogleSignIn
import UIKit
import YapShared

/// The only GoogleSignIn surface shared Kotlin needs from its iOS host.
///
/// Kotlin owns the feature contract and maps `nil` to its ordinary cancelled outcome. The SDK owns
/// browser presentation, PKCE, token exchange, account state, and optional App Check support.
final class GoogleSignInBridge: NSObject, IosGoogleSignInBridge {

    init(clientId: String, serverClientId: String) {
        super.init()
        GIDSignIn.sharedInstance.configuration = GIDConfiguration(
            clientID: clientId,
            serverClientID: serverClientId
        )
    }

    func requestIdToken(nonce: String) async throws -> String? {
        do {
            return try await signIn(nonce: nonce)
        } catch let error as NSError
            where error.domain == kGIDSignInErrorDomain &&
                error.code == GIDSignInError.canceled.rawValue {
            return nil
        }
    }

    @MainActor
    private func signIn(nonce: String) async throws -> String {
        guard let presenting = Self.topViewController() else {
            throw NSError(domain: "app.yap.auth", code: -1, userInfo: [
                NSLocalizedDescriptionKey: "No view controller to present Google Sign-In"
            ])
        }

        let result = try await GIDSignIn.sharedInstance.signIn(
            withPresenting: presenting,
            hint: nil,
            additionalScopes: nil,
            nonce: nonce
        )
        guard let idToken = result.user.idToken?.tokenString, !idToken.isEmpty else {
            throw NSError(domain: "app.yap.auth", code: -2, userInfo: [
                NSLocalizedDescriptionKey: "Google Sign-In returned no ID token"
            ])
        }
        return idToken
    }

    @MainActor
    private static func topViewController() -> UIViewController? {
        let scenes = UIApplication.shared.connectedScenes.compactMap { $0 as? UIWindowScene }
        var controller = scenes
            .flatMap(\.windows)
            .first(where: \.isKeyWindow)?
            .rootViewController
        while let presented = controller?.presentedViewController {
            controller = presented
        }
        return controller
    }
}
