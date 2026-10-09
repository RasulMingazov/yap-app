import GoogleSignIn
import SwiftUI
import YapShared

/// Configuration the entry point owns, mirroring `AppConfiguration` on Android.
///
/// The values come from `Config.xcconfig` (copy `Config.example.xcconfig`) through `Info.plist`.
/// A missing one stops the app here, at launch, with the key to set — not inside a sign-in attempt.
/// The web client ID is the `serverClientId` both platforms send; the iOS client ID is the one this
/// app authorizes with, and its reversed form is the URL scheme `Info.plist` registers. The two
/// legal destinations stay `nil` until the documents exist — the line renders either way, and the
/// app is not released to users while either is unset.
private enum AppConfiguration {
    static let baseUrl = required("YapApiBaseUrl")
    static let googleClientId = required("YapGoogleClientId")
    static let googleServerClientId = required("YapGoogleServerClientId")
    static let termsUrl: String? = nil
    static let privacyUrl: String? = nil

    private static func required(_ key: String) -> String {
        guard let value = Bundle.main.object(forInfoDictionaryKey: key) as? String, !value.isEmpty else {
            fatalError("\(key) is not set: copy Config.example.xcconfig to Config.xcconfig and fill it in")
        }
        return value
    }
}

@main
struct YapApp: App {

    init() {
        // Kotlin exports functions whose names start with `init` with a `do` prefix.
        InitIosKoinKt.doInitIosKoin(
            baseUrl: AppConfiguration.baseUrl,
            googleSignInBridge: GoogleSignInBridge(
                clientId: AppConfiguration.googleClientId,
                serverClientId: AppConfiguration.googleServerClientId
            ),
            googleServerClientId: AppConfiguration.googleServerClientId,
            privacyUrl: AppConfiguration.privacyUrl,
            termsUrl: AppConfiguration.termsUrl
        )
    }

    var body: some Scene {
        WindowGroup {
            // The same colour the launch storyboard paints, held beneath the Compose root. The
            // shared root draws nothing until auth state resolves, so this is what the user keeps
            // seeing in that gap — the launch screen appears to stay up rather than giving way to a
            // blank screen and then the main one.
            ZStack {
                Color("SplashBackground")
                    .ignoresSafeArea(.all)

                ComposeRootView()
                    .ignoresSafeArea(.all)
            }
            .onOpenURL { url in
                GIDSignIn.sharedInstance.handle(url)
            }
        }
    }
}

/// Hosts the shared Compose view controller.
struct ComposeRootView: UIViewControllerRepresentable {

    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.mainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
