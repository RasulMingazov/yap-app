# Phase 0 Research: Login Screen

Every unknown in the plan's Technical Context is resolved below. Nothing is left as
NEEDS CLARIFICATION.

## R1. Google login on Android

**Decision**: Credential Manager with `GetSignInWithGoogleOption` as the primary path, wrapped by
`AndroidGoogleCredentialProvider` in `feature-auth/impl/src/androidMain`, which falls back to the
browser flow of R14 when no credential provider is present.

- `androidx.credentials:credentials` + `androidx.credentials:credentials-play-services-auth`
  (1.7.0-alpha03 is what the current Android guide pins; take the newest stable at
  implementation time and record it in `gradle/libs.versions.toml`).
- `com.google.android.libraries.identity.googleid:googleid` for `GetSignInWithGoogleOption`
  and `GoogleIdTokenCredential`.
- Build the option with the **web** client ID as `serverClientId` plus a nonce, call
  `CredentialManager.getCredential(request, context)`, then extract the credential when
  `credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL` and read
  `idToken`.
- The call needs an **Activity** context, not the application context. `MainActivity` publishes
  itself through an `ActivityProvider` in `core-common`'s `androidMain`, wired by
  `initAndroidKoin`. The provider holds a nulled-out reference outside the resumed lifecycle so
  no Activity leaks.

**Rationale**: `GetSignInWithGoogleOption` is the button-driven flow, which is exactly what the
design specifies — the user taps "ВОЙТИ", then picks a provider. `GetGoogleIdOption` with
`filterByAuthorizedAccounts` is the one-tap bottom-sheet flow and would fire before the user
chose anything.

**Alternatives considered**: the legacy `GoogleSignInClient` from play-services-auth is
deprecated in favour of Credential Manager. A prebuilt KMP wrapper (KMPAuth,
compose-google-login) would hide both platforms behind one dependency, but it owns the very
boundary this project's guides put in `androidMain`/`iosMain`, and it would place a third-party
type in the feature's public surface.

**Cancellation**: Credential Manager reports user dismissal as
`GetCredentialCancellationException`. FR-013 requires cancellation to be silent, so the adapter
maps it to a distinct `LoginOutcome.Cancelled` rather than to a failure — this is the branch the
"cancellation emits no news" rule in `docs/mobile/presentation/002-ui-compose.md` depends on.

**Falling back**: `GetCredentialProviderConfigurationException` — and `NoCredentialException` when
no provider can serve the request at all — mean the device has no Google credential provider.
Those two, and only those two, trigger R14's browser flow. A cancellation must **not** trigger it:
the user said no, and reopening the same question in a browser would be the opposite of FR-013.

## R2. Google login on iOS

**Decision**: `GoogleCredentialProvider` is a public `suspend` interface in `feature-auth/api`,
implemented in **Swift** in the Xcode host using the GoogleSignIn SDK, and handed to Kotlin
through `initIosKoin(baseUrl:googleServerClientId:googleCredentialProvider:)` exported by
`shared-app`.

- GoogleSignIn is added to the Xcode project via Swift Package Manager.
- `shared-app` must `api(project(":apps:mobile:feature-auth:api"))` and `export` it from the
  framework so Swift can see and implement `GoogleCredentialProvider`.
- Kotlin `suspend` functions surface to Swift as `async`, so the Swift implementation is a plain
  `async` function returning `GoogleCredential.IdToken`. iOS never returns an authorization code:
  the browser fallback of R14 is Android-only, which is also why `/v1/auth/google/code` is an
  Android-only door.
- The host also needs the reversed-client-ID URL scheme and must forward
  `application(_:open:options:)` to `GIDSignIn.sharedInstance.handle(_:)`.

**Rationale**: The GoogleSignIn SDK is an Objective-C/Swift framework that would otherwise need
cinterop or CocoaPods wiring inside the Gradle build. Keeping it entirely in Xcode means the
Gradle build stays independent of an iOS SDK, and the Kotlin side sees one narrow suspend
function. `initKoin` already accepts an `appDeclaration` for exactly this kind of platform-only
binding, so the pattern is the one the DI guide already describes.

**Alternatives considered**: CocoaPods integration through the Kotlin CocoaPods plugin — rejected
because it makes `./gradlew build` depend on a working CocoaPods install. cinterop against the
GoogleSignIn framework — rejected as more fragile than a ten-line Swift adapter.

**Consequence to state plainly**: `./gradlew build` cannot verify the iOS host. The Gradle side is
verified by `:apps:mobile:shared-app:compileKotlinIosSimulatorArm64`; the Swift side is verified
by building and running the app in Xcode, which is a manual step in `quickstart.md`.

## R3. Session storage on device

**Decision**: one `SessionStorage` `expect`/`actual` pair in `feature-auth/impl`.

- **Android**: an AES-GCM key generated in the Android Keystore encrypts the token values; the
  ciphertext lives in DataStore Preferences (`androidx.datastore:datastore-preferences`).
- **iOS**: Keychain (`kSecClassGenericPassword`) through `platform.Security`, which Kotlin/Native
  provides with no extra dependency, with `kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly`.

**Rationale**: The refresh token is a long-lived credential, so it belongs behind
hardware-backed protection on both platforms. `androidx.security:security-crypto`
(`EncryptedSharedPreferences`) is the classic answer but is no longer actively developed;
generating the key in the Keystore directly and storing ciphertext in DataStore gives the same
protection with a maintained storage API and a coroutine interface. On iOS the Keychain is
already encrypted by Secure Enclave-derived class keys, so no second layer is needed.

**Alternatives considered**: a KMP secure-storage library (KVault, Kissme, KSafe) — rejected
because the `expect`/`actual` pair is roughly fifty lines per platform and the guides already
require platform adapters to sit in platform source sets. Plain DataStore with no encryption —
rejected: `allowBackup="false"` and the app sandbox are not sufficient protection for a refresh
token on a rooted device.

## R4. Keeping the network layer authenticated

**Decision**: `feature-auth/impl` provides the `AccessTokenProvider` implementation that
`coreNetworkModule` already looks up with `getOrNull<AccessTokenProvider>()`.

- `getAccessToken(rejectedAccessToken = null)` returns the stored access token.
- `getAccessToken(rejectedAccessToken = <token>)` refreshes: it serializes concurrent callers with
  a mutex, returns a newer token immediately if one arrived while waiting, calls
  `POST /v1/auth/refresh` otherwise, and persists the rotated session — the seam
  `docs/mobile/data/002-data-sources.md` already specifies.
- **Only an explicit rejection signs the user out.** A `400` or `401` from that call is the server
  answering and refusing the session: local storage is cleared and `LoggedOut` is published
  (FR-012). Anything that is *not* an answer about the session — no network, a timeout, a `5xx`,
  or a `429` — MUST leave the stored session untouched and surface to the caller as an ordinary
  failure to be retried later (FR-012, SC-014). Two cases deserve naming because getting them
  wrong is silent: a transport failure would otherwise eject a user for riding a lift, and a `429`
  is an answer about the caller's address rather than about the session (FR-027).
- Koin ordering matters: `AccessTokenProvider` must be declared before `NetworkClient` is first
  resolved, so `appModules` lists `featureAuthModule()` ahead of `coreNetworkModule()`.

**Rationale**: The seam exists and is documented; the auth feature is its only possible owner.
Repositories and data sources therefore never implement refresh or 401 retry. Concentrating the
sign-out decision here is also what makes FR-012's "unreachable is not invalid" rule provable in
one place instead of at every call site.

## R5. Verifying the Google ID token on the server

**Decision**: `GoogleIdentityVerifier` in `services/server/feature-auth/identity`, built on
`com.auth0:java-jwt` (already in the catalog) plus `com.auth0:jwks-rsa` for the key set.

- Fetch keys from `https://www.googleapis.com/oauth2/v3/certs` through a `JwkProvider` with
  caching and rate limiting; do not fetch per request.
- Verify: RS256 signature against the matching `kid`; `iss` is `accounts.google.com` or
  `https://accounts.google.com`; `aud` is one of the configured client IDs; `exp` is in the
  future; `nonce` equals the nonce the client submitted alongside the token.
- Read `sub` as the stable provider account identifier, and `email`, `name`, and `picture` only
  as non-authoritative profile data (FR-026). Any of those three may be absent and is stored as
  null. `sub` may not: a token that verifies but carries no `sub` is rejected exactly like an
  unverifiable one, because it cannot be matched on a later login and would silently hand the
  user a fresh account with no progress (FR-026, FR-007).
- The `GoogleAuthConfig` **type** belongs to `feature-auth`, not `core-config` — `AuthConfig`'s own
  KDoc already says provider-specific settings stay in the feature that consumes them. Reading the
  three values is a different job and stays with `core-config`: the constitution fixes it as the
  owner of environment loading and validation, and `AppConfigLoader` is the only thing that reads
  the `.env` file `quickstart.md` documents. So `AppConfigLoader` loads
  `GOOGLE_WEB_CLIENT_ID`, `GOOGLE_ANDROID_CLIENT_ID`, and `GOOGLE_IOS_CLIENT_ID`, and `app` passes
  them into the feature's `GoogleAuthConfig`. Loading in `app` would bypass `.env` entirely and
  silently break the documented setup.

**Rationale**: FR-031 requires the server to establish for itself that a confirmation is genuine
rather than trusting what the app sends — a request can reach `/v1/auth/google` without passing
through the app at all, so a client-side check would protect nobody. Verifying the signature
locally against Google's published keys is the documented way to do that, and it avoids a network
round trip to the `tokeninfo` endpoint on every login, which is rate-limited and adds a
dependency on Google being reachable synchronously inside our request. No account is created or
resolved and no session is issued before verification succeeds (FR-031).

**Alternatives considered**: `google-api-client`'s `GoogleIdTokenVerifier` — it does the same job
but drags in a large transitive tree for one class, and java-jwt is already a dependency.

**On the nonce**: the client generates it, so an attacker who controls the client controls both
halves. It is defense in depth, not a challenge-response. `TokenService.createChallenge` in
`core-security` is the upgrade path if a server-issued challenge is later required; it is not
wired in this feature.

## R6. Account resolution and session persistence on the server

**Decision**: three tables owned by `services/server/feature-auth`, with a forward-only Flyway
migration under its own `src/main/resources/db/migration`. See [data-model.md](data-model.md).

- `provider_identities` carries a unique constraint on `(provider, provider_user_id)`. That
  constraint — not application code — is what makes FR-007 (one provider account resolves to one
  Yap account) hold under concurrent first logins.
- FR-026 stores the provider's email, display name, and avatar URL on `provider_identities`,
  refreshed on each login, all nullable, with no unique constraint and no index. FR-008
  (providers never linked automatically) is satisfied by never querying on them: two providers
  reporting the same address produce two rows and two users, by construction. The absence of an
  index is deliberate — it removes the temptation to add the lookup that would break FR-008.
- These three come from the ID token's `email`, `name`, and `picture` claims, which
  `GetSignInWithGoogleOption` and the iOS SDK both return. They are read from the already-verified
  token, so no extra call to Google's userinfo endpoint is needed.
- Refresh tokens are stored only as SHA-256 hashes via the existing `TokenService.hash`.
- Rotation replaces the stored hash inside the same transaction that validates the old one, so
  two concurrent rotations cannot both succeed. The same statement pushes `expires_at` forward
  by `REFRESH_TOKEN_TTL_SECONDS`, which is what makes FR-025's 90-day window sliding. The
  existing `REFRESH_TOKEN_TTL_SECONDS` default of 2 592 000 seconds is 30 days, so this feature
  raises it to 7 776 000 for the configuration to match the product decision.
- **A successful rotation is the only thing that renews a session** (FR-025). `expires_at` is
  written here and nowhere else, and the client copies back whatever the server states rather
  than advancing its own stored copy. So a user who opens the app for 90 days without ever
  reaching the server keeps a session that quietly ages out — which is what the spec chose, and
  the reason the device is never allowed to move the date itself.

**Rationale**: `docs/server/002-persistence.md` puts a feature's tables and migrations in the
feature. `docs/testing/003-backend-integration.md` requires the uniqueness constraint, the
rotation race, and the migration bootstrap to be proven against real PostgreSQL — those are the
Testcontainers tests.

**PostgreSQL version**: pinned to **17** in both the Testcontainers image and the deployment
notes. `docs/testing/003` requires the same major in both and forbids `latest`; this feature is
the first to introduce either, so it owns the choice.

## R7. Reduced motion and platform detection

**Decision**: two platform capability ports in `core-common`, consumed by `LoginUiStateMapper`.

- `Platform` — `ANDROID` or `IOS`, resolved by an `expect fun currentPlatform()` pair and bound by
  the platform entry point. Drives FR-003: Apple is absent from the provider list on Android.
- `MotionPreferences.isReduced()` — Android reads `Settings.Global.ANIMATOR_DURATION_SCALE == 0f`;
  iOS reads `UIAccessibility.isReduceMotionEnabled`. Drives FR-020.

`MotionPreferences` is a **contract in `commonMain` with a narrow adapter in each platform source
set**, not an `expect class`. Two reasons, both load-bearing: the Android reading needs a `Context`
while the iOS one needs none, so the two would have different constructors; and R10 puts FR-020's
proof at the mapper boundary, which requires a test to hand the mapper a chosen answer — an
`expect class` cannot be stood in for. The same reasoning applies to `SessionStorage` in R3, whose
`expect`/`actual` pair is the factory `createSessionStorage()` rather than the type itself.

**Rationale**: `docs/mobile/presentation/002-ui-compose.md` names "platform capabilities" as a
mapper input and requires availability and visibility to be decided in the mapper, not in a
composable `when`. Putting both facts in `UiState` makes FR-003 and FR-020 provable by a mapper
test rather than by a screenshot test.

**Alternatives considered**: reading the preference inside the composable — rejected because it
puts a product decision in Compose and makes the requirement untestable at the mapper boundary.

## R8. Navigation after login

**Decision**: conditional navigation owned by `app-root`. It observes `ObserveAuthStateUseCase`
from `feature-auth/api` and holds either `AuthNavKey.Login` or `RootNavKey.Main` as the back
stack root. `LoginViewModel` never navigates.

Each root is composed by the module that owns it: `featureAuthModule()` registers
`navigation<AuthNavKey.Login> { LoginScreen() }` and `appRootModule()` registers
`navigation<RootNavKey.Main> { MainPlaceholderScreen() }`, so `koinEntryProvider()` can resolve
both. The scaffolded `RootNavKey.Auth` is deleted: a feature owns its own destinations, and a key
that no module composes fails at first navigation rather than at build time.

**Rationale**: `feature-auth` cannot reference a main-screen key that no feature owns yet, and
`app-root` is the only module that sees both. This is the conditional-navigation pattern the
`navigation-3` skill documents, it satisfies the constitution's "a view model reports navigation
intent rather than navigating itself", and it makes FR-012 (invalid session returns to login)
fall out of the same observation rather than needing separate handling.

**Alternatives considered**: having `LoginViewModel` push a main-screen key — impossible without
a dependency the module graph forbids, and it would leave FR-012 unimplemented.

## R9. Screen composition

**Decision**: one `LoginViewModel` with a nested `UiState`, `News`, and `Event`, one
`LoginUiStateMapper`, and a `LoginScreen` composable in
`feature-auth/impl/presentation/login`.

- Provider-sheet visibility is repeatable screen state and lives in `UiState`; the transient
  message is one-shot and travels as `News.ShowMessage`, per the UI guide's split.
- The sheet is Material3 `ModalBottomSheet`; dismissal is an `Event`, never an error (FR-016).
- Marquee, rolling topic, spinner, and press feedback are Compose-owned animation, gated by
  `UiState.isMotionReduced`.
- Copy lives in the feature's own `composeResources`; `UiState` carries `StringResource`
  references, never resolved strings.
- The legal line (FR-028) sits below the caption. Its two links open through Compose's
  `LocalUriHandler`, which exists in `commonMain`, so no new platform port is needed. The
  destination addresses are injected as module-function parameters alongside the base URL rather
  than hard-coded, since they are supplied later — the same rule the DI guide applies to all
  configuration.
- `core-design` gains nothing in this feature. `YapTypography` already matches the design's type
  ramp; the design's specific palette is applied through the feature's own colours until a second
  screen needs them, at which point they move to `YapTheme`.
- The screen is a three-part split, not one stacked column: the tilted band at the top, the action
  block at the bottom, and the hero centred in what is left. It is built as a scrolling column with
  a minimum height of one viewport and `SpaceBetween`, so the split holds at ordinary sizes and
  gives way to scrolling at 320 dp and 200% font scale. Hero, topic, and body are start-aligned;
  only the caption and the legal line are centred. Below 340 dp the two display sizes and the
  horizontal padding step down, matching the design's own container query.
- FR-019 says "following the colour roles defined in the design", and the design lives outside the
  repository. T076 therefore records the resolved role→value table for light and dark **in this
  section**, so the requirement can be reviewed against a committed artefact instead of a tool the
  reviewer may not be able to open.

### Resolved colour roles (T076)

Lifted from the design prototype's `themeVals()` (`screen_login.dc.html`) and implemented in
`feature-auth/impl/presentation/login/LoginColors.kt`.

| Role | Light | Dark | Used by |
| --- | --- | --- | --- |
| `background` | `#FFFEF7` | `#08070A` | screen ground; also the platform splash colour |
| `onBackground` | `#0B0A0D` | `#FAF9F6` | hero heading |
| `marqueeBackground` | `#D9FF57` | `#D9FF57` | scrolling promotional band |
| `onMarquee` | `#0B0A0D` | `#0B0A0D` | band text |
| `accent` | `#5E3689` | `#D9FF57` | rotating topic word, provider marks |
| `actionBackground` | `#0B0A0D` | `#D9FF57` | primary action |
| `onAction` | `#FFFAFC` | `#0B0A0D` | primary action label and its progress indicator |
| `muted` | `#5F5A6B` | `#8F8899` | body copy, legal line prose |
| `caption` | `#8B8496` | `#5B5765` | the caption under the primary action |
| `link` | `#0B0A0D` | `#FAF9F6` | the legal line's two links |
| `surface` | `#FFFEF7` | `#15141A` | provider sheet container |
| `onSurface` | `#0B0A0D` | `#FAF9F6` | provider sheet title and rows |
| `sheetLabel` | `#8B8496` | `#7C7787` | provider sheet heading |
| `bannerBackground` | `#5E3689` | `#26232C` | the transient message |
| `onBanner` | `#FFFAFC` | `#FAF9F6` | transient message text |

The band keeps one pair in both themes: in the design it is a printed sticker over the screen, not a
surface that follows it. The accent splits from the action colour in light, where the topic word is
purple and the button is near-black.

`link` is the only role with no counterpart in the prototype — the design carries no legal line
(FR-028) — so it follows `onBackground`, and the line's prose reuses `muted`.

**Rationale**: These are the choices `docs/mobile/presentation/001-view-models.md` and
`002-ui-compose.md` already fix. The one judgement call is leaving colour in the feature: the UI
guide says a composable moves to `core-design` only when it has a stable API and a real consumer,
and the same reasoning applies to tokens.

## R10. Verification strategy

| Behavior | Where it is proven |
| --- | --- |
| Provider list contents per platform, availability, reduced motion, loading state | `LoginUiStateMapper` tests — the primary surface |
| Event → state, event → news, duplicate-tap guard, cancellation silence, cleanup | `LoginViewModel` tests through `runViewModelTest` |
| Session cache reuse, forced refresh, concurrent refresh | Auth repository tests with data-source and storage stubs |
| Refresh `401`/`400` signs out; no network, timeout, `5xx`, and `429` all leave the session stored and logged in (SC-014) | `AccessTokenProvider` tests, one case per outcome, over a stubbed data source |
| ID token → session mapping, HTTP failure translation | Remote data source and mapper tests |
| Koin graph completeness | `verify()` test in the module that owns each graph |
| Google token verification: bad signature, wrong `aud`, wrong `iss`, expired, nonce mismatch, absent `sub` — none creates a user, an identity, or a session (FR-031, SC-015) | `GoogleIdentityVerifier` tests with locally signed tokens, plus a route test asserting no rows are written |
| Fallback triggers on a missing provider but never on a cancellation | `AndroidGoogleCredentialProvider` tests over a stubbed Credential Manager |
| Code exchange: success, replayed code, verifier mismatch, Google unreachable | Exchange-adapter tests against a stubbed token endpoint |
| Route parsing, serialization, and the `AuthFailure` each route raises | Ktor `testApplication` route tests inside `feature-auth` |
| `AuthFailure` → `400`/`401`/`503`, and `429` over the limit on all three unauthenticated endpoints | `app`'s own tests — both the `StatusPages` mapping and the limiter live there, and no feature may depend on `app` |
| Auth state stays `Unknown` until storage is read, then resolves once | Root auth-state test; the absence of a visible flash is checked by hand |
| Renewal fires only when the stored access token is expired or within five minutes of expiring (FR-032) | `RenewSessionUseCase` tests over stubbed storage and a stubbed `AccessTokenProvider` |
| Launch renewal runs once per launch, only after the local decision, and never delays the main screen (FR-032) | Root/launch-renewal test over a stubbed `RenewSessionUseCase` |
| An attempt unresolved after 60 seconds ends as a cancellation, silently (FR-013, SC-007) | `LoginWithGoogleUseCase` test on virtual time |
| Unique provider identity under concurrent first login, refresh-token rotation race, sliding `expires_at`, expired-session rejection, migration bootstrap | Testcontainers PostgreSQL 17 integration tests |
| Email stored and refreshed but never used to match an account | Repository test asserting a second provider with the same address yields a second user |
| Rendering, content order, semantics, light/dark, narrow width, large font scale, the four-second banner (FR-023) | Compose UI tests — only for what a mapper test cannot prove, and each written before the composable it covers |

**Rationale**: `docs/testing/001-structure.md` requires each behavior to be tested once at the
boundary that owns it, and `docs/testing/003` forbids substituting a fake for PostgreSQL
semantics.

## R11. New dependencies to add to `gradle/libs.versions.toml`

| Coordinate | Used by | Why |
| --- | --- | --- |
| `androidx.credentials:credentials`, `:credentials-play-services-auth` | `feature-auth/impl` `androidMain` | Credential Manager (R1) |
| `com.google.android.libraries.identity.googleid:googleid` | `feature-auth/impl` `androidMain` | `GetSignInWithGoogleOption`, `GoogleIdTokenCredential` (R1) |
| `net.openid:appauth` | `feature-auth/impl` `androidMain` | PKCE + Custom Tabs browser fallback (R14) |
| `androidx.datastore:datastore-preferences` | `feature-auth/impl` `androidMain` | Encrypted session container (R3) |
| `com.auth0:jwks-rsa` | `services/server/feature-auth` | Google JWKS with caching (R5) |
| `androidx.core:core-splashscreen` | `apps/mobile/android-app` | System splash held until auth state resolves (R12) |
| `io.ktor:ktor-server-status-pages` | `services/server/app` | Shared failure-to-status mapping the server guide requires |
| `io.ktor:ktor-server-rate-limit` | `services/server/app` | Per-IP limiting on the auth endpoints (R13) |
| `io.ktor:ktor-server-test-host` | server tests | Route tests (R10) |
| `org.testcontainers:postgresql` | server tests | PostgreSQL 17 integration tests (R6, R10) |

Resolve each to the newest stable release at implementation time and then **pin that exact
version in `gradle/libs.versions.toml`** — no dynamic ranges, no `+`, and no alpha unless nothing
stable exists for the coordinate. The `1.7.0-alpha03` named in R1 is what the current Android
guide happens to show, not a pin. GoogleSignIn for iOS is added in Xcode through Swift Package
Manager and never appears in the version catalog.

## R14. Browser fallback for devices without Google's services

**Decision**: OAuth 2.0 authorization code flow with PKCE, presented in **Chrome Custom Tabs**
via `net.openid:appauth`, used on Android when and only when Credential Manager reports that no
provider exists (FR-029).

- AppAuth generates the `code_verifier`/`code_challenge` pair, launches the Custom Tab, and
  catches the redirect. It is the reference implementation of RFC 8252 and saves hand-rolling
  state validation and redirect handling.
- Redirect URI is the reversed client ID scheme —
  `com.googleusercontent.apps.<id>:/oauth2redirect` — registered as an intent filter on a
  dedicated activity in `android-app`.
- The client never exchanges the code. It hands `code` + `code_verifier` + `redirect_uri` to
  `POST /v1/auth/google/code`, and the server performs the exchange. Installed-app clients have
  no secret, so PKCE is the whole proof — which is why the verifier is required, not optional.
- Scopes: `openid email profile`, matching the claims FR-026 stores.

**Why not a WebView**: Google returns `disallowed_useragent` for OAuth inside embedded web views
and has done since 2016. This is policy, not a bug, and it cannot be worked around by spoofing a
user agent without violating Google's terms. An embedded view also breaks the security property
that makes this flow safe — the user cannot see the address bar and verify they are typing their
password into Google. FR-029 therefore forbids it outright rather than leaving it as a fallback
of the fallback.

**Why iOS needs nothing**: the GoogleSignIn SDK already presents
`ASWebAuthenticationSession`, which is the system browser under Apple's rules, and it does not
require Google Play services. iOS has no equivalent gap, so it keeps one path.

**Rationale**: A de-Googled device — GrapheneOS without sandboxed Play, or any build shipped
without Google's services — has no credential provider at all. Since Google is the only working
provider in this feature, such a device would otherwise be locked out of the product entirely, with
nothing the user could do about it. The browser flow removes that dead end for the cost of one
adapter and one endpoint.

**Alternatives considered**: hand-rolling PKCE over `androidx.browser` — feasible, but AppAuth
already handles the redirect race and state validation that are easy to get subtly wrong.
Making the browser flow the only path on both platforms — rejected because it costs every user
the native account sheet the design's own "ВХОД ЗА 1 ТАП" promise depends on.

**Known limitation**: a device with no Google provider *and* no browser capable of handling the
intent cannot log in. That is a genuine dead end, reported through the ordinary failure path
(FR-014); T-ID is what eventually removes it, not more fallbacks.

## R12. Holding the launch screen until auth state is known

**Decision**: use each platform's own splash mechanism and keep it up until the first `AuthState`
arrives (FR-024).

- **Android**: `androidx.core:core-splashscreen`, with
  `splashScreen.setKeepOnScreenCondition { authState == null }` installed in `MainActivity`
  before `setContent`.
- **iOS**: the storyboard launch screen the Xcode host already reserves a colour set for
  (`YapApp/Assets.xcassets/SplashBackground.colorset`), with the Compose root rendering nothing
  until the state resolves.
- `AuthState` therefore starts as **unknown**, not as `LoggedOut`. This is the whole point: if the
  initial value were `LoggedOut`, `app-root` would put the login screen on the back stack and then
  replace it, which is exactly the flash FR-024 forbids.

**Rationale**: The system splash is already on screen during process start, so extending it costs
one call and no new screen. Reading Keystore-decrypted DataStore or the Keychain is fast but not
instant, and it is disk I/O on a cold start, so the gap is real rather than theoretical.

**Alternatives considered**: an in-app splash composable, as the design prototype's unused
`splash` phase suggests — rejected because it would appear *after* the system splash, adding a
second flash rather than removing one. Blocking the main thread on the storage read — rejected
outright.

**Verification**: the tri-state is provable by a view-model or root-state test; the absence of a
visible flash is confirmed by hand on both platforms, since it is a timing property no unit test
observes.

## R13. Rate limiting the auth endpoints

**Decision**: Ktor's `RateLimit` plugin in `services/server/app`, applied per originating IP to
every unauthenticated endpoint — `POST /v1/auth/google`, `POST /v1/auth/google/code`, and
`POST /v1/auth/refresh` (FR-027).

- **The threshold is 100 requests per minute per IP**, read from
  `AUTH_RATE_LIMIT_REQUESTS_PER_MINUTE` with 100 as the default, so it can be retuned by
  configuration rather than by a release.
- All three doors are limited, not two. Every one of them is unauthenticated, which is exactly
  the condition FR-027 names; leaving `/v1/auth/google/code` open would put the whole limit behind
  a door an attacker can simply walk around.
- The client IP comes from Ktor's `ApplicationRequest.origin`, which honours forwarded headers
  when the already-present `TRUST_PROXY_HEADERS` setting is on. Behind a proxy with that flag
  off, every request would appear to come from the proxy and the limit would throttle everyone —
  so the deployment note is that the flag must be true wherever a proxy terminates TLS.
- Why 100 a minute clears both bars in SC-010: a person retrying after cancellations makes a
  handful of attempts a minute at most, and a shared office or carrier address pools devices that
  each log in about once every 90 days (FR-025), so their combined arrival rate stays orders of
  magnitude below the bucket. Automated abuse hits it immediately.
- Over the limit returns `429 Too Many Requests`. The client renders it through the ordinary
  failure path (FR-014) rather than a special message — the user does not need to know which
  server rule rejected them.
- **A `429` on `/v1/auth/refresh` must not sign the user out.** It is an answer about the
  caller's address, not a rejection of the session, so R4's rule applies: the stored session
  survives and the call is retried (FR-012, SC-014). Treating it as a rejection would let a noisy
  neighbour behind the same NAT sign a user out.

**Rationale**: These three endpoints are the only unauthenticated surface, so they are the only
place an attacker can create accounts or force Google token verifications without credentials.
The plugin is part of Ktor, needs no external store for a single instance, and the limit lives
in `app` beside the other shared plugins, which is where `docs/server/001-feature-boundaries.md`
puts cross-cutting HTTP concerns.

**Alternatives considered**: per-account or per-session counters — rejected as premature; they
need persisted state and cleanup for a threat that per-IP limiting already blunts. Leaving it to
a gateway or WAF — rejected because no such layer exists yet, so the server would ship open.

**Known limitation to record**: a shared outbound IP (office NAT, carrier NAT) pools many real
users behind one counter, which is why the threshold is generous rather than tight. If that
proves too coarse, the upgrade is a distributed limiter keyed on IP plus device, not a lower
threshold. The counter is also per instance, since the plugin keeps it in memory — a second
server instance doubles the effective limit. That is acceptable for a coarse abuse guard and is
the second reason the number is not treated as a precise control.

**Where it is tested**: the plugin lives in `app`, and neither `feature-auth` nor any `core-*`
module may depend on `app`, so a `testApplication` in `feature-auth` assembles the routes *without*
the limiter and can never observe a `429`. The limit is therefore proven in `app`'s own tests —
once against a probe route for the threshold and the per-IP key, and once across all three auth
doors to prove each really sits inside the limited scope. The same argument covers the status
codes: the `AuthFailure` → `400`/`401`/`503` mapping is a `StatusPages` plugin in `app` too, so a
feature route test cannot observe it either. The feature's route tests therefore assert the failure
a route **raises**, and every status code it eventually becomes — `429` included — is proven in
`app`'s own tests.

## R15. Renewing the session at launch

**Decision**: `app-root` fires a session renewal after auth state has resolved to logged-in, by
calling `RenewSessionUseCase` — a contract in `feature-auth/api`. The use case renews when the
stored access token has expired or is **within five minutes** of expiring, and calls the
`AccessTokenProvider` seam of R4, which already owns refresh and the sign-out decision — no new
endpoint, no new DTO, and no new failure handling.

- The condition lives in the **use case, not in `app-root`**. Both expiry fields sit in
  `SessionLocal`, which is `internal` to `impl`; a decision made in `app-root` would either widen
  that visibility — against Principle I — or degrade into an unconditional renewal on every launch.
  `app-root` therefore owns only the trigger, which is the one fact it can already see: auth state
  has resolved to logged-in, so renew once.
- **Five minutes** against the repository's 900-second `ACCESS_TOKEN_TTL_SECONDS` default is a
  third of the window: far enough out that a slow launch on a poor connection still finishes the
  rotation before the token dies, near enough that a healthy token is left alone.
- It runs **after** the launch decision, never before it. FR-011 forbids a request before the app
  decides where to land, and an offline launch must still reach the main screen; the renewal is a
  background effect of already being logged in, not a gate on it.
- It is **conditional**, not unconditional: a stored access token more than five minutes from
  expiry means no call, so opening the app ten times an hour costs one renewal at most, not ten.
- Its three outcomes are the ones R4 already fixes: `200` rotates and pushes `expires_at` another
  90 days out (FR-025), `400`/`401` clears storage and publishes `LoggedOut` (FR-012, and this is
  how a device superseded by a second-device login finds out — FR-030), and no network, a
  timeout, a `5xx`, or a `429` leaves the stored session exactly as it was (SC-014).

**Rationale**: every endpoint in this feature is the unauthenticated front door, so nothing else
reaches the server once a user is logged in. Without a deliberate renewal, FR-025's sliding window
never slides: every session would expire 90 days after login no matter how much the app was
used, SC-009 would be false for real users, and SC-013 would describe an event that never happens.
The cheapest honest fix is to make the launch itself the exchange, reusing the seam that already
exists rather than inventing a heartbeat.

**Alternatives considered**: a dedicated authenticated `GET /v1/auth/session` — cleaner as a
liveness probe and the natural home for a future profile read, but it adds a DTO, a route, a
service path, and their tests to a feature whose scope is one screen, and it buys nothing the
refresh call does not already provide today. It becomes the better answer the moment a second
feature needs an authenticated read; at that point this renewal simply stops being the only one.
Renewing on every launch regardless of expiry — rejected as needless traffic and needless rotation.
A background periodic worker — rejected: it needs platform schedulers on both sides for a problem
one launch-time call solves.

**Consequence to state plainly**: a user who never opens the app with a network for 90 days still
loses the session. That is what the spec chose (FR-025), and this decision does not change it.

## R16. Bounding an attempt that never returns

**Decision**: `LoginWithGoogleUseCase` wraps the provider call in `withTimeoutOrNull(60.seconds)`
and reports the expiry as `LoginOutcome.Cancelled`.

- Cancellation, not failure: SC-007 says an attempt that concludes without either a success or an
  explicit failure resolves to the idle screen, never to a failure message. An expired attempt is
  exactly that case, so it takes FR-013's silent path.
- The bound sits in the use case rather than in the view model, so it holds for both platforms and
  both Android paths at once, and so a view model test does not have to own a timer.
- `withTimeoutOrNull` cancels the provider coroutine, which is what makes the Credential Manager
  or AppAuth call actually stop rather than leak.

**Rationale**: FR-013 forbids leaving an attempt showing progress indefinitely and SC-007 puts a
number on it, but nothing enforced either. Process death and screen recreation happen to cover the
"left the app and came back much later" case; they do nothing for an attempt that hangs while the
user is watching. One timeout covers both, and virtual time makes it a fast, deterministic test.

**Alternatives considered**: resetting state on lifecycle resume — covers only the leave-and-return
case and leaves SC-007's 60 seconds unmeasured. Dropping the number from SC-007 — rejected: it is
the only bound the spec puts on the screen's worst behavior.
