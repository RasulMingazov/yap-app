# Tasks: Login (001-authentication)

**Specification**: [spec.md](spec.md)
**Plan**: [plan.md](plan.md)

Every behavior task is test-first: add the focused failing test, observe the expected failure,
implement the smallest passing change, then refactor while green. Task order reflects that — a
behavior's test task always precedes its implementation task. Tasks that are explicitly labelled
*visual verification* are inspection against the frozen design baseline, not failing tests.

This iteration has no logout: no logout DTO, route, service operation, use case, or test appears
below.

T-ID is scaffolded, not implemented (R-078, R-103…R-106). Phase 11 adds inert shells only; it links
no SDK, writes no exchange code, and claims no T-ID verification. Its deferred questions D1–D4 in
`plan.md` never block another phase.

## Phase 1 — Build foundations and wire contract

- [x] **T001** Add the exact pins from `plan.md` to `gradle/libs.versions.toml`:
  `com.arkivanov.decompose:decompose:3.3.0`, `com.arkivanov.decompose:extensions-compose:3.3.0`,
  `com.arkivanov.essenty:lifecycle:2.5.0`, `org.testcontainers:postgresql:1.21.3` with image
  `postgres:17`, `com.auth0:jwks-rsa:0.22.2`, `androidx.credentials:credentials:1.6.0`,
  `androidx.credentials:credentials-play-services-auth:1.6.0`,
  `com.google.android.libraries.identity.googleid:googleid:1.2.0`, and
  `org.jetbrains.compose.ui:ui-test-junit4`. No T-ID dependency is added (R-104)
- [x] **T002** Add the dependency-only `app.yap.decompose.compose` convention plugin and use it to
  wire Decompose and Essenty into `apps/mobile/feature-auth`, `apps/mobile/app-root`, and
  `apps/mobile/shared-app`; add `core-common` plus `core-test` and the Compose resources package to
  `feature-auth`
- [x] **T003** Add the `integrationTest` source set and task to
  `services/server/feature-auth/build.gradle.kts` per the plan's Gradle verification model: own
  classpath, `useJUnitPlatform()`, and **not** wired into `check`
- [x] **T004** Add the wire DTOs in
  `shared/contract/auth/src/commonMain/kotlin/app/yap/contract/auth/` exactly as specified in
  `plan.md` — `LoginChallengeRequestDto`, `LoginChallengeDto`, `LoginRequestDto`, `SessionDto`,
  `RefreshRequestDto`, `ErrorDto` — with no nonce field on `LoginRequestDto` (R-042)

## Phase 2 — Server challenge issuance and login

- [ ] **T005** Add failing `AuthServiceTest` cases for challenge issuance: a fresh challenge is
  stored with only the nonce hash, the client-supplied `codeChallenge` is persisted verbatim as the
  challenge proof, the TTL is 5 minutes, and an unregistered provider is rejected (R-040, R-041,
  R-096, AC-062)
- [ ] **T006** Add `model/` values (`ProviderId`, `AuthFailure`, `VerifiedIdentity`, `AuthAccount`,
  `ProviderIdentity`) and the `AuthRepository` port
- [ ] **T007** Add `persistence/` Exposed tables and
  `services/server/feature-auth/src/main/resources/db/migration/V1__auth.sql` for challenge,
  account, provider identity, and session, including `unique(provider, subject)` and the
  challenge-expiry index
- [ ] **T008** Implement `AuthService.startChallenge(provider, codeChallenge)` on
  `TokenService.createChallenge`
- [ ] **T009** Add failing `GoogleIdentityVerifierTest` cases: bad signature, wrong issuer, wrong
  audience, `azp` when present, expiry, missing `sub`, and nonce mismatch (R-049, R-076)
- [ ] **T010** Implement `identity/IdentityVerifier` and `GoogleIdentityVerifier` with JWKS
  verification, the authorization-code exchange path used by the Android fallback, and feature-owned
  provider configuration
- [ ] **T011** Add failing `AuthServiceTest` cases for login: provider verification happens before
  the transaction opens; the S256 verifier comparison happens before any token exchange; the
  challenge is consumed exactly once; identity and account are resolved or created; and expired,
  mismatched, missing, and proof-mismatched challenges all return the single opaque
  `challenge_invalid` (R-043, R-044, R-045, R-098, AC-063)
- [ ] **T012** Add a failing test proving a missing or invalid provider configuration fails as a
  configuration error, never as a "coming soon" outcome (R-024, AC-030)
- [ ] **T013** Implement `AuthService.login` with the locked-challenge transaction, resolve-or-create
  identity, session creation, and pre-commit secret hashing (R-047)

## Phase 3 — Server session lifecycle

- [ ] **T014** Add failing `AuthServiceTest` cases for refresh: rotation on success, inactivity
  expiry after 30 days, absolute expiry after 180 days, replay of a previous hash revoking the whole
  session, and an unknown value rejected without inferring replay from a zero-row update
  (R-053…R-057)
- [ ] **T015** Implement `AuthService.refresh` with the locked-session transaction and hash-only
  storage
- [ ] **T016** Add a failing test proving a rejection path never deletes an expired row inside a
  transaction that rolls back, then implement `cleanupExpiredChallenges()` in its own committed
  transaction (R-046)

## Phase 4 — Server exposure and verification checkpoint

- [ ] **T017** Add failing route tests in `services/server/feature-auth/src/test/.../api/` covering
  the full DTO combination matrix from `plan.md`: every valid per-provider combination accepted, and
  every rejected combination mapped to `invalid_request`, `challenge_invalid`, or
  `provider_unavailable` as documented, with no credential in any error body
- [ ] **T018** Add a failing route test asserting that only `POST /auth/challenge`,
  `POST /auth/login`, and `POST /auth/refresh` exist, so no logout route is introduced
- [ ] **T019** Implement `api/AuthRoutes.kt` translating HTTP only, with shared failure mapping and
  redirect-URI validation against the registered per-provider value
- [ ] **T020** Create `services/server/app/src/main/kotlin/app/yap/server/Main.kt` with Ktor
  `ContentNegotiation`, `StatusPages`, `/health`, manual graph construction, auth route
  registration, and the scheduled challenge cleanup
- [ ] **T021** Add the PostgreSQL integration suite in
  `services/server/feature-auth/src/integrationTest/`: clean bootstrap of all migrations, challenge
  lock/consume with two concurrent attempts yielding one session, concurrent first login yielding
  one account, unique provider+subject, rollback leaving no partial account, refresh rotation,
  replay revocation, expired-challenge cleanup, and stable-account-ID continuity for same-provider
  re-login versus unlinked providers (R-083, R-085, AC-031…AC-035, AC-054). Coordinate concurrency
  with a latch
- [ ] **T022** **Server checkpoint** — run `./gradlew :services:server:feature-auth:test`,
  `./gradlew :services:server:app:test`, and
  `./gradlew :services:server:feature-auth:integrationTest`. If no container runtime is available,
  report "integration suite not executed"; never report it as passing database verification

## Phase 5 — Mobile session storage, repository, and attempt lifecycle

- [ ] **T023** Add failing Android storage tests proving an AES-GCM blob in private app storage with
  its key held in Android Keystore and no `EncryptedSharedPreferences` anywhere in the module
  (R-079, AC-050)
- [ ] **T024** Add failing iOS storage tests proving a Keychain item written with
  `kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly` (AC-050)
- [ ] **T025** Implement `data/local/SessionStorage` as `expect` plus the Android and iOS actuals
- [ ] **T026** Add failing `DefaultSessionRepositoryTest` cases for the attempt lifecycle: the
  adapter prepares the attempt before the challenge request, only the `codeChallenge` is sent, the
  verifier reaches only the login request, `discard` runs on success, cancellation, and failure, and
  a discarded attempt cannot be reused (R-096…R-100, AC-062, AC-064)
- [ ] **T027** Add failing `DefaultSessionRepositoryTest` cases for the session: restore a valid
  session, coalesce concurrent refreshes into one in-flight call, persist rotated credentials before
  publishing them, clear storage on definitive rejection, preserve it on transient failure, and open
  provisionally when offline (R-058…R-060, R-062, AC-021)
- [ ] **T028** Add `domain/entity`, `domain/repository`, `data/identity` port types
  (`LoginProviderAdapter`, `PreparedAttempt`, `ProviderCredential`, `ProviderAuthResult`),
  `data/remote/AuthApi` on the existing `NetworkClient`, and `data/mapper`, then implement
  `DefaultSessionRepository`
- [ ] **T029** Add the use cases (`ObserveSessionUseCase`, `RestoreSessionUseCase`, `LogInUseCase`,
  `ObserveLoginProvidersUseCase`) with their `Default...UseCase` implementations in the same files
- [ ] **T030** Add a failing test proving exactly one silent refresh and one retry for a rejected
  authenticated request, then implement `DefaultAccessTokenProvider` over `SessionRepository`
  (R-055, AC-022)

## Phase 6 — Auth orchestration and screen state

- [ ] **T031** Add failing `SelectProviderUiStateMapperTest` cases asserting the complete `UiState`
  for both platform defaults: order preserved, stable keys, display names, icon tokens, visibility,
  enabled state, and the `Нет доступных способов входа` empty state (R-011…R-017, AC-005, AC-006,
  AC-016, AC-040, AC-041)
- [ ] **T032** Add `LoginProvider`, `LoginProviderId`, and the injected `LoginProviderConfig`, with
  Android and iOS defaults in `di` platform source sets (Google enabled; Apple and T-ID disabled;
  Apple hidden on Android)
- [ ] **T033** Add the Russian string resources for both screens to `feature-auth` Compose resources
  verbatim from the frozen baseline and the specification (R-067, R-068, AC-039)
- [ ] **T034** Implement `SelectProviderComponent` with `Output.Dismissed` and
  `Output.ProviderSelected`, its default component, model, and mapper. It must contain no reference
  to `LoginComponent` (R-088, R-091)
- [ ] **T035** Add failing `LoginUiStateMapperTest` cases for marquee, hero, topics, body, button
  label, caption, and the loading state that replaces the label (R-003, AC-044)
- [ ] **T036** Implement `LoginComponent` with `Output.OpenProviderSelection`, its default
  component, `LoginModel`, mapper, and the public `dispatch(Event)` API with `LoginClicked` and
  `ProviderSelected(providerId)`. It must declare no slot navigation and no reference to
  `SelectProviderComponent` (R-088, R-089, R-093)
- [ ] **T037** Add failing `AuthComponentTest` orchestration cases: a `Login`
  `OpenProviderSelection` output presents `SelectProvider` through `AuthComponent`; `Dismissed`
  returns to the same unchanged `Login` child without recreating it; `ProviderSelected` is delivered
  to `AuthComponent`; the slot is cleared before Auth calls
  `login.dispatch(LoginComponent.Event.ProviderSelected(providerId))` exactly once; duplicate
  actions are blocked across the transition; and `Login` state is preserved while `SelectProvider`
  is presented (R-087…R-095, AC-055…AC-059)
- [ ] **T038** Add failing structural tests asserting `LoginComponent` sources reference neither
  `SelectProviderComponent` nor any slot navigation, `SelectProviderComponent` references no
  `LoginComponent`, and `app-root` references neither screen (AC-060, AC-061)
- [ ] **T039** Add failing `LoginModelTest` cases for `dispatch(ProviderSelected(...))`: disabled-provider
  news exactly once, configuration-failure news, enabled dispatch, cancellation emitting no news,
  retry after failure, loading always ending, and duplicate provider events starting at most one
  attempt (R-027…R-031, R-090, AC-023…AC-029, AC-042)
- [ ] **T040** Implement `AuthComponent`, `DefaultAuthComponent`, and `AuthModel`: the permanent
  `Login` child, the `ChildSlot` holding `SelectProvider`, output handling, and the cross-screen
  duplicate-action guard; then implement the tested `LoginModel` provider-event behavior and
  `AuthStubs`, `LoginStubs`, and `SelectProviderStubs`
- [ ] **T041** **Mobile logic checkpoint** — run `./gradlew :apps:mobile:feature-auth:allTests`

## Phase 7 — Compose rendering of the frozen baseline

- [ ] **T042** Add the login palette tokens and text styles to
  `apps/mobile/core-design/src/commonMain/kotlin/app/yap/core/design/theme/` from the hashed
  baseline, for light and dark, and add `rememberReducedMotionEnabled()` as `expect`/`actual` for
  Android and iOS
- [ ] **T043** Configure the `feature-auth` Android device-test source set and confirm the exact
  device-test task name from `./gradlew :apps:mobile:feature-auth:tasks`
- [ ] **T044** Add failing Compose UI tests for `Login`: in-button loading replacement with unchanged
  button size, one-shot snackbar rendering, usable semantics, reduced-motion behavior, and
  light/dark rendering (R-081, AC-043…AC-045)
- [ ] **T045** Add failing Compose UI tests for the sheet, driven through `AuthContent`: modal
  presentation over the existing `Login`, scrim-tap and system-back dismissal, and provider action
  wiring (R-081, AC-003, AC-004)
- [ ] **T046** Implement `LoginContent` with the seamless two-copy marquee, the rotating topics, the
  in-button spinner, and the top snackbar position (R-002…R-005, R-027)
- [ ] **T047** Implement `SelectProviderContent` with handle, `СПОСОБЫ ВХОДА`, provider rows, and the
  empty state, and implement `AuthContent` owning the modal sheet and scrim composition over
  `LoginContent` (R-008, R-010, R-013, R-093)
- [ ] **T048** Run the device-test task on an emulator or device and record the result
- [ ] **T049** *Visual verification* — inspect the rendered `Login` and `SelectProvider` against the
  hashed baseline files for typography, spacing, palette, marquee seamlessness, and both themes.
  This is post-implementation inspection, not a failing test (R-002, AC-043, AC-045)

## Phase 8 — Application hosts and session branch

- [ ] **T050** Add failing `AppRootComponentTest` cases: a valid stored session opens the
  authenticated destination with no intermediate `Login`, an invalid session clears storage and
  opens the authentication branch, and the root exposes only those two branches (R-061, R-063,
  R-094, AC-017, AC-020, AC-061)
- [ ] **T051** Implement `AppRootComponent` with the `Auth`/`Authenticated` `ChildStack`,
  synchronous initial-destination derivation, and the offline provisional path (R-062, R-066)
- [ ] **T052** Implement `AuthenticatedComponent` and its content with only `Успешно авторизован` on
  the theme background and no controls (R-064, R-065, AC-015, AC-018)
- [ ] **T053** Implement `createAuthContainer(...)` in `apps/mobile/feature-auth/src/.../di/` and the
  `shared-app` root entry point plus the iOS facade that accepts Swift-provided adapters
- [ ] **T054** Add `Application` and `MainActivity` to `apps/mobile/android-app/src/main/` and update
  `AndroidManifest.xml`
- [ ] **T055** Create the Xcode host in `apps/mobile/ios-app/`: app target, Swift entry point linking
  `YapShared`, the Google reversed-client-ID URL scheme, and entitlements; add no T-ID URL scheme
- [ ] **T056** **Hosts checkpoint** — run `./gradlew build` and
  `./gradlew :apps:mobile:shared-app:compileKotlinIosSimulatorArm64`

## Phase 9 — Google adapters

- [ ] **T057** Add failing Android Google adapter tests: attempt preparation producing a fresh
  verifier and S256 challenge, the nonce-bound `GetSignInWithGoogleOption` success path, explicit
  cancellation, lifecycle cancellation clearing pending callbacks, an unusable non-cancellation
  result, invalid-result mapping, and the absence of legacy Google Sign-In application APIs
  (R-074, R-082, AC-047)
- [ ] **T058** Implement the Android Credential Manager adapter behind `LoginProviderAdapter` using
  `androidx.credentials` 1.6.0 and `googleid` 1.2.0, with a lifecycle-aware host holder
- [ ] **T059** Add failing Android fallback tests: allowed triggers only, PKCE bound to the same
  backend challenge, a fresh `state`, the same attempt nonce, no fallback after explicit
  cancellation, and a mismatched `state` failing locally before the code is sent (R-075, R-101,
  R-050, AC-038)
- [ ] **T060** Implement the browser fallback that submits `authorization_code` for the same prepared
  attempt
- [ ] **T061** Add failing iOS Google adapter tests: custom nonce passed to the SDK, success with an
  extracted ID token, explicit cancellation, lifecycle cancellation, and invalid-result mapping
  (R-076, R-082, AC-048)
- [ ] **T062** Implement the iOS Google adapter in Swift against `GoogleSignIn` 9.2.0 using
  `signIn(withPresenting:hint:additionalScopes:nonce:completion:)` and
  `GIDSignInResult.user.idToken?.tokenString`, returning only the ID token through the port
- [ ] **T063** Perform a physical-device Google round trip on Android and iOS and record the
  evidence (R-084, AC-052)

## Phase 10 — Apple adapter

- [ ] **T064** Add failing `AppleIdentityVerifier` tests for signature, issuer, bundle-ID audience,
  expiry, subject, and the `sha256(rawNonce)` comparison, plus a test proving a previously stored
  verified email survives a later response that omits email (R-037, R-077, AC-048)
- [ ] **T065** Implement the server Apple verifier
- [ ] **T066** Add failing iOS Apple adapter tests: the hashed nonce sent to Apple, success,
  explicit cancellation, lifecycle cancellation, a missing email, and a private relay email
  (R-038, R-082, AC-048)
- [ ] **T067** Implement the iOS Apple adapter in Swift on `AuthenticationServices`, and add a test
  proving Apple stays absent on Android (R-017, AC-005)
- [ ] **T068** Record the Apple physical round trip or the named external blocker (AC-052)

## Phase 11 — T-ID scaffold

- [ ] **T069** Add failing scaffold tests: the Android and iOS `TidLoginAdapter` shells return the
  typed `IntegrationNotConfigured` result, construct and initialize no SDK, issue no network
  request, expose no SDK type through the provider port, and do not block startup when no T-ID
  configuration exists; selecting the disabled T-ID row still shows `Вход через T-ID скоро появится`
  exactly once and starts no loading state (R-023, R-104, R-105, AC-029, AC-066, AC-067)
- [ ] **T070** Implement the compile-safe Android and iOS `TidLoginAdapter` scaffolds behind
  `LoginProviderAdapter` — `prepareAttempt()` without PKCE material, `authenticate(...)` returning
  `ProviderAuthResult.Failure(IntegrationNotConfigured)`, no-op `discard(...)` — plus the
  unregistered server-side `TidIdentityVerifier` and `TidTokenExchange` scaffolding with no
  implementation, no HTTP client, and no endpoint calls. Wire them into DI so nothing initializes
  while the provider is disabled (R-103, R-104)
- [ ] **T071** Document the exact external conditions required to replace the scaffolds with working
  adapters: reference D1–D4 and the T-ID external prerequisites from `plan.md` in a `TODO` on each
  scaffold, and record the same conditions in the `feature-auth` module documentation so the
  boundary is discoverable without reading the spec (R-106, AC-067)
- [ ] **T072** Add a failing test proving the server refuses a challenge or login request naming
  `"tid"` as `provider_unavailable` because the verifier is unregistered, then satisfy it (AC-049)
- [ ] **T073** Verify the application builds, starts, and passes the implemented authentication
  contract tests with no T-ID credentials, configuration, SDK dependency, or URL scheme present;
  do not repeat the physical Google round trip already covered by T062 (R-106, AC-065)

## Phase 12 — Analytics

- [ ] **T074** Add failing tests asserting the emitted event sequence — view, provider selected, flow
  started, cancellation, coarse failure, success — segmented by platform and provider and containing
  no tokens, codes, verifiers, nonces, email, or subjects (R-070…R-072, AC-046)
- [ ] **T075** Add the public provider-neutral `LoginAnalytics` port and its events in
  `apps/mobile/feature-auth/src/.../analytics/`; emit navigation/selection events from `AuthModel`
  and attempt lifecycle events from `LoginModel`, then bind a no-op implementation in
  `apps/mobile/app-root` (R-073)

## Final verification

- [ ] **T076** Add and satisfy assertions that credentials, verifiers, and nonces are absent from
  screen state, news payloads, logs, analytics, and test output, and that provider tokens,
  authorization codes, and identity tokens are not persisted after the attempt ends (R-039, R-051,
  R-100, AC-036, AC-037)
- [ ] **T077** Re-import `screen_login.dc.html` and `support.js` and confirm the SHA-256 hashes
  still match `plan.md` before signing off visual criteria (AC-053)
- [ ] **T078** Run `./gradlew build`, `./gradlew detekt`, and
  `./gradlew :apps:mobile:shared-app:compileKotlinIosSimulatorArm64`
- [ ] **T079** Run `./gradlew :services:server:feature-auth:integrationTest` against PostgreSQL 17
  and the `feature-auth` device-test task; report explicitly if a container runtime or device is
  unavailable (AC-051)
- [ ] **T080** Compare the implementation with every acceptance criterion AC-001…AC-067 and record
  any externally blocked item with its named missing credential or access
- [ ] **T081** Update `README.md` (scaffolding status) and `docs/testing/006-backend-integration.md`
  (pinned PostgreSQL major)
