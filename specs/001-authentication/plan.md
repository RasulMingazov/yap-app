# Implementation Plan: Login (001-authentication)

**Specification**: [spec.md](spec.md)

## Design baseline (R-086, AC-053)

Imported from Claude Design project `0c49e08b-d7ab-4cd3-88be-8483024790e5`
("Дизайн чата speak-simple refactor") through the design MCP on 2026-08-09.

| File | Bytes | SHA-256 |
| --- | --- | --- |
| `screen_login.dc.html` | 18348 | `21726b536ea23c7cd02bc78e32658ecd6d801f53e4341f225c984d3b2ed717b0` |
| `support.js` | 69150 | `8fe7df74405f3c55f49b7249c74ea1397e65d07dea2b1bd3b4a489bec2e28cbe` |

Hashes are over the raw UTF-8 bytes of the imported file contents. Re-import the same two
paths from that project and re-hash before any visual verification; a different hash returns
the feature to specification review and invalidates every visual acceptance criterion.

Baseline facts extracted from `screen_login.dc.html` that drive implementation:

- Type scale: hero 44/44/900/-0.05em, topic 40/40/900/-0.05em, body 15/21/400,
  button 17/20/900/-0.025em uppercase, caption 12/16/700, marquee 13/16/900/-0.005em uppercase.
- Layout: content padding 24dp (20dp under 340dp width), section gap 16dp (12dp compact),
  button `min-height` 52dp with fully rounded corners, provider row `min-height` 52dp with a
  12dp icon gap, sheet corner radius 24dp top, sheet handle 36×4dp.
- Dark palette: background `#08070A`, foreground `#FAF9F6`, topic/button `#D9FF57`,
  button label `#0B0A0D`, body `#8F8899`, caption `#5B5765`, sheet `#15141A`,
  sheet border `#E2E2E2` @14%, scrim `#050406` @55%, handle `#E2E2E2` @25%,
  sheet label `#7C7787`, snackbar `#26232C` on `#FAF9F6`.
- Light palette: background `#FFFEF7`, foreground `#0B0A0D`, topic/provider hover `#5E3689`,
  button `#0B0A0D` on `#FFFAFC`, body `#5F5A6B`, caption `#8B8496`, sheet `#FFFEF7`,
  sheet border `#0B0A0D` @10%, scrim `#3C3742` @35%, handle `#0B0A0D` @20%,
  snackbar `#5E3689` on `#FFFAFC`.
- Motion: marquee `translateX(0 → -50%)` linear 15s infinite over two identical text copies
  (this is the seamless loop required by R-004); topic roll 6s
  `cubic-bezier(.7,0,.2,1)` infinite through three positions; snackbar enter 220ms; button
  spinner 700ms linear; `prefers-reduced-motion` disables all `[data-anim]` animation while
  leaving text visible.
- Marquee band: `#D9FF57`, rotated `-2.6deg`, 7dp vertical padding, bleeding 4dp past both edges.
- Loading: the spinner replaces the `ВОЙТИ` label **inside** the same button (20dp circle,
  2.5dp stroke, transparent top edge); the button keeps its size and shape.
- Snackbar: pinned near the top (`left/right` 20dp, `top` 58dp), radius 14dp, padding 12/18dp.

The design prototype is a demo: its provider list is hardcoded, `showApple` is a prop, and every
provider ends in a snackbar. Behavior comes from the specification, not from the prototype script.

## Summary

Build the login slice end to end across three areas, adding no module that does not already exist:

- `shared/contract/auth` gains the wire DTOs for challenge issuance, login, and refresh.
- `services/server/feature-auth` gains challenge, account, provider-identity, and session tables
  with Flyway migrations, Google and Apple verifiers, and one `AuthService` that owns
  the transactional invariants; `services/server/app` gains the process entry point and routing that
  the `ServerApplicationPlugin` already expects at `app.yap.server.MainKt`.
- `apps/mobile/feature-auth` gains the domain ports, secure session storage, session repository,
  use cases, provider adapters, and the `Auth`/`Login`/`SelectProvider` presentation slice;
  `apps/mobile/app-root` owns the auth/authenticated branch and the minimal authenticated screen;
  `apps/mobile/android-app` and `apps/mobile/ios-app` gain real hosts.

Provider variability is expressed once, as injected configuration (`LoginProviderConfig`) plus one
provider-neutral adapter port. Presentation, navigation, domain, wire contracts, and server routes
never branch on a concrete provider (R-012, R-021, R-022, R-025).

Google and Apple are implemented for real. **T-ID is deliberately scaffolded, not implemented**
(R-078, R-103…R-106): the identifier, configuration, disabled row, adapter shells, and unregistered
server-side verification scaffolding exist so the integration point stays clean, while no T-ID SDK
is linked and no T-ID login can succeed. Every T-ID unknown is a deferred integration question, not
a blocker — the application builds, starts, and logs in with Google with no T-ID credentials, SDK,
or configuration present.

There is no logout in this iteration. The authenticated screen has no controls, so no logout DTO,
route, service operation, or use case exists. Clearing the stored session is an internal repository
operation driven only by definitive refresh rejection (R-059).

## Current state

- Every module in the plan already exists in `settings.gradle.kts`; none of them has login behavior.
- `services/server/core-security` already provides `TokenService`/`JwtTokenService` with
  `createChallenge`, `createRefreshToken`, `rotateRefreshToken`, `parseRefreshToken`, `hash`
  (SHA-256 hex), and `issueTokens`. Refresh tokens are `ysr_<sessionId>.<secret>`, so the server can
  locate a session row from the presented value without storing the value itself.
- `services/server/core-config` already provides `AuthConfig` (JWT secret/issuer/audience, access
  TTL default 900s, refresh TTL default 2_592_000s) and `DatabaseConfig`; provider client IDs are
  explicitly documented as feature-owned configuration.
- `services/server/core-database` provides `DatabaseFactory` with Hikari, Flyway
  (`classpath:db/migration`), and Exposed `Database.connect`. `services/server/app` has **no**
  sources at all — no `Main.kt`, no Ktor module, no status-page mapping.
- `apps/mobile/core-network` owns the single Ktor client, `authenticated()`, and
  `installAccessTokenModifier`, which already performs the one-refresh/one-retry cycle on `401`
  (R-055). The single-flight refresh and persistence belong to the `AccessTokenProvider`
  implementation (`apps/mobile/core-common`, interface only today) that this feature supplies from
  `DefaultSessionRepository`.
- `apps/mobile/core-design` has `YapTheme` on default Material 3 color schemes and Inter fonts
  (regular/semibold/bold/black) already bundled as Compose resources.
- `apps/mobile/android-app` has only an `AndroidManifest.xml` with no activity;
  `apps/mobile/ios-app` is an empty `.xcodeproj` placeholder with no Swift sources.
- `apps/mobile/core-test` exposes `stubcall`, `kotlinx-coroutines-test`, and dispatcher helpers.
- **Decompose/Essenty is not in `gradle/libs.versions.toml` and is used nowhere**, although
  `docs/mobile/presentation/*` mandates `Value`, `ComponentContext`, `InstanceKeeper`, `ChildSlot`,
  and `ChildStack`. This feature introduces it.
- No Testcontainers, Compose UI test, secure-storage, or provider-SDK dependency exists yet, and no
  PostgreSQL major version is pinned anywhere. Per
  [`docs/testing/006-backend-integration.md`](../../docs/testing/006-backend-integration.md) this
  change owns defining that shared version: **PostgreSQL 17**.

## Design

### Component hierarchy and event flow (R-087…R-095)

```text
AppRootComponent                       (apps/mobile/app-root)
├── AuthComponent                      (apps/mobile/feature-auth, presentation/auth)
│   ├── LoginComponent                 (presentation/login)          — permanent child
│   └── SelectProviderComponent        (presentation/selectprovider) — ChildSlot child
└── AuthenticatedComponent             (apps/mobile/app-root)
```

`AppRootComponent` holds a `ChildStack` with exactly two configurations, `Auth` and `Authenticated`,
chosen from observed session state. It never names `Login` or `SelectProvider` (R-094, AC-061).

`AuthComponent` owns both children and all navigation between them:

- `LoginComponent` is a permanent child created once through
  `childContext(key = "login")`; it is never recreated by opening or closing the sheet (R-092).
- `SelectProviderComponent` lives in a `ChildSlot` owned by `AuthComponent`
  (`childSlot(source = slotNavigation, key = "select-provider", ...)`), so its presence and
  lifecycle are `AuthComponent` state (R-093). No `ChildSlot`, `ChildStack`, or slot navigation
  exists inside `LoginComponent`.

Outputs, declared as constructor callbacks on the children and handled only by `AuthComponent`
(R-088, R-089):

```kotlin
interface LoginComponent {
    fun dispatch(event: Event)

    sealed interface Event {
        data object LoginClicked : Event
        data class ProviderSelected(val providerId: LoginProviderId) : Event
    }

    sealed interface Output {
        data object OpenProviderSelection : Output
    }
}

interface SelectProviderComponent {
    sealed interface Output {
        data object Dismissed : Output
        data class ProviderSelected(val providerId: LoginProviderId) : Output
    }
}
```

Event flow:

1. `ВОЙТИ` → `login.dispatch(LoginComponent.Event.LoginClicked)` → `LoginComponent` emits
   `Output.OpenProviderSelection` unless its own attempt guard is active.
2. `AuthComponent` activates the slot unless it is already active or transitioning
   (cross-screen duplicate-action guard, R-090, AC-059).
3. Scrim tap or system back → `SelectProviderComponent` emits `Output.Dismissed` →
   `AuthComponent` clears the slot; the same `LoginComponent` instance and state are revealed
   (AC-056).
4. Row tap → `SelectProviderComponent` emits `Output.ProviderSelected(providerId)` →
   `AuthComponent` clears the slot **first**, then calls
   `login.dispatch(LoginComponent.Event.ProviderSelected(providerId))` exactly once (R-026, R-089,
   AC-057, AC-058).
5. `LoginModel` handles the dispatched provider event: a disabled provider emits one-shot
   `ShowSnackbar` news; an enabled provider starts the attempt and owns loading, cancellation,
   failure, success, and duplicate-attempt protection (R-090, R-095).

`AuthComponent.UiState` carries only what the overlay composition needs (whether the sheet is
presented); `LoginComponent.UiState` and `SelectProviderComponent.UiState` carry their own screen
content. `AuthContent` composes `LoginContent` and, when the slot is active, presents
`SelectProviderContent` as the modal bottom sheet with the baseline scrim (R-093).

### Wire contract (`shared/contract/auth`) — exact fields

All types are `@Serializable`, provider-neutral, and live in
`shared/contract/auth/src/commonMain/kotlin/app/yap/contract/auth/`. `provider` is a lowercase
string identifier: `"google"`, `"apple"`, `"tid"`.

```kotlin
@Serializable
data class LoginChallengeRequestDto(
    val provider: String,
    val codeChallenge: String? = null,       // base64url S256, no padding; PKCE attempts only
    val codeChallengeMethod: String? = null, // "S256" exactly when codeChallenge is present
)

@Serializable
data class LoginChallengeDto(
    val challengeId: String,                 // opaque, cryptographically random
    val nonce: String? = null,               // raw nonce, nonce-bound providers only
    val expiresAtEpochSeconds: Long,
)

@Serializable
data class LoginRequestDto(
    val challengeId: String,
    val provider: String,
    val credentialType: String,              // "identity_token" | "authorization_code"
    val idToken: String? = null,
    val authorizationCode: String? = null,
    val codeVerifier: String? = null,
    val redirectUri: String? = null,
)

@Serializable
data class SessionDto(
    val accessToken: String,
    val refreshToken: String,
    val accessTokenExpiresAtEpochSeconds: Long,
    val accountId: String,                   // stable Yap account ID (AC-013, AC-019, AC-054)
)

@Serializable
data class RefreshRequestDto(
    val refreshToken: String,
)

@Serializable
data class ErrorDto(
    val code: String,                        // "challenge_invalid" | "invalid_request" | "provider_unavailable" | "session_invalid"
    val message: String,
)
```

`LoginRequestDto` has no nonce field of any kind: the server never accepts a client-echoed nonce as
evidence (R-042).

`session_invalid` is the definitive outcome of `POST /auth/refresh` (maps to HTTP 401): it is
distinct from `challenge_invalid`, which only ever comes from `/auth/challenge` and `/auth/login`.
An unknown, expired, or reused (`previous_token_hash`) refresh token all resolve to this single
opaque code, mirroring the same non-disclosure principle already applied to `challenge_invalid`.

Valid combinations, by provider flow:

| Flow | Challenge request | Challenge response | Login credential |
| --- | --- | --- | --- |
| Google, Android Credential Manager | `codeChallenge` set (the same prepared attempt also backs the fallback) | `nonce` set | `identity_token` + `idToken` |
| Google, Android browser fallback | same challenge as above, no new request | same `nonce` | `authorization_code` + `authorizationCode` + `codeVerifier` + `redirectUri` |
| Google, iOS | `codeChallenge` absent | `nonce` set | `identity_token` + `idToken` |
| Apple, iOS | `codeChallenge` absent | `nonce` set | `identity_token` + `idToken` |

The contract stays provider-neutral and PKCE-capable, but T-ID is not a registered provider in this
iteration: a challenge or login request naming `"tid"` is refused as `provider_unavailable`. The
`authorization_code` credential shape is exercised only by the Android Google browser fallback.

Rejected combinations:

- `codeChallenge` present without `codeChallengeMethod = "S256"`, or the method without the
  challenge → `invalid_request`.
- `codeChallenge` sent for Apple → `invalid_request`.
- `credentialType = "identity_token"` with any of `authorizationCode`, `codeVerifier`,
  `redirectUri` set, or without `idToken` → `invalid_request`.
- `credentialType = "authorization_code"` with `idToken` set, or missing any of
  `authorizationCode`, `codeVerifier`, `redirectUri` → `invalid_request`.
- `credentialType = "authorization_code"` for a provider/challenge whose stored proof is absent, or
  a verifier whose S256 does not equal the stored proof → `challenge_invalid`.
- Any unknown or unregistered `provider`, which in this iteration includes `"tid"` →
  `provider_unavailable`.

Shape violations are rejected as `invalid_request` before the challenge is looked up, so they never
reveal challenge state. Everything involving challenge existence, expiry, provider match, or proof
comparison collapses into the single opaque `challenge_invalid` (R-045, AC-032).

`redirectUri` ownership: the backend owns the canonical registered redirect URI per provider, read
from feature configuration. The client sends the URI it actually used and the backend compares it
verbatim with the registered value, rejecting a mismatch as `challenge_invalid` before any exchange.
The only registered value in this iteration is the Google Android fallback redirect URI.

### Provider attempt lifecycle (R-096…R-102)

The adapter port is the only place aware of PKCE mechanics, and it exposes no SDK type:

```kotlin
// data/identity/LoginProviderAdapter.kt
internal interface LoginProviderAdapter {

    val providerId: LoginProviderId

    /** Creates fresh, single-use attempt material before the backend challenge is requested. */
    suspend fun prepareAttempt(): PreparedAttempt

    /** Runs the provider flow for an already-prepared attempt bound to [challenge]. */
    suspend fun authenticate(attempt: PreparedAttempt, challenge: LoginChallenge): ProviderAuthResult

    /** Discards prepared material; idempotent and safe to call after any outcome. */
    fun discard(attempt: PreparedAttempt)
}

internal class PreparedAttempt internal constructor(
    val attemptId: String,
    val codeChallenge: String?,   // public S256 value, or null for non-PKCE flows
    internal val codeVerifier: String?, // never leaves the data layer
)

internal data class LoginChallenge(
    val challengeId: String,
    val expiresAtEpochSeconds: Long,
    val nonce: String?,
)

internal sealed interface ProviderAuthResult {
    data class Success(val credential: ProviderCredential) : ProviderAuthResult
    data object Cancelled : ProviderAuthResult
    data class Failure(val kind: ProviderFailureKind) : ProviderAuthResult
}

internal sealed interface ProviderCredential {
    data class IdentityToken(val idToken: String) : ProviderCredential
    data class AuthorizationCode(
        val code: String,
        val codeVerifier: String,
        val redirectUri: String,
    ) : ProviderCredential
}

internal enum class ProviderFailureKind {
    Configuration,          // required external configuration missing or invalid (R-024)
    Connectivity,
    IntegrationNotConfigured, // scaffolded provider, no integration yet (R-104)
    Provider,
}
```

`IntegrationNotConfigured` is the T-ID scaffold's only outcome. It maps to the same user-facing
copy as `Configuration` (AC-030), so no presentation branch knows which providers are scaffolded.

Sequence owned by `DefaultSessionRepository.logIn(providerId)`:

1. `adapter.prepareAttempt()` — for PKCE flows this generates a 32-byte random `code_verifier`,
   base64url without padding, and `code_challenge = base64url(SHA-256(code_verifier))`.
2. `POST /auth/challenge` with `provider` and, when present, `codeChallenge` + `"S256"`. The backend
   stores the received `codeChallenge` verbatim as `auth_challenge.proof` — this is the documented
   source of that column (R-096, AC-062).
3. `adapter.authenticate(attempt, challenge)` — the adapter runs the SDK flow with the returned raw
   `nonce` when the provider is nonce-bound, and returns a credential that carries the `codeVerifier`
   for code flows. The verifier never appears in `PreparedAttempt`'s public surface, domain types,
   model state, `UiState`, `News`, logs, or analytics (R-097, R-102).
4. `POST /auth/login` with the challenge ID and the credential fields from the table above.
5. `adapter.discard(attempt)` in a `finally`, so cancellation, failure, and success all destroy the
   material; the repository refuses to reuse an attempt ID (R-099, R-100, AC-064).

The Android Google adapter always prepares PKCE material even though Credential Manager returns an
ID token, so the browser fallback can submit an authorization code bound to the same challenge
without requesting a second challenge (R-101). The PKCE-capable attempt abstraction is generic and
is justified by that fallback alone; it is not speculative T-ID machinery (R-102).

The T-ID adapter shells implement the same port: `prepareAttempt()` returns an attempt with no PKCE
material, `authenticate(...)` returns `ProviderAuthResult.Failure(IntegrationNotConfigured)`
immediately, and `discard(...)` is a no-op. They construct nothing, touch no SDK, and issue no
request (R-104).

### Server (`services/server/feature-auth`)

Package shape follows [`docs/server/feature-boundaries.md`](../../docs/server/feature-boundaries.md):

```text
services/server/feature-auth/src/main/kotlin/app/yap/server/feature/auth/
├── api/            AuthRoutes.kt, request validation and translation
├── model/          AuthAccount, ProviderIdentity, VerifiedIdentity, AuthFailure, ProviderId
├── identity/       IdentityVerifier port + GoogleIdentityVerifier, AppleIdentityVerifier,
│                   TidIdentityVerifier, IdentityVerifiers registry, provider config
├── persistence/    ChallengeTable, AccountTable, ProviderIdentityTable, SessionTable,
│                   AuthRepository (Exposed)
├── AuthService.kt  scenario orchestration
└── resources/db/migration/V1__auth.sql
```

Tables (Flyway, forward-only):

- `auth_challenge(id uuid pk, provider text, nonce_hash text null, proof text null, created_at,
  expires_at)` — nonce stored as SHA-256 hex only; `proof` holds the exact client-supplied S256 code
  challenge (R-041).
- `auth_account(id uuid pk, created_at)` — product-owned fields only (R-036).
- `auth_provider_identity(id uuid pk, account_id fk, provider text, subject text, email text null,
  is_email_verified bool null, created_at, last_login_at, unique(provider, subject))` (R-034).
- `auth_session(id uuid pk, account_id fk, refresh_token_hash text, previous_token_hash text null,
  created_at, last_used_at, absolute_expires_at, revoked_at null)` — hashes only (R-056).

`AuthService` scenarios (there is no `logOut`):

1. `startChallenge(provider, codeChallenge)` — reject unknown or unconfigured providers, validate the
   PKCE field combination for that provider, create the challenge through
   `TokenService.createChallenge`, persist `hash(nonce)` and/or the code challenge with a 5-minute
   TTL, and return the raw nonce once (R-040, R-041).
2. `login(challengeId, provider, credential)` — verify the provider result **outside** any
   transaction when a network call is needed (R-043), then in one transaction:
   `SELECT ... FOR UPDATE` the challenge, re-check existence/expiry/provider/proof, delete it
   (R-044), resolve-or-create identity and account, create the session, and issue tokens whose hash
   is persisted before commit (R-047). For code credentials the S256 comparison happens **before**
   any token exchange (R-098, AC-063). Any challenge problem maps to `challenge_invalid` (R-045),
   and rejection never deletes expired rows inside a rolled-back transaction (R-046).
3. `refresh(refreshToken)` — parse the session ID, lock the session row, validate inactivity and
   absolute expiry, compare `hash(presented)` with `refresh_token_hash`, move it to
   `previous_token_hash`, and store the rotated hash (R-053, R-056). A value matching
   `previous_token_hash` revokes the whole session (R-057); an unknown, expired, or revoked value all
   resolve to `AuthFailure.SessionInvalid` (`session_invalid`, HTTP 401) and are never inferred from a
   zero-row update.
4. `cleanupExpiredChallenges()` — a separate committed transaction invoked by a scheduled job in
   `app` (R-046).

TTLs: access 15 minutes and refresh 30 days already match `AuthConfig` defaults (R-052, R-053);
absolute session expiry of 180 days (R-054) is a feature-owned constant persisted per session.
`services/server/feature-auth` has no dependency on `core-config`, so `AuthService` takes
`refreshTokenTtl: Duration` as a constructor parameter rather than reading `AuthConfig` itself;
`services/server/app`'s `Main.kt` reads the refresh TTL from `AuthConfig` and passes it when
constructing `AuthService`, and is also responsible for scheduling `cleanupExpiredChallenges()`.

`IdentityVerifier` returns `VerifiedIdentity(provider, subject, email, isEmailVerified)`:

- **Google** — for `identity_token`, verify the ID token against Google's JWKS: signature, `iss`,
  `aud` (server client ID), `azp` when present, `exp`, `sub`, and `nonce` against the stored hash
  (R-049, R-076). For `authorization_code` (Android fallback), exchange the code with PKCE at
  Google's token endpoint and verify the returned ID token identically.
- **Apple** — the same shape against Apple's JWKS with the bundle-ID audience; the client sends
  `sha256(rawNonce)` to Apple, so the verified `nonce` claim is compared with the stored hash
  directly (R-077).
- **T-ID** — `TidIdentityVerifier` exists only as an unregistered scaffold: the port and a
  `TidTokenExchange` interface with no production implementation, no HTTP client, and no endpoint
  constants beyond documentation references. It is never added to the `IdentityVerifiers` registry,
  so `"tid"` resolves as an unavailable provider (R-103, AC-049).

Provider credentials live in feature-owned configuration read from the environment. A provider whose
configuration is absent is not registered, and naming it fails as a configuration error rather than
"coming soon" (R-024, AC-030).

`services/server/app` gains `Main.kt` (`app.yap.server.MainKt`), Ktor `ContentNegotiation`,
`StatusPages` mapping `AuthFailure` to HTTP, `/health`, the auth route registration
(`POST /auth/challenge`, `POST /auth/login`, `POST /auth/refresh` — no logout route), manual graph
construction, and the scheduled challenge cleanup.

### T-ID: deferred scaffold (R-078, R-103…R-106, AC-049, AC-065…AC-067)

T-ID does not perform a login in this iteration. What ships is scaffolding that keeps the
integration point clean:

- the provider-neutral `LoginProviderId.Tid` identifier and its configuration entry, visible and
  disabled by default on both platforms (R-016, R-017);
- `TidLoginAdapter` shells in `androidMain` and `iosMain` implementing `LoginProviderAdapter` and
  returning `ProviderAuthResult.Failure(IntegrationNotConfigured)`;
- callback and result placeholders expressed only in the provider-neutral types above, exposing no
  SDK type;
- an unregistered server-side `TidIdentityVerifier` scaffold with a `TidTokenExchange` port and no
  implementation;
- DI wiring that registers the shells without constructing, linking, or initializing any SDK;
- an explicit `TODO` in each scaffold naming the deferred integration questions below.

No T-ID Gradle dependency, SPM package, URL scheme, client ID, client secret, endpoint call, or
Info.plist entry is added. The application builds and starts with none of them present (R-106,
AC-065).

**Target integration, for the future iteration only.** Reviewed public sources, 2026-08-09:
App-to-App authorization <https://developer.tbank.ru/docs/products/TID/app>; T-API data
<https://developer.tbank.ru/docs/products/TID/web/tapi-data>; userinfo
<https://developer.tbank.ru/docs/api/t-id-informatsiya-o-polzovatele>; Android SDK
<https://opensource.tbank.ru/mobile-tech/T-ID-Android>; iOS SDK
<https://opensource.tbank.ru/mobile-tech/T-ID-iOS>. The intended shape is the official App-to-App
SDK with the registered non-HTTP(S) `mobile_redirect_uri`, a unique PKCE verifier per attempt, no
shipped `client_secret`, a backend authorization-code exchange at the documented token endpoint
(`POST https://id.tbank.ru/auth/token`, which the documentation states must run from the backend),
and a stable subject from the `sub` field of the documented userinfo endpoint
(`POST https://id.tbank.ru/userinfo/userinfo`), with the technical `id_token` ignored. This
paragraph is a research record, not a specification of code to write now: nothing in it may be
implemented as production exchange code until the deferred questions are answered.

Reviewed SDK observations, recorded so the future integration does not repeat the research
(neither is added as a dependency now): Android `ru.tinkoff.core.tinkoffauth:tid`, package
`ru.tbank.core.tid`, newest Maven Central version `1.1.0` while the repository `CHANGELOG.md` lists
`1.2.0`; entry points `TidAuth(...)`, `isTBankAppAuthAvailable()`, `createTBankAppAuthIntent()`,
`createTidAuthIntent(webMode)`; results `TidStatusCode.SUCCESS`/`CANCELLED_BY_USER`,
`TidTokenPayload`, `TidRequestException`. iOS SPM
`https://opensource.tbank.ru/mobile-tech/T-ID-iOS`, newest tag `2.0.1` with the reviewed README on
`master`; `TIDFactory`/`ITID`, `startTAuth(completion:)`, `handleCallbackUrl(_:)`, `TTokenPayload`,
`TAuthError`. Both SDKs, as publicly documented, perform the token exchange themselves and document
neither caller-supplied PKCE material nor raw authorization-code access — which is precisely why the
integration is deferred rather than guessed.

### Provider SDK pins

| Purpose | Exact dependency | Version | API used |
| --- | --- | --- | --- |
| Android Google | `androidx.credentials:credentials` | `1.6.0` (newest stable; `1.7.0` is alpha) | `CredentialManager.getCredential(context, GetCredentialRequest)`; `GetCredentialCancellationException` for explicit cancellation |
| Android Google | `androidx.credentials:credentials-play-services-auth` | `1.6.0` | Play services credential provider |
| Android Google | `com.google.android.libraries.identity.googleid:googleid` | `1.2.0` | `GetSignInWithGoogleOption.Builder(serverClientId).setNonce(nonce).build()`; `GoogleIdTokenCredential.createFrom(bundle).idToken`. Legacy `GoogleSignInOptions`, `DEFAULT_SIGN_IN`, and `com.google.android.gms:play-services-auth` are prohibited as an application login path (R-074) |
| iOS Google | SPM `https://github.com/google/GoogleSignIn-iOS`, product `GoogleSignIn` | `9.2.0` (custom-nonce support landed in `9.0.0`; the pin satisfies "9.0.0 or newer") | `GIDSignIn.sharedInstance.signIn(withPresenting:hint:additionalScopes:nonce:completion:)`; ID token read as `GIDSignInResult.user.idToken?.tokenString`. Configured with the iOS client ID and `serverClientID`, plus the reversed-client-ID URL scheme in the host `Info.plist`. Backend nonce verification stays mandatory |
| iOS Apple | System `AuthenticationServices` framework (no third-party dependency) | iOS SDK of the Xcode toolchain used by the host | `ASAuthorizationAppleIDProvider().createRequest()` with `requestedScopes = [.email]` and `request.nonce = sha256Hex(rawNonce)`; `ASAuthorizationController` delegate; identity token from `ASAuthorizationAppleIDCredential.identityToken`; cancellation detected via `ASAuthorizationError.canceled` |
No T-ID dependency is added to the version catalog, the Gradle build, or the Xcode project in this
iteration; the scaffold compiles against project-owned types only.

Build and test dependency pins introduced by this feature: `com.arkivanov.decompose:decompose:3.3.0`,
`com.arkivanov.decompose:extensions-compose:3.3.0`, `com.arkivanov.essenty:lifecycle:2.5.0`,
`org.testcontainers:postgresql:1.21.3` with PostgreSQL image `postgres:17`,
`com.auth0:jwks-rsa:0.22.2` (alongside the existing `com.auth0:java-jwt:4.5.0`), and
`org.jetbrains.compose.ui:ui-test-junit4` at the catalog's `composeMultiplatform` version.

### Mobile domain and data (`apps/mobile/feature-auth`)

```text
apps/mobile/feature-auth/src/commonMain/kotlin/app/yap/feature/auth/
├── domain/
│   ├── entity/     LoginProvider, LoginProviderId, LoginOutcome, LoginFailure, Session, AccountId
│   ├── repository/ SessionRepository, LoginProviderRepository
│   └── usecase/    ObserveSessionUseCase, RestoreSessionUseCase, LogInUseCase,
│                   ObserveLoginProvidersUseCase
├── data/
│   ├── identity/   LoginProviderAdapter, PreparedAttempt, ProviderCredential, adapter registry
│   ├── local/      SessionStorage (internal interface, Android/iOS implementations), SessionLocal
│   ├── mapper/     SessionMapper, LoginFailureMapper
│   ├── remote/     AuthApi (Ktor), DTO translation
│   └── repository/ DefaultSessionRepository, DefaultLoginProviderRepository
├── presentation/
│   ├── auth/           AuthComponent.kt, DefaultAuthComponent.kt (+ AuthModel), AuthContent.kt
│   ├── login/          LoginComponent.kt, DefaultLoginComponent.kt (+ LoginModel),
│   │                   LoginUiStateMapper.kt, LoginContent.kt
│   └── selectprovider/ SelectProviderComponent.kt, DefaultSelectProviderComponent.kt
│                       (+ SelectProviderModel), SelectProviderUiStateMapper.kt,
│                       SelectProviderContent.kt
├── analytics/      LoginAnalytics (public port), LoginAnalyticsEvent
└── di/             AuthContainer, DefaultAuthContainer, createAuthContainer(...)
```

- `LoginProviderConfig` is the injected configuration: an ordered list of
  `LoginProvider(id, displayName, iconToken, isVisible, isEnabled)`, preserved verbatim (R-011,
  AC-040). Platform defaults live in `di` platform source sets — Android hides Apple, iOS shows all
  three; Google enabled, Apple and T-ID disabled (R-016, R-017).
- `LoginProviderId` carries the lowercase wire identifier as an `id` property
  (`Google("google")`, `Apple("apple")`, `Tid("tid")`) instead of a separate `data/mapper`
  translation. This is a deliberate exception to
  [`docs/mobile/domain.md`](../../docs/mobile/domain.md), which keeps serialization concerns out of
  domain code: the value is a stable product identifier the enum already names, and duplicating it in
  a mapper bought nothing — exhaustiveness protects a newly added provider either way.
- `SessionRepository` (domain) owns `observe(): Flow<Session?>`, `get(forceUpdate: Boolean): Session?`,
  and `logIn(providerId)` only. Per
  [`docs/mobile/domain.md`](../../docs/mobile/domain.md), a domain repository port exposes no
  credentials, so `refresh(rejectedAccessToken)` does not live on it. Rotation and persistence still
  stay atomic: concurrent refreshes are coalesced single-flight, the rotated session is persisted
  before it becomes observable (R-058), definitive rejection clears storage and publishes signed-out
  (R-059), and a transient failure preserves the stored session (R-060). The clear-on-rejection path
  is an internal operation, not a product logout.
- `DefaultSessionRepository` implements the `core-common` `AccessTokenProvider` port itself, so the
  credential-facing `getAccessToken(rejectedAccessToken)` lives in the data layer while the domain
  port stays clean. There is no separate `DefaultAccessTokenProvider` class and no
  `SessionCredentials` interface: both were pure delegation to the repository that already owns
  storage and single-flight refresh. It is installed once through `installAccessTokenModifier`
  (which takes a suspend lambda, so wiring stays `installAccessTokenModifier(repository::getAccessToken)`
  and introduces no construction cycle), so no repository implements `401` retry.
- `SessionStorage` is an internal interface with a platform implementation per target, not
  `expect`/`actual`: the Android implementation needs a `Context`, which an `expect` class cannot
  carry until the DI container exists in Phase 8. Android writes an AES-GCM blob to a private-storage
  file with a key held in Android Keystore (no `EncryptedSharedPreferences`), iOS writes a Keychain
  item with `kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly` (R-079, AC-050). Platform factories
  (`AndroidSessionStorage`/`KeychainSessionStorage` construction) are wired in `di/` by
  `createAuthContainer(...)` in Phase 8.

### Mobile presentation details

- `LoginComponent.UiState` carries marquee text, hero, rotating topics, body, button label, caption,
  and `isLoading`. `SelectProviderComponent.UiState` carries the ordered provider rows with stable
  keys, display names, icon tokens, enabled state, and the empty-state message (R-069, AC-041,
  AC-016).
- One-shot output is `News`: `LoginComponent.News.ShowSnackbar(message)` for a disabled provider, a
  configuration failure, and a recoverable failure. Cancellation emits nothing (R-029, AC-042). Copy
  selection happens in the model/mapper, never in a composable.
- `LoginContent` renders the frozen baseline with Compose primitives: the marquee is two identical
  text copies translated `0 → -50%` in an infinite linear 15s animation; the topic roller uses the
  three-step keyframe timing above; reduced motion is read through a new
  `rememberReducedMotionEnabled()` `expect`/`actual` in `core-design` (Android
  `Settings.Global.ANIMATOR_DURATION_SCALE`, iOS `UIAccessibility.isReduceMotionEnabled`) and stops
  movement while keeping text visible (R-005).
- `core-design` gains the login palette tokens and text styles; `YapTheme` keeps ownership of
  light/dark selection.

### Root composition

`app-root` owns a `ChildStack` with `Auth` and `Authenticated`, switching on observed session state
and deriving the initial destination synchronously so `Login` never flashes during restoration
(R-061, R-066). `AuthenticatedComponent` renders a single `Text("Успешно авторизован")` on the theme
background with no controls (R-064, R-065). Offline startup with a stored session opens the
authenticated destination provisionally and defers refresh (R-062).

`android-app` gains an `Application` that builds the container graph with the `Context` and a
`MainActivity` hosting the root component; `ios-app` gains a real Xcode host that builds `YapShared`,
supplies the Swift-implemented Google and Apple adapters into a `shared-app` factory, and declares
the reversed-client-ID URL scheme. iOS SDKs are linked in Xcode (SPM) and
reach Kotlin through the adapter port, so no Kotlin/Native cinterop or CocoaPods setup is introduced.

## Affected areas

| Area | Paths | Change |
| --- | --- | --- |
| Version catalog | `gradle/libs.versions.toml` | Add the exact pins listed under "Provider SDK pins" |
| Convention plugins | `convention-plugins/` | Add dependency-only `app.yap.decompose.compose` for the shared Decompose/Essenty bundle |
| Wire contract | `shared/contract/auth/src/commonMain/kotlin/app/yap/contract/auth/` | New challenge/login/session/refresh/error DTOs |
| Server auth feature | `services/server/feature-auth/src/main/kotlin/app/yap/server/feature/auth/`, `.../src/main/resources/db/migration/` | New model, identity verifiers, Exposed persistence, `AuthService`, routes, `V1__auth.sql` |
| Server integration tests | `services/server/feature-auth/src/integrationTest/kotlin/...`, `services/server/feature-auth/build.gradle.kts` | New `integrationTest` source set and task (see "Gradle verification model") |
| Server composition | `services/server/app/src/main/kotlin/app/yap/server/` | New `Main.kt`, Ktor plugins, status mapping, `/health`, auth route wiring, challenge-cleanup schedule |
| Mobile auth feature | `apps/mobile/feature-auth/src/{commonMain,androidMain,iosMain,commonTest,androidHostTest,androidDeviceTest}/` | New domain, data, adapters, `Auth`/`Login`/`SelectProvider` presentation, DI, tests. `androidHostTest` is the AGP 9 name for the JVM-hosted Android unit-test source set (not `androidUnitTest`) |
| Mobile feature build | `apps/mobile/feature-auth/build.gradle.kts` | Add `core-common`, `core-test`, Decompose, Credential Manager, googleid, Compose resources. No T-ID dependency |
| Design system | `apps/mobile/core-design/src/{commonMain,androidMain,iosMain}/kotlin/app/yap/core/design/` | Login palette/typography tokens, `rememberReducedMotionEnabled()` |
| Root composition | `apps/mobile/app-root/src/commonMain/kotlin/app/yap/app/root/` | `AppRootComponent` with `Auth`/`Authenticated` branches, `AuthenticatedComponent`, analytics no-op binding |
| Shared entry point | `apps/mobile/shared-app/src/{commonMain,iosMain}/kotlin/` | Root Compose entry and iOS facade accepting Swift adapters |
| Android host | `apps/mobile/android-app/src/main/` | `Application`, `MainActivity`, manifest updates |
| iOS host | `apps/mobile/ios-app/` | Xcode app target, Swift entry point, Swift Google and Apple adapters, reversed-client-ID URL scheme, entitlements. No T-ID SPM package or URL scheme |
| Documentation | `README.md`, `docs/testing/006-backend-integration.md` | Update scaffolding status; record the pinned PostgreSQL major |

## Contracts and data

- New wire DTOs exactly as specified above; no existing contract changes, so there is no client
  compatibility concern. No logout DTO exists.
- New forward-only migration `V1__auth.sql` creating the four tables, the
  `unique(provider, subject)` constraint, and the challenge-expiry index, run through the existing
  `DatabaseFactory` Flyway bootstrap.
- Deployment and Testcontainers both pin **PostgreSQL 17**.
- No client-side database: the unauthenticated zone stays without Room and stores only the session
  blob in platform secure storage.

## Gradle verification model

Docker is required only by the PostgreSQL suite, and that suite runs under its own task:

- `services/server/feature-auth` declares an `integrationTest` source set with its own
  `integrationTest` task (`useJUnitPlatform()`, classpath extending `test`). `check` does **not**
  depend on it, so `./gradlew build` never fails because Docker is missing, and the same tests are
  never executed twice.
- Every fast test — server unit, identity verifier, route, mobile mapper/model/repository/adapter —
  stays in the normal `test`/`allTests` lifecycle and runs under `./gradlew build`.
- Compose UI tests live in the `feature-auth` Android device-test source set and are likewise
  excluded from `./gradlew build`.
- Final verification and CI both run `./gradlew :services:server:feature-auth:integrationTest`
  explicitly. Release acceptance requires it to pass.
- If the container runtime is unavailable, the result is reported as "integration suite not
  executed". That is never reported as passing database verification, and an in-memory fake is never
  substituted (R-083, AC-051).

## Testing strategy

Test-first for every behavior change, following
[`docs/testing/001-structure.md`](../../docs/testing/001-structure.md).

- **Server unit** — `AuthService` scenarios against thin repository stubs: challenge issuance and
  PKCE field validation, opaque `challenge_invalid` for expired/mismatched/missing/proof-mismatch,
  verifier-comparison-before-exchange ordering, verification-before-transaction ordering, refresh
  rejection classes, and configuration failures.
- **Server identity adapters** — Google and Apple token verification with fabricated JWKS material
  (bad signature, wrong issuer/audience, expired, missing `sub`, nonce mismatch, Google `azp`), plus
  a test proving the T-ID verifier scaffold is not registered, so `"tid"` resolves as an unavailable
  provider.
- **Server routes** — HTTP parsing, serialization, the valid/rejected DTO combination matrix, and
  status mapping, including that no credential appears in an error body. No logout route exists.
- **Server integration (`integrationTest`, Testcontainers, PostgreSQL 17)** — challenge
  lock/consume, one session from two concurrent attempts, refresh rotation and replay revocation,
  concurrent first login producing one account, unique provider+subject, rollback leaving no partial
  account, expired-challenge cleanup, and clean bootstrap of all migrations. Concurrency uses a
  latch, never delays.
- **Mobile mapper tests** (primary presentation surface) — exact Russian resources, provider order,
  stable keys, icon tokens, visibility, enabled/loading state, empty-provider state, and both
  platform configurations (R-080, AC-039…AC-041).
- **`AuthComponent` orchestration tests** — `Login` output presents `SelectProvider`; `Dismissed`
  returns to the unchanged `Login` child without recreating it; `ProviderSelected` reaches
  `AuthComponent`, which clears the slot before dispatching
  `LoginComponent.Event.ProviderSelected` to `Login` exactly once; duplicate actions are blocked
  across the transition; `Login` state survives presentation and dismissal (AC-055…AC-059).
- **Structural tests** — assert that `LoginComponent` sources reference neither
  `SelectProviderComponent` nor any slot navigation, that `SelectProviderComponent` references no
  `LoginComponent`, and that `app-root` references neither screen (AC-060, AC-061).
- **Screen model tests** — disabled-provider news, configuration-failure news, enabled dispatch,
  cancellation without news, retry after failure, loading always ending.
- **Mobile repository tests** — restore, single-flight refresh, one retry, definitive rejection
  clearing storage, transient failure preserving it, offline provisional start, attempt preparation
  ordering (prepare → challenge → authenticate → discard), and refusal to reuse a discarded attempt.
- **Platform adapter tests, one suite per adapter** — Android Google (nonce-bound
  `GetSignInWithGoogleOption`, cancellation, lifecycle cancellation, unusable result, fallback
  triggers, no fallback after explicit cancellation, `state` mismatch failing locally), iOS Google
  (custom nonce, success, cancellation, lifecycle cancellation, invalid result mapping), iOS Apple
  (hashed nonce, success, cancellation, lifecycle cancellation, missing email, private relay email).
  Server verifier tests do not substitute for these (R-082).
- **T-ID scaffold tests** — the Android and iOS shells return `IntegrationNotConfigured`, initialize
  no SDK, issue no network request, and do not block startup when no T-ID configuration exists; the
  disabled row still shows `Вход через T-ID скоро появится` exactly once (R-104, R-105, AC-029,
  AC-066, AC-067). No test asserts a working T-ID login.
- **Storage tests** — Android Keystore/AES-GCM evidence and iOS Keychain evidence with the
  device-only accessibility class (AC-050).
- **Compose UI tests** (device source set) — only what a mapper cannot prove: sheet presentation over
  the existing screen, overlay/back dismissal, provider action wiring, in-button loading
  replacement, one-shot snackbar rendering, semantics, reduced motion, and light/dark rendering
  (R-081).
- **Visual verification (not a test)** — pixel-level conformance to the hashed baseline (typography,
  spacing, palette, marquee seamlessness) is checked by inspection against the frozen files after
  the screens render. It is recorded as visual verification, not as a failing-test-first task.
- **Acceptance** — integration tests assert the stable account ID for same-provider re-login and
  restoration, and different IDs for unlinked providers (R-085, AC-054).
- **Physical device** — a real Google round trip on Android and iOS is mandatory evidence; the Apple
  round trip is reported as externally blocked with the named missing credential if its
  configuration is unavailable, while its adapter and contract tests still pass. T-ID has no
  physical verification in this iteration and is reported as deferred (R-084, AC-052).

## Documentation impact

- `README.md` — the "only scaffolded / no authentication behavior yet" statements become false.
- `docs/testing/006-backend-integration.md` — record PostgreSQL 17 as the pinned shared major.
- No other guide changes: the design follows the existing mobile, server, shared, and testing rules
  as written.

## Deferred integration questions (T-ID)

None of these blocks implementation work. They are recorded so the future T-ID iteration starts from
research rather than guesswork, and each is referenced by a `TODO` in the corresponding scaffold.
Google, Apple, the component hierarchy, backend sessions, storage, UI, hosts, and analytics all
proceed without any of them being answered.

- **D1 — token-exchange ownership.** Public documentation shows both official SDKs performing the
  token exchange on-device (`getTidTokenPayload(uri)`; `startTAuth`/`handleCallbackUrl` →
  `TTokenPayload`) while the T-API documentation states the token request must run from the backend.
  Partner-only documentation must state whether the SDK accepts a caller-supplied
  `code_verifier`/`code_challenge`, or exposes the raw authorization code, before any backend
  exchange path is written. Do not invent either API.
- **D2 — Android artifact version.** The public source tag and the latest published Maven Central
  artifact have differed during planning. Re-check both sources and confirm the intended coordinate,
  version, and repository at integration time instead of copying a stale version into the scaffold.
- **D3 — iOS API surface.** The reviewed README is repository `master`; the newest tag is `2.0.1`.
  Confirm the `2.0.1` type and method names at integration time.
- **D4 — minimal userinfo scope.** The documentation lists `profile` and `phone` for the full field
  set; the minimal scope that still returns `sub` must be confirmed rather than assumed.

## External prerequisites

- **Required by this iteration**: Google OAuth clients (Android, iOS, server) and access to Google's
  signature-verification keys, for the mandatory physical round trip (R-084).
- **Required for Apple**: Sign in with Apple configuration and the iOS bundle identifier. If absent,
  the Apple physical round trip is reported as externally blocked while its adapter and contract
  tests still pass.
- **Required only to replace the T-ID scaffold later**: partner registration, `client_id` and
  backend-held `client_secret`, the registered non-HTTP(S) `mobile_redirect_uri`, confirmed SDK
  versions and callback APIs, answers to D1–D4, and partner test accounts. The application must
  build, start, and log in with Google without any of these.

## Risks and mitigations

- **Decompose is mandated by the guides but absent from the build**: adding it is a prerequisite,
  not an optional refactor. Introduce it first so the presentation and navigation rules are followed
  literally.
- **Navigation ownership regressions**: the `Login`/`SelectProvider` separation is easy to erode.
  Structural tests (AC-060, AC-061) guard it in addition to behavior tests.
- **T-ID scaffolding could drift into speculative implementation**: the scaffold must stay inert.
  No SDK dependency, no HTTP exchange code, no invented PKCE or token contract, and no claim of
  adapter or end-to-end verification. Tests assert the inert behavior, and D1–D4 stay recorded as
  open questions rather than assumptions.
- **iOS SDK linkage**: Google Sign-In and Authentication Services are used from Swift in the Xcode
  host and injected through the provider port, avoiding cinterop/CocoaPods churn in the KMP build.
- **Design drift**: any re-import with a different SHA-256 invalidates visual acceptance; re-check
  the hashes before visual verification rather than after.
- **Secret handling**: credentials, verifiers, nonces, and tokens must never enter state, logs,
  analytics, or test output; assert their absence rather than assuming it (AC-036, AC-037, R-097).
- **Detekt excludes miss `androidHostTest`**: `config/detekt/detekt.yml` and the `DetektPlugin`
  convention have no source-set exclude for `androidHostTest` (or the device-test source set).
  Phase 5 worked around this with narrow `@Suppress` on the affected test files; either add a proper
  exclude for Android test source sets or keep using targeted `@Suppress`, but do not widen a rule
  project-wide for a test-only false positive.
- **`ktor-client-mock` is not pinned in `gradle/libs.versions.toml`**: without it, `DefaultAuthApi`'s
  protocol-level behavior and the end-to-end `401` single-retry cycle (R-055) are untested at the
  Ktor client boundary — only through fakes above it. Pin `ktor-client-mock` at the catalog's Ktor
  version and add that coverage before Phase 6/7 sign-off, or explicitly accept the gap in
  "Testing strategy".

## Verification

- `./gradlew build` — compilation, fast tests, and Detekt across all modules (no Docker, no device)
- `./gradlew detekt`
- `./gradlew :services:server:feature-auth:integrationTest` — PostgreSQL 17 Testcontainers suite
- `./gradlew :apps:mobile:shared-app:compileKotlinIosSimulatorArm64`
- the `feature-auth` Android device-test task (Compose UI tests on an emulator or device); the
  JVM-hosted Android test source set is `androidHostTest` (AGP 9 naming, confirmed in Phase 5), not
  `androidUnitTest` — the device-test source/task name still needs confirming from
  `./gradlew :apps:mobile:feature-auth:tasks` once T043 configures it, since the KMP Android library
  plugin names it differently from an application module
- physical-device provider round trips: Google mandatory; Apple passes or is reported as externally
  blocked; T-ID is reported as deferred and is not attempted
- a build-and-start check with no T-ID credentials, configuration, or SDK present (AC-065)
