# Quickstart: Login Screen

**Branch**: `feature/001-login-screen` | **Feature ID**: `001-login-screen` | **Refreshed**: 2026-08-15

How to configure, run, and verify the feature. Shapes and rationale live in [plan.md](plan.md),
[research.md](research.md), [data-model.md](data-model.md), and [contracts/](contracts/).

## Prerequisites

- JDK 17, Android SDK with `compileSdk` 37, Xcode for the iOS host.
- Docker running — Testcontainers starts PostgreSQL 17 for the server integration suite.
- A local PostgreSQL 17 for running the server by hand.
- A Google Cloud project with OAuth client IDs for **web**, **Android** (package name plus signing
  SHA-1), and **iOS**. The web client ID is the `serverClientId` both platforms send and one of the
  audiences the server accepts; the Android and iOS IDs are the audiences of tokens minted through
  the browser fallback, so all three are configured on the server.
- For the fallback: the Android client's reversed client ID registered as a redirect URI, matching
  the intent filter in `AndroidManifest.xml`.
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
REFRESH_TOKEN_TTL_SECONDS=7776000          # 90 days (FR-026); also the shipped default
AUTH_RATE_LIMIT_REQUESTS_PER_MINUTE=100    # per originating IP, all three auth endpoints (FR-033)
```

Everything above is read by `AppConfigLoader` in `core-config`, the only thing that reads `.env`.
`AuthConfig` rejects a short or well-known `JWT_SECRET` at startup, so the process fails fast rather
than issuing forgeable tokens. `.env.example` lists every variable.

Mobile — the base URL, the web client ID, and the two legal addresses are parameters of
`initKoin(...)`, never global state. The Android emulator reaches a host-machine server at
`http://10.0.2.2:8080`. The legal addresses may be null until the documents exist; the line and its
links are built regardless (FR-051).

## Run

```shell
./gradlew :services:server:app:run              # server
./gradlew :apps:mobile:android-app:installDebug # Android
```

iOS: open `apps/mobile/ios-app/YapApp.xcodeproj`, build `:apps:mobile:shared-app` first so
`YapShared.framework` exists, let Swift Package Manager resolve the pinned GoogleSignIn dependency,
then run. The host must register the reversed-client-ID URL scheme and replace the iOS and web
client-ID placeholders in `YapApp.swift`; `GoogleSignInBridge` is passed into `initIosKoin`.

## Verify

| Purpose | Command |
| --- | --- |
| Everything — compilation, tests, Detekt | `./gradlew build` |
| KMP boundary — required for any `commonMain` or dependency change | `./gradlew :apps:mobile:shared-app:compileKotlinIosSimulatorArm64` |
| The auth slice alone, while iterating | `./gradlew :apps:mobile:feature-auth:impl:allTests :apps:mobile:feature-auth:impl:detekt` |
| Network, theme, and root modules | `./gradlew :apps:mobile:core-network:allTests :apps:mobile:core-design:build :apps:mobile:app-root:build` |
| Wiring guard | `./gradlew :apps:mobile:feature-auth:impl:androidHostTest --tests '*FeatureAuthModuleTest*'` |

`./gradlew build` runs the Testcontainers suite. If Docker is not running, say the integration tests
did not run — do not report the database behaviour as verified. The iOS host is outside Gradle:
building and logging in from Xcode is the only proof for iOS, and it is a manual step.

## Scenarios that prove the feature works

Each maps to acceptance scenarios in [spec.md](spec.md).

**First login** (US1) — fresh install, no session. Tap "ВОЙТИ": the selection sheet opens as its own
destination and lists Google and T-ID on Android, all three on iOS. Choose Google: the sheet
animates closed, the button shows progress, confirm the account, land on the main screen.
`select count(*) from users` increases by one and `provider_identities` holds one row with Google's
`sub`.

**Providers not yet available** (SC-006) — choose Apple or T-ID. The sheet closes and a message
names *that* provider as coming soon. No spinner, no request, and Google still works.

**Apple absent on Android** (SC-004) — Apple is not in the Android list in any state.

**Adding a provider costs nothing upstream** (SC-016) — on a scratch commit, give T-ID a
`ProviderLogin` binding and mark it enabled in the roster, then confirm `git diff --name-only` lists
only the new login implementation, the Koin module, and the roster — not `LoginViewModel.kt` and not
`LoginScreen.kt`. Discard afterwards.

**Same account returns** (US2) — clear app data, log in with the same Google account. No new `users`
row; the same `id` comes back, with email, display name, and avatar refreshed (FR-021). An account
with no picture still logs in, leaving `avatar_url` null.

**Legal line** (SC-011) — the line sits below "ВОЙТИ" with both links tappable, in both themes, at
the largest font scale, and adds no step to logging in.

**Session restored** (US3) — log in, force-stop, reopen. The main screen appears with no login
screen and no Google confirmation.

**No login-screen flash** (FR-002) — with a session stored, launch repeatedly. The splash gives way
to the main screen directly.

**Session invalidated** (US3.2) — `delete from sessions where user_id = '<id>'`, then relaunch with
a network. The launch refresh gets a `401`, local storage clears, the login screen returns.

**Sliding window** (SC-009) — relaunch with a network so the refresh fires, then
`select expires_at from sessions`: roughly 90 days from now, not from the original login. Relaunch
immediately and confirm no second refresh is sent — it fires only when the access token is expired
or within five minutes of expiring. Then launch in airplane mode several times and confirm the
stored `refreshTokenExpiresAtEpochSeconds` has not moved: only the server renews a session.

**Unreachable server does not log anyone out** (SC-014) — stop the server and use the app; requests
fail with the ordinary message but the session survives a force-stop and relaunch. Repeat in
airplane mode, then with the server returning `503`, then `429`. None of the four signs the user
out; only an explicit `401` does.

**Expired session at launch** (US3.4) — edit the stored `refreshTokenExpiresAtEpochSeconds` to the
past, kill the app, relaunch in airplane mode. The splash gives way to the login screen directly:
no main screen, no request.

**Rate limit** (SC-010) — send more than 100 requests a minute from one address to each of
`/v1/auth/google`, `/v1/auth/google/code`, and `/v1/auth/refresh`. All three start returning `429`
and the app shows the ordinary failure message. Behind a proxy, confirm `TRUST_PROXY_HEADERS=true`,
or every user shares one counter.

**Forged confirmation refused** (SC-015) — `POST /v1/auth/google` directly with a self-signed token,
a token for another audience, and a well-formed token with no `sub`. Each returns `401`, and the row
counts in `users`, `provider_identities`, and `sessions` are unchanged after all three.

**Cancellation is silent** (US4.1) — dismiss Google's account picker: idle screen, no message, and a
second attempt succeeds. Dismissing the browser tab behaves identically.

**Dismissing the sheet** (FR-032) — dismiss by system back, by swipe, and by tapping the scrim. Each
closes it, returns nothing, starts no login. Only the system-back case is reachable by an automated
test (`RootBackStackTest`).

**Browser fallback** (SC-012) — on an image without Play services, choosing Google opens a Custom Tab
with no prompt asking the user to pick a path, and completing it lands on the main screen with the
same `provider_identities` row. Confirm it never appears inside an embedded web view, and that a
cancellation on a device *with* Play services does not open the browser.

**Failure is recoverable** (US4.2) — stop the server and log in: a plain-language message with no
status code or provider text. Restart and retry successfully.

**One attempt at a time** (FR-031) — tap "ВОЙТИ" repeatedly during an attempt, then tap a provider
twice and two providers in quick succession. One flow starts in every case.

**Second device logs the first out** (SC-013) — log in with the same account on a second device,
relaunch the first with a network: its launch refresh returns `401` and it lands on the login
screen; `select count(*) from sessions where user_id = '<id>'` stays 1.

**An attempt that never returns** (SC-007) — start Google login and leave the confirmation open.
After 60 seconds the screen is idle again, with no message, and a fresh attempt starts normally.

**Two messages in a row** (FR-050) — fail a login twice quickly. Both appear in order, neither is
dropped, and each leaves by moving upward after 2600 ms (SC-019).

**Dark theme** (SC-018) — the message keeps the light-theme colour; nothing else changes tone.

**Reduced motion** (FR-045) — the marquee and rotating topic stop, all copy stays readable, and the
message appears and leaves without motion while still showing for its full duration. The sheet keeps
its standard animation — that is deliberate.

**Sheet chrome against the design** (FR-037) — marks, handle, border, radius, paddings, and label
tracking match `screen_login.dc.html`, and the section label is uppercase.

**Rotation with the sheet open** — the sheet survives and no login starts twice. Process death is
deliberately different: the app returns to the login screen with the chooser closed.

**Tap count** (SC-021) — two taps from the login screen to a completed Google sign-in.

**Themes and scaling** (SC-008) — light and dark at 320 dp and at 200% font scale, then both at
once. Nothing clips or overlaps, overflow scrolls, everything clears the system bars. On Android,
`adb shell wm size` and `adb shell settings put system font_scale 2.0` reproduce both extremes.

The one edge case with no check here — the roster changing while the sheet is open — is unreachable
today: the local roster is a pure function of `Platform` and re-emits nothing after its first value.
It becomes testable with the remote roster, alongside the empty-roster state spec.md puts out of
scope.

## Structural checks

Cheap greps standing in for the structural success criteria.

```bash
# SC-017 — no colour literals in login UI. Brand colours live inside the drawables.
# core-design/theme is exempt: YapColors is the one file where literals belong.
rg 'Color\(0x' apps/mobile/feature-auth \
  apps/mobile/core-design/src/commonMain/kotlin/app/yap/core/design/navigation \
  --glob '!**/build/**'                                              # expect: no matches

# FR-007 / SC-020 — no provider knowledge in the login slice's state code
rg 'AuthProviderType\.|providers' \
  apps/mobile/feature-auth/impl/src/commonMain/kotlin/app/yap/feature/auth/presentation/login
                                                                     # expect: no matches

# FR-038 — exactly one place turns a provider into display data
rg -l 'AuthProviderType\.(APPLE|GOOGLE|T_ID)' apps/mobile --glob '!**/build/**'
     # expect: AuthProviderUiMapper.kt, GoogleProviderLogin.kt, DefaultObserveAuthProvidersUseCase.kt

# FR-013 — no UI resource reachable from domain or data
rg 'generated.resources|StringResource|DrawableResource' \
  apps/mobile/feature-auth/impl/src/commonMain/kotlin/app/yap/feature/auth/{domain,data}
                                                                     # expect: no matches

# FR-064 — no feature reads a status code; outcomes come from core-network
rg 'HttpStatusCode' apps/mobile/feature-auth --glob '!**/build/**'    # expect: no matches
```

### The package boundary holds (SC-024)

```bash
# Drawing code must not be left beside the state code:
grep -rl 'androidx.compose' \
  apps/mobile/feature-auth/impl/src/commonMain/kotlin/app/yap/feature/auth/presentation/*/[A-Z]*.kt

# State code must not have drifted into ui/:
grep -L 'androidx.compose\|TestTags' \
  apps/mobile/feature-auth/impl/src/commonMain/kotlin/app/yap/feature/auth/presentation/*/ui/*.kt
```

**Expected**: both print nothing.

### No comments remain (SC-023)

```bash
find apps services shared convention-plugins -path '*/build/*' -prune -o -type f -name '*.kt' -print0 \
  | xargs -0 grep -cE '^\s*(//|/\*|\*)' | awk -F: '{s+=$NF} END {print "comment lines:", s}'
```

**Expected**: `comment lines: 0`.

## Done when

Every command above passes, every scenario has been walked, and the result is reported with the
exact commands run. If any step fails or is skipped, say so plainly rather than omitting it.
