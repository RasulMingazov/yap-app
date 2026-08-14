# iOS host

The Xcode host application. Everything shared with Android is Kotlin, delivered as
`YapShared.framework` by `:apps:mobile:shared-app`; what lives here is only what has
to be Swift.

This project is outside Gradle, so `./gradlew build` cannot verify it. The Gradle
side is checked by `:apps:mobile:shared-app:compileKotlinIosSimulatorArm64`; this
side is checked by building and running it in Xcode.

## Building

1. Build the shared framework first, so `YapShared.framework` exists:

   ```shell
   ./gradlew :apps:mobile:shared-app:linkDebugFrameworkIosSimulatorArm64
   ```

2. Open `YapApp.xcodeproj` and run.

## Google login

Google's iOS SDK stays entirely in Xcode rather than in the Gradle build, so the
Kotlin side never depends on an iOS SDK and sees one narrow suspend function.

- Add **GoogleSignIn** through Swift Package Manager
  (`https://github.com/google/GoogleSignIn-iOS`).
- Register the reversed iOS client ID as a URL scheme in `Info.plist`, replacing the
  `com.googleusercontent.apps.REPLACE_WITH_IOS_CLIENT_ID` placeholder.
- Forward `application(_:open:options:)` to `GIDSignIn.sharedInstance.handle(_:)` —
  `YapApp.swift` does this through SwiftUI's `onOpenURL`.
- `GoogleCredentialProviderImpl.swift` implements the Kotlin
  `GoogleCredentialProvider` contract and is handed to `initIosKoin`, which is what
  makes it the credential path the shared code uses. It returns an ID token only:
  the authorization-code fallback is Android's, because Credential Manager needs
  Google's services while `ASWebAuthenticationSession` does not.

## Configuration

`YapApp.swift` owns the base URL, the web client ID, and the two legal destinations,
mirroring `MainActivity` on Android. A simulator reaches a server running on this
machine at `http://localhost:8080`. Both legal destinations stay `nil` until the
documents exist — the line renders either way, and the app is not released to users
while either is unset.

## Launch screen

`LaunchScreen.storyboard` paints `Assets.xcassets/SplashBackground.colorset`, and
`YapApp.swift` holds the same colour beneath the Compose root. The shared root draws
nothing until auth state resolves, so that colour is what a returning user keeps
seeing until the main screen arrives — the login screen never flashes past.
