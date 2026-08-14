import GoogleSignIn
import UIKit
import YapShared

/// The iOS half of the Google credential port.
///
/// The GoogleSignIn SDK already presents `ASWebAuthenticationSession`, which is the system browser
/// under Apple's rules and needs none of Google's services — so iOS has no equivalent of Android's
/// browser fallback and always returns an ID token.
final class GoogleCredentialProviderImpl: NSObject, GoogleCredentialProvider {

    /// Raised when the user dismisses Google's confirmation. Kotlin maps it to a silent outcome.
    static let cancelledErrorDomain = "app.yap.auth.cancelled"

    func requestCredential(nonce: String) async throws -> GoogleCredential {
        guard let presenting = await Self.topViewController() else {
            throw NSError(domain: "app.yap.auth", code: -1, userInfo: [
                NSLocalizedDescriptionKey: "No view controller to present the confirmation"
            ])
        }

        do {
            let result = try await GIDSignIn.sharedInstance.signIn(
                withPresenting: presenting,
                hint: nil,
                additionalScopes: nil,
                nonce: nonce
            )
            guard let idToken = result.user.idToken?.tokenString else {
                throw NSError(domain: "app.yap.auth", code: -2, userInfo: [
                    NSLocalizedDescriptionKey: "Google returned no ID token"
                ])
            }
            return GoogleCredentialIdToken(value: idToken)
        } catch let error as NSError where error.code == GIDSignInError.canceled.rawValue {
            throw NSError(domain: Self.cancelledErrorDomain, code: error.code, userInfo: nil)
        }
    }

    @MainActor
    private static func topViewController() -> UIViewController? {
        let scene = UIApplication.shared.connectedScenes.first as? UIWindowScene
        var controller = scene?.windows.first(where: \.isKeyWindow)?.rootViewController
        while let presented = controller?.presentedViewController {
            controller = presented
        }
        return controller
    }
}
