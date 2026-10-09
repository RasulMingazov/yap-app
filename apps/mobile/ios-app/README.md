# iOS host

The Xcode host application. Everything shared with Android is Kotlin, delivered as
`YapShared.framework` by `:apps:mobile:shared-app`; what lives here is only what has
to be Swift — the entry point, its configuration, the GoogleSignIn bridge, and the launch screen.

This project is outside Gradle, so `./gradlew build` cannot verify it. The Gradle
side is checked by `:apps:mobile:shared-app:compileKotlinIosSimulatorArm64`; this
side is checked by building and running it in Xcode.

## Building

1. Build the shared framework first, so `YapShared.framework` exists:

   ```shell
   ./gradlew :apps:mobile:shared-app:linkDebugFrameworkIosSimulatorArm64
   ```

2. Copy `Config.example.xcconfig` to `Config.xcconfig` (ignored by Git) and fill it in.

3. Open `YapApp.xcodeproj` and run.

## Google login

GoogleSignIn 9.1.0 is pinned through Swift Package Manager. `GoogleSignInBridge.swift` is its only
boundary with shared Kotlin: it passes the attempt nonce into the SDK and returns an ID token, or
`nil` when the user dismisses the flow. `feature-auth/impl` keeps its credential contract internal
and maps that narrow result into the same repository path Android uses.

- `Info.plist` registers `YAP_GOOGLE_IOS_REVERSED_CLIENT_ID` from `Config.xcconfig` as the URL
  scheme the SDK returns to.
- `YapApp.swift` forwards the returned URL to `GIDSignIn` through SwiftUI's `onOpenURL`.
- The SDK owns browser presentation, PKCE, token exchange, saved account state, and optional App
  Check integration. Kotlin sees none of those SDK types.

## Configuration

`Config.xcconfig` holds the base URL and the iOS and web client IDs; `Config.example.xcconfig`
lists the keys, and the project reads the real file as the base configuration of the target, so
`Info.plist` can forward the values to `YapApp.swift`. A missing value stops the app at launch
with the key to set. A simulator reaches a server running on this machine at `http://localhost:8080`
(written `http:/$()/localhost:8080`, since `//` starts a comment in this format). The two legal
destinations stay `nil` in `YapApp.swift` until the documents exist — the line renders either way,
and the app is not released to users while either is unset.

## Launch screen

`LaunchScreen.storyboard` paints `Assets.xcassets/SplashBackground.colorset`, and
`YapApp.swift` holds the same colour beneath the Compose root. The shared root draws
nothing until auth state resolves, so that colour is what a returning user keeps
seeing until the main screen arrives — the login screen never flashes past.
