# Research: Login Screen

**Branch**: `feature/001-login-screen` | **Feature ID**: `001-login-screen` | **Refreshed**: 2026-08-15

The decisions behind the plan, as implemented. Superseded reasoning is dropped rather than kept as
archaeology; what remains is what a reader needs to change this code safely.

**Design source** throughout: Claude Design project `0c49e08b-d7ab-4cd3-88be-8483024790e5`, file
`screen_login.dc.html`. Its `themeVals()` block holds the exact light and dark palette; its markup
holds the sheet chrome, the provider marks, and the snackbar. Values are lifted, not invented.

## R1. Google login on Android

**Decision**: Credential Manager with `GetSignInWithGoogleOption`, wrapped by
`AndroidGoogleCredentialProvider` in `feature-auth/impl/src/androidMain`, falling back to R14's
browser flow when no credential provider is present.

- `androidx.credentials:credentials` + `:credentials-play-services-auth`, plus
  `com.google.android.libraries.identity.googleid` for the option and the credential type.
- Build the option with the **web** client ID as `serverClientId` plus a nonce, call
  `CredentialManager.getCredential(request, context)`, and read `idToken` when the credential type
  matches.
- The call needs an **Activity** context. `MainActivity` publishes itself through an
  `ActivityProvider` in `core-common`'s `androidMain`, nulled outside the resumed lifecycle so no
  Activity leaks.

**Rationale**: `GetSignInWithGoogleOption` is the button-driven flow the design specifies.
`GetGoogleIdOption` with `filterByAuthorizedAccounts` would fire before the user chose anything.

**Alternatives**: the legacy `GoogleSignInClient` is deprecated; a prebuilt KMP wrapper would own
the very boundary the guides put in platform source sets and place a third-party type in the
feature's public surface.

**Cancellation vs fallback**: `GetCredentialCancellationException` is a silent cancellation
(FR-029). Only `GetCredentialProviderConfigurationException` and `NoCredentialException` — the
device has no Google credential provider — trigger the browser flow. A cancellation must never
trigger it: the user said no, and reopening the question in a browser is the opposite of FR-029.

## R2. Google login on iOS

**Decision**: the Xcode host pins GoogleSignIn 9.1.0 through Swift Package Manager and implements
`IosGoogleSignInBridge` from `shared-app`. The bridge accepts the attempt nonce and returns only a
nullable ID-token string: `nil` is dismissal, while SDK failures remain failures. An internal
`IosSdkGoogleCredentialProvider` in `feature-auth/impl/src/iosMain` maps that result into the
feature's private credential hierarchy.

- `GIDConfiguration` receives both the iOS client ID and the web server client ID, so the returned
  ID token is minted for the audience the backend accepts.
- The custom nonce API binds that token to the repository attempt the server verifies.
- GoogleSignIn owns browser presentation, PKCE, token exchange, saved account state, and the
  supported path to App Check. The host forwards its reversed-client-ID URL scheme through
  `onOpenURL`.
- No GoogleSignIn type crosses the Kotlin framework boundary, and no auth declaration returns to
  `feature-auth/api`.

**Rationale**: this keeps the corrected feature boundary while leaving OAuth protocol maintenance
to Google's SDK. A bridge in the composition host is the honest platform boundary: it contains no
repository or domain decision, only SDK adaptation.

**Consequence to state plainly**: Gradle tests the internal adapter and framework export, but only
the Xcode build and a simulator/device run prove the Swift SDK call and presentation.

## R3. Session storage on device

**Decision**: one `SessionStorage` `expect`/`actual` factory in `feature-auth/impl`. Android: an
AES-GCM key in the Android Keystore encrypts the token values, ciphertext in DataStore Preferences.
iOS: Keychain (`kSecClassGenericPassword`) with `kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly`.

**Rationale**: the refresh token is a long-lived credential and belongs behind hardware-backed
protection. `androidx.security:security-crypto` is no longer developed; generating the key directly
gives the same protection with a maintained storage API and a coroutine interface.

**Alternatives**: a KMP secure-storage library — the `expect`/`actual` pair is ~50 lines per
platform and the guides already require platform adapters in platform source sets. Plain unencrypted
DataStore — not sufficient protection for a refresh token on a rooted device.

## R4. Keeping the network layer authenticated

**Decision**: `feature-auth/impl` provides the `AccessTokenProvider` that `coreNetworkModule` looks
up with `getOrNull<AccessTokenProvider>()`.

- `getAccessToken(rejectedAccessToken = null)` returns the stored access token.
- `getAccessToken(rejectedAccessToken = <token>)` rotates: it serializes concurrent callers with a
  mutex, returns a newer token immediately if one arrived while waiting, and calls
  `POST /v1/auth/refresh` otherwise.
- **Only an explicit rejection signs the user out**: `ApiError.Rejected` or `ApiError.Unauthorized`
  clears storage and publishes `LoggedOut` (FR-025). `ApiError.Unavailable` — no network, a
  timeout, a `5xx`, a `429` — leaves the stored session untouched (SC-014). `ApiError.Malformed`
  likewise: an undecodable success is not an answer about the session.
- Koin ordering matters: `AccessTokenProvider` must be declared before `NetworkClient` is first
  resolved, so `appModules` lists `featureAuthModule()` ahead of `coreNetworkModule()`.

**Rationale**: concentrating the sign-out decision here is what makes FR-025's "unreachable is not
invalid" rule provable in one place instead of at every call site.

## R5. Verifying the Google ID token on the server

**Decision**: `GoogleIdentityVerifier` in `services/server/feature-auth/identity`, on
`com.auth0:java-jwt` plus `com.auth0:jwks-rsa`.

- Keys from `https://www.googleapis.com/oauth2/v3/certs` through a caching, rate-limited
  `JwkProvider`; never fetched per request.
- Verify RS256 against the matching `kid`; `iss` is `accounts.google.com` or its https form; `aud`
  is one of the configured client IDs; `exp` is in the future; `nonce` equals the submitted one.
- Read `sub` as the stable identifier, and `email`, `name`, `picture` as non-authoritative profile
  data (FR-021) — any may be absent and is stored as null. `sub` may not: a token that verifies
  without it is refused exactly like an unverifiable one, because it cannot be matched on a later
  login (FR-019).
- The `GoogleAuthConfig` **type** belongs to `feature-auth`; reading the three client IDs stays with
  `core-config`, the constitution's owner of environment loading.

**Rationale**: FR-017 requires the server to establish genuineness for itself — a request can reach
the endpoint without passing through the app. Local signature verification avoids a round trip to
`tokeninfo` on every login.

**On the nonce**: the client generates it, so it is defense in depth rather than challenge-response.
`TokenService.createChallenge` in `core-security` is the upgrade path if the threat model demands
one.

## R6. Account resolution and session persistence on the server

**Decision**: three tables owned by `services/server/feature-auth` with a forward-only Flyway
migration — see [data-model.md](data-model.md).

- `unique (provider, provider_user_id)` — not application code — is what makes FR-019 hold under
  concurrent first logins.
- Descriptive fields are nullable, unindexed, and never queried, which is the mechanism behind
  FR-020. They come from the already-verified token, so no userinfo call is needed.
- Refresh tokens are stored only as SHA-256 hashes via `TokenService.hash`.
- Rotation replaces the stored hash inside the transaction that validates the old one and pushes
  `expires_at` forward by `REFRESH_TOKEN_TTL_SECONDS`, raised from 30 to **90 days**. **A successful
  rotation is the only thing that renews a session** (FR-026).

**Testing**: `docs/testing/003-backend-integration.md` requires the uniqueness constraint, the
rotation race, and the migration bootstrap to be proven against real PostgreSQL, pinned to **17** in
both the Testcontainers image and the deployment notes.

## R7. Reduced motion and platform detection

**Decision**: two platform capability ports in `core-common`. `Platform` — `ANDROID` or `IOS`, from
an `expect fun currentPlatform()` — feeds the roster, not the UI (FR-004).
`MotionPreferences.isReduced()` reads `Settings.Global.ANIMATOR_DURATION_SCALE == 0f` on Android and
`UIAccessibility.isReduceMotionEnabled` on iOS (FR-045).

`MotionPreferences` is a contract in `commonMain` with a narrow adapter per platform, not an
`expect class`: the Android reading needs a `Context` and the iOS one does not, and R10 puts
FR-045's proof at the mapper boundary, which requires handing the mapper a chosen answer. The same
reasoning makes `SessionStorage`'s `expect`/`actual` the factory rather than the type.

`MotionPreferences` deliberately does **not** reach the selection slice: the sheet keeps the
platform's standard animation (R24).

## R8. Navigation after login

**Decision**: conditional navigation owned by `app-root`. `RootBackStack` observes
`ObserveAuthSessionStateUseCase` and holds either `AuthNavKey.Login` or `RootNavKey.Main` as the
back stack **base**. No view model pushes a post-login destination.

Each root is composed by the module that owns it — `featureAuthModule()` registers the auth
destinations, `appRootModule()` registers `RootNavKey.Main` — so `koinEntryProvider()` resolves
both.

**Rationale**: `feature-auth` cannot reference a main-screen key no feature owns yet, and `app-root`
is the only module that sees both. FR-025 falls out of the same observation rather than needing
separate handling. R22 extends the base with a mutable tail.

## R9. Screen composition

**Decision**: one `LoginViewModel` with nested `DataState`, `UiState`, `News`, `Event`, a
`LoginUiStateMapper`, a `LoginNewsMapper`, and the screen's composables in `presentation/login/ui`.

- The transient message is one-shot and travels as `News.ShowMessage`.
- Marquee, rolling topic, spinner, and press feedback are Compose-owned animation, gated by
  `UiState.isMotionReduced`.
- Copy lives in the feature's `composeResources`; `UiState` carries `StringResource` references,
  never resolved strings.
- The legal line (FR-051) sits below the caption; its links open through `LocalUriHandler`, which
  exists in `commonMain`, so no platform port is needed. The two destinations arrive as
  `featureAuthModule(...)` parameters and reach the screen through `GetLegalLinksUseCase`.
- The screen is a three-part split, not one stacked column: the tilted band at the top, the action
  block at the bottom, the hero in what is left. It is a scrolling column with a minimum height of
  one viewport and `SpaceBetween`, so the split holds at ordinary sizes and gives way to scrolling
  at 320 dp and 200% font scale. Below 340 dp the two display sizes and the horizontal padding step
  down, matching the design's own container query.

## R10. Verification strategy

| Behaviour | Where it is proven |
| --- | --- |
| Which providers exist, their visibility and selectability per platform | Roster use-case tests |
| Provider rows, marks, `isMonochrome`, hidden providers dropped | Selection mapper tests |
| Reduced motion and loading state | `LoginUiStateMapper` tests |
| Outcome → message | `LoginNewsMapper` tests |
| Event → state, duplicate-tap guard, cancellation silence, navigation intent | View-model tests through `runViewModelTest` |
| Registry lookup, the unavailable outcome, the 60-second bound | `DefaultLoginUseCase` tests on virtual time |
| Session resolution once, refresh margin, login outcome mapping | `DefaultAuthSessionRepository` / `DefaultGoogleAuthRepository` tests over stubs |
| Rotation, concurrent rotation, and which failures sign the user out | `DefaultAccessTokenProviderTest`, one case per `ApiError` |
| Status and transport → `ApiResult`, bearer attachment, base-url joining | `ApiClientTest` over `ktor-client-mock` |
| Session DTO → local → domain, including the `sub` claim decode | `SessionMapperTest` |
| Koin graph completeness, every selectable provider resolving a handler | `verify()` plus real resolution in the module that owns each graph |
| Google verification: bad signature, wrong `aud`/`iss`, expired, nonce mismatch, absent `sub` — none creates a row | `GoogleIdentityVerifier` tests plus a route test asserting no rows are written |
| Fallback triggers on a missing provider, never on a cancellation | `AndroidGoogleCredentialProvider` tests over a stubbed Credential Manager |
| Route parsing, serialization, and the `AuthFailure` each route raises | Ktor `testApplication` inside `feature-auth` |
| `AuthFailure` → `400`/`401`/`503`, and `429` over the limit | `app`'s own tests — no feature may depend on `app` |
| Session state stays `Unknown` until storage is read, then resolves once | `SessionStore`/repository tests; the absence of a visible flash is checked by hand |
| The launch refresh runs once, only after the local decision | `LaunchSessionRefreshTest` over stubs |
| Back stack: push, pop, tail reset on base change, no duplicate top | `RootBackStackTest` |
| Unique identity under concurrent first login, rotation race, sliding `expires_at`, migration bootstrap | Testcontainers PostgreSQL 17 |
| Rendering, content order, semantics, themes, narrow width, large font scale, snackbar timing and motion | Compose UI tests — only for what a mapper test cannot prove |

**Rationale**: `docs/testing/001-structure.md` requires each behaviour to be tested once at the
boundary that owns it, and `docs/testing/003` forbids substituting a fake for PostgreSQL semantics.

## R11. New dependencies

| Coordinate | Used by | Why |
| --- | --- | --- |
| `androidx.credentials:credentials`, `:credentials-play-services-auth` | `feature-auth/impl` androidMain | Credential Manager (R1) |
| `com.google.android.libraries.identity.googleid:googleid` | `feature-auth/impl` androidMain | the sign-in option and credential type (R1) |
| `net.openid:appauth` | `feature-auth/impl` androidMain | PKCE + Custom Tabs fallback (R14) |
| `androidx.datastore:datastore-preferences` | `feature-auth/impl` androidMain | encrypted session container (R3) |
| `com.auth0:jwks-rsa` | `services/server/feature-auth` | Google JWKS with caching (R5) |
| `androidx.core:core-splashscreen` | `apps/mobile/android-app` | splash held until session state resolves (R12) |
| `io.ktor:ktor-server-status-pages`, `:ktor-server-rate-limit` | `services/server/app` | failure mapping and per-IP limiting (R13) |
| `io.ktor:ktor-client-mock` | `core-network` tests | `ApiClient` behaviour without a server |
| `io.ktor:ktor-server-test-host`, `org.testcontainers:postgresql` | server tests | routes and PostgreSQL integration |
| `GoogleSignIn` 9.1.0 (SwiftPM) | `ios-app` | official iOS sign-in, nonce, token exchange, App Check path (R2) |

Each is pinned exactly in `gradle/libs.versions.toml` — no ranges, no `+`, no alpha unless nothing
stable exists. The one deliberate exception is Navigation 3 (R19). The SwiftPM dependency is pinned
exactly in `YapApp.xcodeproj` rather than the Gradle catalogue (R2).

## R12. Holding the launch screen until session state is known

**Decision**: each platform's own splash mechanism, kept up until the first non-`Unknown`
`AuthSessionState` (FR-002). Android uses `androidx.core:core-splashscreen` with a keep-on-screen
condition installed before `setContent`; iOS uses the storyboard launch screen, with `App()`
composing no `NavDisplay` while the back stack is empty.

`AuthSessionState` therefore starts as `Unknown`, not `LoggedOut`: the latter would push the login
screen and then replace it, which is exactly the flash FR-002 forbids.

**Alternatives**: an in-app splash composable — it would appear *after* the system splash, adding a
second flash. Blocking the main thread on the storage read — rejected outright.

## R13. Rate limiting the auth endpoints

**Decision**: Ktor's `RateLimit` plugin in `services/server/app`, applied per originating IP to all
three unauthenticated endpoints, at **100 requests per minute**, read from
`AUTH_RATE_LIMIT_REQUESTS_PER_MINUTE`.

- All three doors are limited. Leaving `/v1/auth/google/code` open would put the whole limit behind
  a door an attacker can walk around.
- The client IP comes from `ApplicationRequest.origin`, which honours forwarded headers when
  `TRUST_PROXY_HEADERS` is on. Behind a proxy with that flag off, every request appears to come from
  the proxy and the limit throttles everyone.
- Why 100/min clears both bars in SC-010: a person retrying makes a handful of attempts a minute,
  and a shared address pools devices that each log in about once every 90 days. Automated abuse hits
  it immediately.
- **A `429` on refresh must not sign the user out** — it is an answer about the caller's address,
  not the session, so it maps to `ApiError.Unavailable` (FR-025, SC-014).

**Known limitations**: a shared outbound IP pools many real users behind one counter, which is why
the threshold is generous; and the counter is per instance, so a second server doubles the effective
limit.

## R14. Browser fallback for devices without Google's services

**Decision**: OAuth 2.0 authorization code flow with PKCE in **Chrome Custom Tabs** via
`net.openid:appauth`, used on Android when and only when Credential Manager reports that no provider
exists (FR-016).

- AppAuth generates the verifier/challenge pair, launches the tab, and catches the redirect — it is
  the reference implementation of RFC 8252.
- Redirect URI is the reversed client ID scheme, registered as an intent filter in `android-app`.
- The client never exchanges the code; it hands `code` + `codeVerifier` + `redirectUri` to
  `POST /v1/auth/google/code`. Installed-app clients have no secret, so PKCE is the whole proof.
- Scopes: `openid email profile`, matching the claims FR-021 stores.

**Why not a WebView**: Google returns `disallowed_useragent` for OAuth inside embedded web views,
and an embedded view breaks the property that makes the flow safe — the user cannot see the address
bar. FR-016 forbids it outright.

**Why iOS differs**: GoogleSignIn owns its browser and token exchange (R2), so iOS has nothing to
fall back *from* and never calls this endpoint.

**Known limitation**: a device with no Google provider *and* no capable browser cannot log in. That
is a genuine dead end, reported through the ordinary failure path (FR-030); T-ID is what eventually
removes it, not more fallbacks.

## R15. Refreshing the session at launch

**Decision**: `app-root`'s `LaunchSessionRefresh` awaits the first resolved session state and, when
it is `LoggedIn`, calls `RefreshSessionUseCase`. `DefaultAuthSessionRepository` decides whether to
act: it refreshes when the stored access token is expired or **within five minutes** of expiring,
and delegates the call to the `AccessTokenProvider` of R4, which already owns rotation and the
sign-out decision — no new endpoint, no new DTO, no new failure handling.

- The condition lives in `impl`, not in `app-root`: the expiry fields are `internal`, and a decision
  made in `app-root` would either widen that visibility or degrade into an unconditional refresh on
  every launch.
- Five minutes against the 900-second access-token TTL is a third of the window: far enough that a
  slow launch still finishes the rotation, near enough that a healthy token is left alone.
- It runs **after** the launch decision, never before it — FR-024 forbids a request before the app
  decides where to land, and an offline launch must still reach the main screen.
- Its three outcomes are the ones R4 fixes: rotation pushes `expires_at` another 90 days out
  (FR-026); an explicit rejection clears storage and returns the user to login (FR-025, and this is
  how a device superseded by a second-device login finds out — FR-027); anything unreachable leaves
  the stored session exactly as it was (SC-014).

**Rationale**: every endpoint here is the unauthenticated front door, so nothing else reaches the
server once a user is logged in. Without this, FR-026's window never slides and SC-013 describes an
event that never happens.

**Alternatives**: a dedicated authenticated `GET /v1/auth/session` — cleaner as a liveness probe,
but it adds a DTO, a route, a service path, and their tests for what refresh already provides.
Refreshing on every launch — needless traffic and needless rotation. A background worker — platform
schedulers on both sides for a problem one launch-time call solves.

**Consequence to state plainly**: a user who never opens the app with a network for 90 days still
loses the session. That is what the spec chose.

## R16. Bounding an attempt that never returns

**Decision**: `DefaultLoginUseCase` wraps the provider call in `withTimeoutOrNull(60.seconds)` and
reports expiry as `LoginOutcome.Cancelled`.

- Cancellation, not failure: SC-007 says an attempt concluding without a success or an explicit
  failure resolves to the idle screen.
- The bound sits in the use case, so it holds for every provider and both Android paths at once, and
  a view-model test does not have to own a timer.
- `withTimeoutOrNull` cancels the provider coroutine, so the Credential Manager or AppAuth call
  actually stops rather than leaks.

## R17. The shape of `AuthProvider`

**Decision**: one data class carrying identity plus both facts:

```kotlin
enum class AuthProviderType { APPLE, GOOGLE, T_ID }

data class AuthProvider(val type: AuthProviderType, val isEnabled: Boolean, val isVisible: Boolean)
```

**Rationale**: both facts sit on the provider, and because they are *instance* values rather than
constants, the roster sets them per device today and a backend can set them later (FR-006).
`Flow<List<AuthProvider>>` then reads literally. Identity is a plain enum, which is what makes the
display table (`AuthProviderUiMapper`) an exhaustive `when` and the handler registry a map keyed by
type rather than by class.

**Alternatives**: an enum carrying the two flags, as originally asked — rejected, because a constant
cannot be driven per device or by a backend, and `APPLE` would then appear on Android. A sealed
hierarchy of per-provider data classes — it works, but it buys nothing over one data class plus an
enum and forces every consumer into `is` branches and `KClass` keys.

## R18. Registering a login path per provider

**Decision**: a domain port carrying its own identity:

```kotlin
internal interface ProviderLogin {
    val type: AuthProviderType
    suspend fun login(): LoginOutcome
}
```

Each implementation is bound by its own type and `bind ProviderLogin::class`. `DefaultLoginUseCase`
receives them through Koin's `getAll<ProviderLogin>()` and indexes them by `type`. Adding a provider
is one Koin declaration plus one implementation.

**Rationale**: `getAll<T>()` is public Koin API — `koin-compose-navigation3` uses it to collect
entry providers — so this is the same collection mechanism the navigation DSL relies on. The map is
built where the handlers are declared, not in a view model.

**Alternatives**: a `Map<provider, LoginUseCase>` injected into the view model — every new provider
would edit one central `mapOf`, which is what the refactor removed. Qualified bindings resolved on
demand — failures would surface at tap time instead of at wiring-verification time.

**Guard**: a wiring test asserts that every provider the roster marks selectable resolves a handler,
and that no two handlers claim the same type.

## R19. Navigation 3 upgrade

**Decision**: raise Navigation 3 so the official result API is available.

| Artefact | Before | After |
| --- | --- | --- |
| `androidx.navigation3:navigation3-runtime` | 1.1.1 | **1.2.0-alpha04** |
| `org.jetbrains.androidx.navigation3:navigation3-ui` | 1.1.1 | **1.2.0-alpha02** |

The two versions differ on purpose: JetBrains publishes only the UI artefact, and `navigation3-ui`
1.2.0-alpha02 declares runtime 1.2.0-alpha04. Pinning the runtime to what the UI was built against
is more honest than letting conflict resolution pick it.

**Why an alpha**: `androidx.navigation3.runtime.result` exists in no stable release. The requester
chose the official mechanism over a hand-rolled one, and this is its cost — the one deliberate
exception to R11.

**Verified unchanged across the bump**: the runtime's file set apart from the added `result`
package; `NavEntry.metadata` still `Map<String, Any>`; `EntryProviderScope.entry(content, metadata)`
still present, which is what `koin-compose-navigation3` compiles against; the `NavDisplay` signature,
`OverlayScene.onRemove()`, and `SceneStrategyScope.onBack`.

**New transitive dependency**: `navigationevent-compose` for predictive back. `navigation3-ui` also
declares lifecycle 2.10.0, which resolves up to the project's pinned 2.11.0.

## R20. Returning the chosen provider

**Decision**: use `androidx.navigation3.runtime.result`. No feature-owned channel, no extra use
cases. `App()` adds `rememberResultEventBusNavEntryDecorator<NavKey>()`; the selection screen sends
through `LocalResultEventBus.current.sendResult(...)` and then navigates back; the login screen
receives with `ResultEffect<AuthProvider>(resultKey = …)`, which raises an ordinary `Event`.

**The keying decision**: the bus keys results by type name unless an explicit `resultKey` is given,
so both sides name one shared constant — `AuthResultKeys.PROVIDER_SELECTION` — and the send and the
receive cannot drift apart.

**Alternatives**: `ResultEventBus.conflateAsState` — a login is a one-shot action, and a conflated
state would replay the last choice on recomposition.

## R21. The selection screen as a destination

**Decision**: `AuthNavKey.SelectAuthProvider` is a real destination rendered by a custom
`BottomSheetSceneStrategy`, following `AnimatedBottomSheetSample` in the `navigation3-ui` samples.

**Rationale**: Navigation 3 ships no bottom-sheet scene strategy at any version. The sample's
`onRemove()` override is the piece that matters — it awaits `sheetState.hide()` before the entry
leaves composition, so the sheet animates out instead of vanishing. `metadata { … }` builds a
`Map<String, Any>`, which is exactly what Koin's `Module.navigation<T>(metadata, …)` takes, so the
marker is attached from the feature's own module with no DSL change.

**Alternatives**: keeping the sheet as a composable driven by a `UiState` flag — rejected twice
over: the requester asked for a full destination, and `docs/mobile/presentation/002-ui-compose.md`
forbids a sheet visibility flag in `UiState`.

## R22. A mutable back stack, a `Navigator`, and the roster

**Decision**: `RootBackStack` is a `single` owning a session-derived base (R8) plus a mutable tail.
It implements the `Navigator` contract in `core-common`, bound in `app-root`. `App()` passes
`onBack`, the scene strategies, and the decorator list to `NavDisplay`.

The roster — `DefaultObserveAuthProvidersUseCase` — is a cold `Flow<List<AuthProvider>>` computed
from `Platform` alone: Google, Apple, T-ID in display order; Apple visible on iOS only; only Google
selectable. There is deliberately **no repository behind it yet**; the `Flow` is the seam a remote
roster slots into, and a port with a single in-memory implementation would own no behaviour.

**Rationale**: `docs/mobile/presentation/001-view-models.md` prescribes `Navigator` and forbids
navigation through `News`, but no module provided one. This is the first feature that needs it,
which is when the constitution allows the abstraction to appear.
`docs/mobile/003-dependency-injection.md` requires the back stack to stay in `app-root` and never be
passed to a feature, so the feature sees only the contract.

**Alternatives**: `rememberNavBackStack` in `App()` — the base is derived from session state inside
a Koin-owned component, and a composable-owned stack cannot be reset from there without duplicating
that logic.

**Known limitation, stated rather than hidden**: the singleton survives configuration change but not
process death, so a sheet open at process death reopens as the login screen. Acceptable for a
transient chooser.

## R23. Colours, lifted from the design

**Decision**: `core-design` owns `YapColors`, provided through `LocalYapColors`, with a Material
`ColorScheme` derived from the same values. The login slice reads `YapTheme.colors` and declares no
literal.

| Role | Light | Dark | Design key |
| --- | --- | --- | --- |
| `background` / `onBackground` | `FFFEF7` / `0B0A0D` | `08070A` / `FAF9F6` | `bg` / `fg` |
| `surface` / `onSurface` | `FFFEF7` / `0B0A0D` | `15141A` / `FAF9F6` | `sheetBg` / `providerColor` |
| `accent` | `5E3689` | `D9FF57` | `topicColor`, also `providerHover` |
| `action` / `onAction` | `0B0A0D` / `FFFAFC` | `D9FF57` / `0B0A0D` | `buttonBg` / `buttonFg` |
| `bodyMuted` | `5F5A6B` | `8F8899` | `bodyColor` |
| `caption` | `8B8496` | `5B5765` | `captionColor` |
| `sectionLabel` | `8B8496` | `7C7787` | `sheetLabelColor` |
| `link` | `0B0A0D` | `FAF9F6` | anchor colour |
| `notice` / `onNotice` | `5E3689` / `FFFAFC` | **`5E3689` / `FFFAFC`** | `snackBg` / `snackFg` |
| `highlight` / `onHighlight` | `D9FF57` / `0B0A0D` | `D9FF57` / `0B0A0D` | marquee band |
| `outline` | `0B0A0D` at 10% | `E2E2E2` at 14% | `sheetBorder` |
| `handle` | `0B0A0D` at 20% | `E2E2E2` at 25% | `sheetHandle` |
| `scrim` | `3C3742` at 35% | `050406` at 55% | `sheetOverlay` |

`notice` is the one role fixed across themes by requirement (FR-044, SC-018) — the design has always
specified one snackbar colour. The band keeps one pair in both themes because it is a printed
sticker over the screen, not a surface that follows it. `link` is the only role with no counterpart
in the prototype, since the design carries no legal line, so it follows `onBackground`.

**Rationale**: the palette does not fit Material's slot set — the primary action is near-black in
light while `accent` is purple, the snackbar is fixed across themes, and the marquee band has no
slot at all. Keeping `YapColors` as the source of truth and deriving the `ColorScheme` from it gives
`ModalBottomSheet`, `Snackbar`, and `Button` correct defaults without inventing meanings for slots.

## R24. Snackbar mechanism, motion, and timing

**Decision**: `SnackbarHostState` owns queueing, one-at-a-time display, and dismissal; a
feature-owned host renders it with vertical entry and upward exit.

**Rationale**: `SnackbarHostState`'s mutex is a documented fair queue and `showSnackbar` suspends
until the current message disappears. What it cannot provide is the motion: `SnackbarHost` renders
through a private `FadeInFadeOutWithScale` carrying the upstream comment *"TODO: to be replaced with
the public customizable implementation"*, with no parameter to change it. Owning ~25 lines of host
is the smallest way to get the required motion while keeping the standard state machine. The screen
therefore shows every message with `SnackbarDuration.Indefinite` and lets the host own the timer.

**Values from the design**: top of the screen, 10 dp below the safe area, 20 dp side margins; 14 dp
corner radius; 12 dp × 18 dp padding; 14 sp weight 600 centred; enter 220 ms ease-out from 8 dp
below with fade; exit upward with fade; duration **2600 ms**.

**Reduced motion**: the host swaps both transitions for `EnterTransition.None` /
`ExitTransition.None`, so the message still displays for its full duration without motion. The
preference governs the message only — the selection sheet keeps `ModalBottomSheet`'s standard
animation (FR-045).

## R25. Provider marks, lifted from the design

**Decision**: three vector drawables in `feature-auth/impl/src/commonMain/composeResources/drawable/`,
converted verbatim from the design's markup.

| Provider | Mark | Size | Tint |
| --- | --- | --- | --- |
| Google | four-path multicolour "G" — `#4285F4`, `#34A853`, `#FBBC05`, `#EA4335` | 20 dp | none |
| Apple | single-path logo, drawn in the row's content colour | 19 dp | `onSurface` |
| T-ID | 20 dp rounded square, 6 dp radius, `#FFDD2D`, with a dark "T" | 20 dp | none |

**Consequence for the row model**: the marks are not uniform, so the row carries `isMonochrome`, set
by the display table. That is a fact about the asset, not a theme value, and the composable branches
on the flag rather than on the provider. Brand colours live inside the drawables, never in
`YapColors` — the theme describes the product's roles, not other companies' palettes.

## R26. The data layer, as reviewed and delivered

The auth data layer was reviewed in full and simplified; this records what the review settled, so
the questions are not reopened by accident.

| Question | Answer |
| --- | --- |
| One repository or two? | **Two.** `AuthSessionRepository` owns the session lifecycle; `GoogleAuthRepository` owns one provider's login path. A provider-named method on a shared repository re-states, at the wrong layer, a decision `DefaultLoginUseCase` already made, and a second provider would either add a near-identical method or reuse a misnamed one (FR-063) |
| Where does session state live? | **`SessionStore`.** One owner of the stored record and the only publisher of `AuthSessionState`, so "who decides the session state" is answerable from one file. The repository observes it; the token provider writes through it |
| The "read storage once" guard | A mutex plus a resolved flag inside `SessionStore`, entered only by `resolveOnce()`. `observe()` deliberately emits the current value **before** resolving, so the first emission is `Unknown` and the root renders nothing until the second — which is what FR-002 depends on, and it is asserted by test |
| `CurrentTime` port | **Kept.** Its purpose is testability of expiry and it is genuinely substituted; `Clock.System` is not injectable otherwise, and expiry arithmetic must be testable without wall-clock sleeps |
| `SessionLocal` vs `SessionDto`, field-identical | **Kept apart.** Principle IV: a DTO is not a storage model. Collapsing them would make the on-device format a function of the wire format, so a server field rename would silently invalidate every stored session on every device. The four-line mapper is the price of the boundary |
| Feature-owned HTTP failure translation | **Removed.** It moved into `core-network` as `ApiResult`/`ApiError`, so every feature gets the same four outcomes and FR-025's sign-out rule is expressible once (FR-064) |
| `Lazy<AuthRemoteDataSource>` | **Kept.** The cycle is real; breaking it inside `coreNetworkModule` would move a feature's wiring problem into a `core-*` module for no behavioural gain. Recorded in plan.md's Complexity Tracking rather than left implicit |
| `NonceGenerator` port | **Kept.** One implementation today, but it is the substitution seam the repository test uses, and a platform secure-random strategy is a plausible second |

Out of scope of the review, deliberately: `SessionStorage` and its two platform implementations
(the Keystore path is not something to refactor without a device test), and the Android credential
adapters, whose fallback chain is real behaviour with its own tests.

## R27. Where each behaviour lives

| Behaviour | Owner |
| --- | --- |
| Which providers are shown, per platform | the roster (`DefaultObserveAuthProvidersUseCase`) |
| Which providers can be chosen | the roster; enforced at login time by `DefaultLoginUseCase` |
| Provider label, mark, and row tag | `AuthProviderUiMapper`, one table (FR-038) |
| Opening the provider list | `Navigator.navigate(AuthNavKey.SelectAuthProvider)` |
| The login call | one `LoginUseCase(provider)` over `getAll<ProviderLogin>()` |
| The 60-second bound | `DefaultLoginUseCase` |
| Outcome → message | `LoginNewsMapper` |
| Session state and the stored record | `SessionStore` |
| The refresh margin | `DefaultAuthSessionRepository` |
| Rotation and the sign-out decision | `DefaultAccessTokenProvider` |
| Status and transport → outcome | `core-network`'s `ApiClient` |
| The palette | `YapColors` in `core-design` |
| Message timing and motion | `LoginSnackbarHost` |

## R28. The unavailable-provider path

**Decision**: exactly the design's flow. Every visible row is tappable; choosing one closes the
sheet and returns the provider as a navigation result; `LoginViewModel` calls `loginUseCase(provider)`
and gets `Unavailable` back for a provider with no login path or one the roster marks not
selectable; the message appears on the login screen. The selection screen only reports the choice —
it never calls a use case and never decides an outcome.

**The copy problem and its resolution**: the design's wording is per provider — *"Вход через Apple
скоро появится"*. Branching over providers inside `LoginViewModel` would break FR-011. Instead the
wording is one parameterised string, `login_provider_soon` = *"Вход через %1$s скоро появится"*,
whose argument is the provider's label from the one display table. `News.ShowMessage` carries an
optional `argument: StringResource?`, and the screen resolves the nested resource before showing it.

**Alternatives**: a generic message — the design specifies the wording, and matching it cost one
optional field. Keeping the sheet open and showing the message from its own mapper — rejected by the
requester: the sheet closes on selection, as the design stages it. Per-provider message resources —
three strings differing only by a name, growing with every provider.

## R29. Source layout and hygiene

**Decision**: rendering code lives in `app.yap.feature.auth.presentation.<slice>.ui`, nested under
each presentation slice and mirrored in `commonMain`, `commonTest`, and `androidHostTest`. A file
belongs there when it draws or styles the screen, or names a drawn element; view models and mappers
stay one level up. What both slices share sits in `presentation/common`.

**Rationale**: nesting keeps the slice's identity above the layer split, so a second slice repeats
the pattern rather than competing for a shared `ui` namespace. The split matches the pipeline
`docs/mobile/presentation/002-ui-compose.md` states: domain → `DataState` → mapper → `UiState` →
Compose; everything left of the last arrow stays, everything right of it moves. `LoginTestTags` is
the judgement call — no Compose import, but it is a vocabulary of rendered elements, and keeping it
beside the mapper would leave the UI tests importing from two packages for one concern.

**Visibility**: nothing widens. Kotlin's `internal` is module-scoped, not package-scoped, so a
nested package needs no exception (FR-056).

**Comments**: Kotlin sources carry none (FR-058). A survey found no machine-read comment form
anywhere in the repository — suppressions here are the `@Suppress` annotation, which is code — so
FR-058's preservation list is empty in practice. Facts that once lived only in a comment live in
these artefacts or under `docs/`. Detekt has no `comments` ruleset configured, so nothing about the
rule depends on tooling.

**Guides reconciled in the same PR** (FR-062): `docs/mobile/001-feature-boundaries.md` (the nested
`ui` package; the feature's Koin module function is public in `impl`, not `api`),
`docs/mobile/003-dependency-injection.md` (the mutable back-stack tail, the `Navigator` binding,
scene strategies, entry decorators), `docs/001-code-conventions.md` (declaration order: public API
first, then private helpers depth-first below their callers, and in a view model each private holder
directly above the public stream it backs), and `config/detekt/detekt.yml`
(`ReturnCount.excludeGuardClauses`, so the guard-clause convention and the rule agree).

**Test source sets opt in to `kotlinx.coroutines.ExperimentalCoroutinesApi` once**, in
`KmpLibraryPlugin`. `TestScope.runCurrent()` and `advanceUntilIdle()` are still marked experimental
in coroutines 1.11.0, and every view-model and repository test drives virtual time through them; a
per-file `@OptIn` would be repeated in every test written from here on. Production source sets are
untouched, so the marker still means something where it matters.

## R30. Verification set

`./gradlew build` for the repository-wide sweep, plus
`./gradlew :apps:mobile:shared-app:compileKotlinIosSimulatorArm64` for the KMP boundary. Detail in
[quickstart.md](quickstart.md). A change is reported complete only after both pass (FR-066).
