# Quickstart: Login Screen

How to configure, run, and verify the feature. Shapes and rationale live in
[plan.md](plan.md), [research.md](research.md), [data-model.md](data-model.md), and
[contracts/auth-api.md](contracts/auth-api.md).

## Prerequisites

- JDK 17, Android SDK with `compileSdk` 37, Xcode for the iOS host.
- Docker running — Testcontainers starts PostgreSQL 17 for the server integration suite.
- A local PostgreSQL 17 for running the server by hand.
- A Google Cloud project with OAuth client IDs for **web**, **Android** (package name plus the
  signing certificate SHA-1), and **iOS**. The web client ID is the `serverClientId` both mobile
  platforms send, and it is one of the audiences the server accepts. The Android and iOS client
  IDs are the audiences of tokens minted through the browser fallback, so all three must be
  configured on the server.
- For the fallback: the Android client's reversed client ID registered as a redirect URI in the
  Google console, matching the intent filter in `AndroidManifest.xml`.
- An Android emulator image **without** Google Play services, to exercise the fallback (SC-012).
- `git config core.hooksPath .githooks` once per clone.

## Configuration

Server — `.env` at the repository root (git-ignored) or real environment variables:

```shell
DATABASE_URL=jdbc:postgresql://localhost:5432/yap
DATABASE_USER=postgres
DATABASE_PASSWORD=postgres
JWT_SECRET=<43+ characters, generated securely>
GOOGLE_WEB_CLIENT_ID=<...>.apps.googleusercontent.com
GOOGLE_ANDROID_CLIENT_ID=<...>.apps.googleusercontent.com
GOOGLE_IOS_CLIENT_ID=<...>.apps.googleusercontent.com
REFRESH_TOKEN_TTL_SECONDS=7776000          # 90 days (FR-025); also the shipped default
AUTH_RATE_LIMIT_REQUESTS_PER_MINUTE=100    # per originating IP, all three auth endpoints (FR-027)
```

Every value above is read by `AppConfigLoader` in `core-config`, which is the only thing that reads
`.env` — including the three Google client IDs, which it hands to the feature's `GoogleAuthConfig`.
`AuthConfig` rejects a short or well-known `JWT_SECRET` at startup, so the process fails fast
rather than issuing forgeable tokens.

Mobile — the base URL, the web client ID, and the two legal document addresses are passed as
parameters into `appModules(...)`, never read from global state. The Android emulator reaches a
host-machine server at `http://10.0.2.2:8080`. The terms and privacy addresses are placeholders
until the documents exist; the line and its links are built regardless (FR-028).

## Run

```shell
# Server
./gradlew :services:server:app:run

# Android
./gradlew :apps:mobile:android-app:installDebug
```

iOS: open `apps/mobile/ios-app/YapApp.xcodeproj`, build `:apps:mobile:shared-app` first so
`YapShared.framework` exists, then run. The host must add the GoogleSignIn package, register the
reversed-client-ID URL scheme, forward `application(_:open:options:)` to `GIDSignIn`, and pass its
`GoogleCredentialProvider` implementation into `initIosKoin`.

## Verify

```shell
./gradlew build                                                  # required: all modules, tests, Detekt
./gradlew :apps:mobile:shared-app:compileKotlinIosSimulatorArm64 # required: KMP boundary changed
```

`./gradlew build` runs the Testcontainers suite. If Docker is not running, say the integration
tests did not run — do not report the database behavior as verified.

The iOS host is outside Gradle. Building and logging in from Xcode is the only proof for iOS, and
it is a manual step.

## Scenarios that prove the feature works

Each maps to acceptance scenarios in [spec.md](spec.md).

**First login** (US1) — fresh install, no session. The login screen appears. Tap "ВОЙТИ": the
sheet lists Google and T-ID on Android, and Google, Apple, and T-ID on iOS. Choose Google, confirm
the account, and land on the main screen. `select count(*) from users` increases by one, and
`provider_identities` holds one row with Google's `sub`.

**Not-yet-available providers** (US1.6) — choose Apple or T-ID. A message says the provider is not
available yet. No spinner appears, no request is sent, and the sheet stays usable for Google.

**Same account returns** (US2) — clear app data, log in with the same Google account. No new
`users` row is created and the same `id` comes back. `provider_identities` holds the email,
display name, and avatar URL Google reported, refreshed on this login (FR-026). A Google
account with no picture still logs in, leaving `avatar_url` null.

**Legal line** (FR-028, SC-011) — the login screen shows the line below "ВОЙТИ" with both links
tappable, in light and dark, at the largest font scale. Logging in still takes one tap — the line
adds no checkbox and no extra step.

**Session restored** (US3) — log in, force-stop the app, reopen it. The main screen appears with
no login screen and no Google confirmation.

**No login-screen flash** (US3.3, FR-024) — with a session stored, launch repeatedly. The splash
screen gives way to the main screen directly; the login screen never appears in between.

**Session invalidated** (US3.2) — `delete from sessions where user_id = '<id>'`, then force-stop
and relaunch with a network. The launch renewal (FR-032) refreshes, gets `401`, local storage
clears, and the login screen returns.

**Sliding session window** (FR-025, SC-009) — relaunch with a network so the launch renewal fires,
then `select expires_at from sessions`: it has moved roughly 90 days out from now, not from the
original login. Relaunch again immediately and confirm no second renewal is sent — the renewal
fires only when the access token is expired or within five minutes of expiring (FR-032), and a
token issued moments ago is neither.
`update sessions set expires_at = now() - interval '1 day'` then force a refresh: it returns `401`
and the user is sent back to the login screen. Then confirm the other half: launch the app in
airplane mode several times and check that the stored `refreshTokenExpiresAtEpochSeconds` has not
moved. Only the server renews a session; opening the app offline never does.

**Unreachable server does not log anyone out** (FR-012, SC-014) — log in, then stop the server
and use the app. Requests fail with the ordinary message, but the session survives: force-stop and
relaunch, still with the server down, and the main screen appears rather than the login screen.
Repeat in airplane mode, and once more with the server returning `503`, then with it returning
`429`. None of the four signs the user out. Only `delete from sessions ...` — an explicit `401` —
does, which is the scenario above.

**Expired session at launch** (US3.4, FR-011) — with a session stored, edit the stored
`refreshTokenExpiresAtEpochSeconds` to a past instant, kill the app, and relaunch it in airplane
mode. The splash screen gives way to the login screen directly: the main screen never appears and
no request is made.

**Rate limit** (FR-027, SC-010) — send more than 100 requests in a minute from one address to each
of `POST /v1/auth/google`, `POST /v1/auth/google/code`, and `POST /v1/auth/refresh`. Every one of
the three starts returning `429`, and the app shows the ordinary failure message rather than
anything about limits. Then confirm the reverse: retrying a login by hand after several
cancellations never reaches the limit. Behind a proxy, confirm `TRUST_PROXY_HEADERS=true`, or
every user shares the proxy's counter.

**Forged confirmation is refused** (FR-031, SC-015) — `POST /v1/auth/google` directly, without the
app, with a self-signed token, a token for another audience, and a well-formed token carrying no
`sub` claim (FR-026). Each returns `401`, and `select count(*)` on `users`,
`provider_identities`, and `sessions` is unchanged after all three — nothing is created before
verification succeeds.

**Cancellation is silent** (US4.1) — dismiss Google's account picker. The screen returns to idle
with no message, and a second attempt succeeds. Dismissing the browser tab in the fallback path
behaves identically — it is a cancellation, not a failure.

**Browser fallback** (US1.7, FR-029, SC-012) — run on a device or emulator image without Google
Play services. Choosing Google opens a Custom Tab rather than the native sheet, with no prompt
asking the user to pick a path, and completing it lands on the main screen with the same
`provider_identities` row the native path would create. Confirm the flow never appears inside an
embedded web view, and that a cancellation on a device *with* Play services still does not open
the browser.

**Failure is recoverable** (US4.2) — stop the server, then log in. A plain-language message
appears, exposing no status code or provider text. Restart the server and retry successfully.

**One attempt at a time** (US4.4, FR-015) — tap "ВОЙТИ" repeatedly while an attempt runs, then
tap a provider in the sheet twice and two different providers in quick succession. Only one provider
flow starts in every case.

**Second device logs the first out** (FR-030, SC-013) — log in with the same Google account on a
second device, then relaunch the first with a network. Its launch renewal returns `401` and it
lands on the login screen; `select count(*) from sessions where user_id = '<id>'` is 1 throughout.

**An attempt that never returns** (FR-013, SC-007) — start Google login and leave the
confirmation open without answering it. After 60 seconds the login screen is idle again, with no
message, and a fresh attempt starts normally.

**Reduced motion** (FR-020) — enable the system reduced-motion setting. The marquee and rotating
topic stop, and all copy stays fully readable.

**Themes and scaling** (FR-019, FR-021, SC-008) — check light and dark at 320 dp of width and at
200% font scale, then both at once. Nothing clips or overlaps, overflowing content scrolls, and
everything clears the system bars. On Android, `adb shell wm size` and `adb shell settings put
system font_scale 2.0` reproduce both extremes without a second device.
