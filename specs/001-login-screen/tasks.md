# Tasks: Login Screen

**Input**: Design documents from `/specs/001-login-screen/`

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md),
[data-model.md](data-model.md), [contracts/auth-api.md](contracts/auth-api.md),
[quickstart.md](quickstart.md)

**Tests**: Included and mandatory. Constitution Principle III (Test-First for Behavior Change) is
NON-NEGOTIABLE, and plan.md's Constitution Check commits to ordering every behavioral test before
its implementation. Each test task must be run and seen to fail for the intended reason before the
implementation task that follows it. Coverage follows `research.md` R10 — each behavior is tested
once, at the boundary that owns it. No test is written for build files, `expect`/`actual` platform
wrappers with no project-owned branch, behavior owned by an SDK, or a use case that only delegates
to a port and adds no rule of its own (T027, T070) — that behavior is proven beneath it and a second
test would assert the delegation, not the rule. Four tasks are labelled wiring or regression guards
and are called out in place rather than passed off as test-first steps: the FR-008 case inside T089,
the two Koin `verify()` tests (T031, T086), and the application-level wiring guard (T120) — each
asserts a property that holds by construction once the wiring exists. For the Compose UI tests, the
red state is a test that does not compile because the composable it names does not exist yet; that
is still red for the intended reason, and it is why they sit in the same phase as the screen rather
than in Polish.

**Where a failure becomes a status code**: the `AuthFailure` → HTTP mapping and the rate limiter
both live in `services/server/app`, and no feature may depend on `app`. A `testApplication` inside
`feature-auth` therefore assembles the feature's routes without either plugin and can observe
neither. So the feature's route tests assert the **failure the route raises** and the rows it does
or does not write, while `400`/`401`/`503`/`429` are proven once in `app`'s own tests (T046, T048,
T120).

**Organization**: grouped by user story, so each story can be implemented, tested, and demoed
independently.

**Task IDs are allocation order, not execution order.** T001–T128 were numbered in execution
order by the first generation. Anything added afterwards takes the next free number and is placed
in the phase it belongs to, so a later number can sit in an earlier phase. Renumbering is not an
option: 87 tasks are already done and the rest reference each other by ID. T129–T134 (FR-033) are
the only such additions so far, and they live in User Story 1.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: can run in parallel — different files, no dependency on an incomplete task
- **[Story]**: US1–US4, mapping to the user stories in spec.md

## Path Conventions

- Mobile KMP: `apps/mobile/<module>/src/{commonMain,androidMain,iosMain,commonTest,androidHostTest}/kotlin/…`
- Server JVM: `services/server/<module>/src/{main,test}/kotlin/…`
- Wire contracts: `shared/contract/auth/src/commonMain/kotlin/app/yap/contract/auth/`
- iOS host: `apps/mobile/ios-app/` — outside Gradle, verified only from Xcode

Three placement rules the design documents state explicitly, repeated here because they decide file
paths below:

- `featureAuthModule()` is public in **`impl`**, since `app-root` depends on `impl` precisely to
  load it and a function in `api` cannot bind declarations that are `internal` to `impl`. Two
  guides currently state the opposite; **T122 corrects them in this PR**, because Principle V
  forbids leaving a rule and the code standing as a contradiction.
- The three `GOOGLE_*_CLIENT_ID` values are read by **`AppConfigLoader`** in `core-config`, the
  only thing that reads the `.env` file `quickstart.md` documents, while the `GoogleAuthConfig`
  type stays in the feature.
- The launch renewal's *condition* lives in **`impl`** behind the `RenewSessionUseCase` contract in
  `api`, because `SessionLocal` and both expiry fields are `internal` and `app-root` owns only the
  trigger (research.md R15).

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: split the auth feature into `api`/`impl`, add the new dependencies, and create the
entry points the repository does not yet have. No behavior, so no tests.

- [X] T001 Split `:apps:mobile:feature-auth` into `:apps:mobile:feature-auth:api` and `:apps:mobile:feature-auth:impl` in `settings.gradle.kts`
- [X] T002 [P] Create `apps/mobile/feature-auth/api/build.gradle.kts` — `yap.kmp.library` + `yap.serialization`, depending on `:apps:mobile:core-common` **and `libs.navigation3.runtime`**, which `AuthNavKey : NavKey` needs: `Navigation3ComposePlugin` declares that artifact with `implementation`, so it never arrives transitively, and applying the whole `yap.navigation3` plugin would drag the Compose navigation stack into a module that has no Compose. No `yap.koin`: nothing in `api` declares bindings
- [X] T003 [P] Create `apps/mobile/feature-auth/impl/build.gradle.kts` — `yap.kmp.library`, `yap.compose.multiplatform`, `yap.koin.compose`, `yap.navigation3`, depending on `:apps:mobile:feature-auth:api`, `core-common`, `core-design`, `core-network`, `:shared:contract:auth`, with `kotlin-test`, `kotlinx-coroutines-test`, `stubcall`, `koin-test`, and `:apps:mobile:core-test` in `commonTest`, and **`withHostTest { }` enabled on the Android target** so `androidHostTest` exists for the Credential Manager tests (T062) and for the UI tests (T012)
- [X] T004 Delete the pre-split scaffold `apps/mobile/feature-auth/build.gradle.kts`, `apps/mobile/feature-auth/src/`, and `apps/mobile/feature-auth/test-fixtures/` (plan.md Structure Decision)
- [X] T005 Point `apps/mobile/app-root/build.gradle.kts` at `:apps:mobile:feature-auth:api` and `:apps:mobile:feature-auth:impl`, and add `kotlin-test`, `kotlinx-coroutines-test`, `koin-test`, and `:apps:mobile:core-test` to its `commonTest`
- [X] T006 [P] Add the Android login, storage, and splash dependencies to `gradle/libs.versions.toml`: `androidx.credentials:credentials`, `androidx.credentials:credentials-play-services-auth`, `com.google.android.libraries.identity.googleid:googleid`, `net.openid:appauth`, `androidx.datastore:datastore-preferences`, `androidx.core:core-splashscreen` — resolve the newest stable release and **pin that exact version**, no ranges and no `+` (research.md R11)
- [X] T007 [P] Add the server dependencies to `gradle/libs.versions.toml`, each pinned to an exact stable version: `com.auth0:jwks-rsa`, `io.ktor:ktor-server-status-pages`, `io.ktor:ktor-server-rate-limit`, `io.ktor:ktor-client-cio`, `io.ktor:ktor-server-test-host`, `org.testcontainers:postgresql`, `org.testcontainers:junit-jupiter` (research.md R11)
- [X] T008 [P] Export the auth API from the iOS framework in `apps/mobile/shared-app/build.gradle.kts`: `api(project(":apps:mobile:feature-auth:api"))` plus `export(...)` on the framework so Swift can implement `GoogleCredentialProvider` (research.md R2)
- [X] T009 [P] Add jwks-rsa, java-jwt, the Ktor CIO client with content negotiation, and the Exposed dependencies to `services/server/feature-auth/build.gradle.kts`, with `ktor-server-test-host`, Testcontainers, and `kotlin-test` in `src/test`
- [X] T010 [P] Add `ktor-server-status-pages`, `ktor-server-rate-limit`, and `:services:server:core-database` / `:services:server:core-security` to `services/server/app/build.gradle.kts`, with `ktor-server-test-host` and `kotlin-test` in `src/test` — `app` is where both the error-mapping and rate-limit tests live, since no feature may depend on `app` (research.md R13)
- [X] T011 [P] Declare the Android entry point in `apps/mobile/android-app/src/main/AndroidManifest.xml` (`MainActivity`, splash theme, `INTERNET` permission) and add `activity-compose` + `core-splashscreen` to `apps/mobile/android-app/build.gradle.kts`
- [X] T012 [P] Configure the Compose Multiplatform UI test harness for `apps/mobile/feature-auth/impl` per the official `testing-setup` skill, in `apps/mobile/feature-auth/impl/build.gradle.kts`. The UI tests (T077–T080, T118) live in `commonTest` and **execute on the Android host compilation** — `androidHostTest` with Robolectric and `isIncludeAndroidResources = true` — so `./gradlew build` (T125) runs them on every change; iOS-simulator execution of the same files is welcome but is not what the gate depends on. Fix this in the build file once, so no test file has two possible homes

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: the wire contracts, platform ports, public feature surface, session storage, app
shell, and server process that every user story below depends on.

**⚠️ CRITICAL**: no user story work can begin until this phase is complete.

### Wire contracts

- [X] T013 [P] Add `GoogleCredentialsDto`, `GoogleAuthorizationCodeDto`, and `RefreshCredentialsDto` in `shared/contract/auth/src/commonMain/kotlin/app/yap/contract/auth/` (contracts/auth-api.md)
- [X] T014 [P] Add `SessionDto` in `shared/contract/auth/src/commonMain/kotlin/app/yap/contract/auth/SessionDto.kt` — credentials only, both expiry fields, wire field order

### Platform capability ports

- [X] T015 [P] Add `Platform` as `expect` plus Android and iOS actuals in `apps/mobile/core-common/src/{commonMain,androidMain,iosMain}/kotlin/app/yap/core/common/platform/Platform.kt` (research.md R7)
- [X] T016 [P] Add `MotionPreferences` as `expect` plus actuals in `apps/mobile/core-common/src/{commonMain,androidMain,iosMain}/kotlin/app/yap/core/common/platform/MotionPreferences.kt` — Android reads `Settings.Global.ANIMATOR_DURATION_SCALE`, iOS reads `UIAccessibility.isReduceMotionEnabled` (research.md R7)
- [X] T017 [P] Add `ActivityProvider` in `apps/mobile/core-common/src/androidMain/kotlin/app/yap/core/common/platform/ActivityProvider.kt`, holding the resumed Activity and clearing the reference outside that window so none leaks (research.md R1)

### Public feature surface (`feature-auth/api`)

- [X] T018 [P] Add `AuthNavKey` with its `@Serializable data object Login` destination in `apps/mobile/feature-auth/api/src/commonMain/kotlin/app/yap/feature/auth/api/AuthNavKey.kt`
- [X] T019 [P] Add `AuthState` (`Unknown`/`LoggedOut`/`LoggedIn`), `AuthProvider`, `LoginOutcome`, and `UserId` in `apps/mobile/feature-auth/api/src/commonMain/kotlin/app/yap/feature/auth/api/entity/` (data-model.md)
- [X] T020 [P] Add the `ObserveAuthStateUseCase`, `LoginWithGoogleUseCase`, and `RenewSessionUseCase` contracts in `apps/mobile/feature-auth/api/src/commonMain/kotlin/app/yap/feature/auth/api/usecase/` — `RenewSessionUseCase` takes no parameter and returns nothing, because `app-root` supplies only the trigger and the feature owns both the condition and every outcome (contracts/auth-api.md Client-side ports, research.md R15)
- [X] T021 [P] Add `GoogleCredentialProvider` and the `GoogleCredential` hierarchy in `apps/mobile/feature-auth/api/src/commonMain/kotlin/app/yap/feature/auth/api/GoogleCredentialProvider.kt` — public because Swift implements it (contracts/auth-api.md)

### Session storage and auth state

- [X] T022 [P] Add `SessionLocal` and the `SessionStorage` `expect` declaration in `apps/mobile/feature-auth/impl/src/commonMain/kotlin/app/yap/feature/auth/data/local/` (data-model.md Client persistence)
- [X] T023 [P] Implement the Android `SessionStorage` actual — Keystore AES-GCM over DataStore Preferences — in `apps/mobile/feature-auth/impl/src/androidMain/kotlin/app/yap/feature/auth/data/local/SessionStorage.android.kt` (research.md R3)
- [X] T024 [P] Implement the iOS `SessionStorage` actual — Keychain `kSecClassGenericPassword` with `kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly` — in `apps/mobile/feature-auth/impl/src/iosMain/kotlin/app/yap/feature/auth/data/local/SessionStorage.ios.kt` (research.md R3)
- [X] T025 Write the failing test that `AuthState` starts `Unknown` until storage has been read and then resolves once, in `apps/mobile/feature-auth/impl/src/commonTest/kotlin/app/yap/feature/auth/data/repository/DefaultAuthRepositoryTest.kt` (FR-024, research.md R10/R12)
- [X] T026 Add the `AuthRepository` port in `apps/mobile/feature-auth/impl/src/commonMain/kotlin/app/yap/feature/auth/domain/repository/AuthRepository.kt` and `DefaultAuthRepository` in `.../data/repository/DefaultAuthRepository.kt`, publishing `Flow<AuthState>` from `SessionStorage` — makes T025 pass
- [X] T027 Add `DefaultObserveAuthStateUseCase` in `apps/mobile/feature-auth/impl/src/commonMain/kotlin/app/yap/feature/auth/domain/usecase/DefaultObserveAuthStateUseCase.kt` — pure delegation to `AuthRepository`, so T025 beneath it is the coverage
- [X] T028 Declare `featureAuthModule()` in `apps/mobile/feature-auth/impl/src/commonMain/kotlin/app/yap/feature/auth/di/FeatureAuthModule.kt` binding `SessionStorage`, `AuthRepository`, and `ObserveAuthStateUseCase`, and list it **before** `coreNetworkModule()` in `apps/mobile/app-root/src/commonMain/kotlin/app/yap/app/root/di/AppModules.kt` (research.md R4)

### App shell

- [X] T029 Replace `RootNavKey.Auth` with `RootNavKey.Main` in `apps/mobile/app-root/src/commonMain/kotlin/app/yap/app/root/navigation/RootNavKey.kt` — the logged-out root is the feature's own `AuthNavKey.Login`, which `featureAuthModule()` registers, so `app-root` owns only the destination no feature owns — and add the placeholder in `apps/mobile/app-root/src/commonMain/kotlin/app/yap/app/root/navigation/MainPlaceholderScreen.kt` (spec.md Assumptions)
- [X] T030 Add `appRootModule()` in `apps/mobile/app-root/src/commonMain/kotlin/app/yap/app/root/di/AppRootModule.kt` binding `navigation<RootNavKey.Main> { MainPlaceholderScreen() }`, and list it in `appModules(...)` — without it `koinEntryProvider()` has no entry for the logged-in root and FR-009 fails at first navigation, not at build time (docs/mobile/003)
- [X] T031 Add the Koin `verify()` test for `appRootModule()` in `apps/mobile/app-root/src/androidHostTest/kotlin/app/yap/app/root/di/AppRootModuleTest.kt` — a wiring **regression guard**, like T086 and T120, not a test-first step. It lives in `androidHostTest` rather than `commonTest` because Koin's `verify()` is JVM-only, which also requires `withHostTest { }` on the module's Android target
- [X] T032 Write the failing root back-stack test — `Unknown` renders no destination, `LoggedOut` roots at `AuthNavKey.Login`, `LoggedIn` roots at `RootNavKey.Main` — in `apps/mobile/app-root/src/commonTest/kotlin/app/yap/app/root/navigation/RootBackStackTest.kt` (research.md R8/R10)
- [X] T033 Implement the auth-driven back stack in `apps/mobile/app-root/src/commonMain/kotlin/app/yap/app/root/navigation/RootBackStack.kt`, collecting `ObserveAuthStateUseCase` and swapping the root — makes T032 pass (FR-001, FR-009, FR-011, FR-012)
- [X] T034 Add the `App()` root composable in `apps/mobile/shared-app/src/commonMain/kotlin/app/yap/shared/app/App.kt`, wrapping `YapTheme` around the back stack with `koinEntryProvider()`
- [X] T035 Add `initAndroidKoin(...)` in `apps/mobile/shared-app/src/androidMain/kotlin/app/yap/shared/app/InitAndroidKoin.kt`, binding `Platform`, `MotionPreferences`, and `ActivityProvider` through `initKoin`'s `appDeclaration`
- [X] T036 Add `initIosKoin(baseUrl:googleCredentialProvider:)` in `apps/mobile/shared-app/src/iosMain/kotlin/app/yap/shared/app/InitIosKoin.kt`, binding the Swift-supplied `GoogleCredentialProvider` plus the iOS `Platform` and `MotionPreferences` (research.md R2). Both entry points gain the login and legal configuration in T085, where the first consumer of those values appears
- [X] T037 Add `MainActivity` in `apps/mobile/android-app/src/main/kotlin/app/yap/android/MainActivity.kt` — call `initAndroidKoin`, publish itself through `ActivityProvider`, and `setContent { App() }`
- [X] T038 Create the Xcode host `apps/mobile/ios-app/YapApp.xcodeproj` with `YapApp/` — embed `YapShared.framework`, add the storyboard launch screen and `Assets.xcassets/SplashBackground.colorset`, call `initIosKoin`, and host the Compose view controller (research.md R12)

### Server process and schema

- [X] T039 [P] Add the PostgreSQL 17 Testcontainers support — container definition and per-test database isolation — in `services/server/feature-auth/src/test/kotlin/app/yap/server/feature/auth/persistence/PostgresTestSupport.kt` (docs/testing/003, research.md R6)
- [X] T040 Write the failing integration test that all current migrations apply cleanly to an empty PostgreSQL 17 database and produce `users`, `provider_identities` with its unique constraint, and `sessions`, in `services/server/feature-auth/src/test/kotlin/app/yap/server/feature/auth/persistence/AuthMigrationIntegrationTest.kt`
- [X] T041 Add the forward-only migration `services/server/feature-auth/src/main/resources/db/migration/V1__auth.sql` creating the three tables, the `unique (provider, provider_user_id)` constraint, and the `user_id` indexes — makes T040 pass (data-model.md)
- [X] T042 [P] Add the Exposed tables `UsersTable`, `ProviderIdentitiesTable`, and `SessionsTable` in `services/server/feature-auth/src/main/kotlin/app/yap/server/feature/auth/persistence/AuthTables.kt`
- [X] T043 [P] Add `GoogleAuthConfig` — web, Android, and iOS client IDs — in `services/server/feature-auth/src/main/kotlin/app/yap/server/feature/auth/identity/GoogleAuthConfig.kt`, and load the three `GOOGLE_*_CLIENT_ID` values in `services/server/core-config/src/main/kotlin/app/yap/server/core/config/AppConfigLoader.kt`, which is the only reader of `.env`; `app` passes the loaded values into the feature's type (research.md R5, constitution «core-config owns environment loading»)
- [X] T044 [P] Add `AuthenticatedSession` and the `AuthFailure` hierarchy — one subtype per outcome the API contract distinguishes: malformed input, unverifiable confirmation, and provider unavailable — in `services/server/feature-auth/src/main/kotlin/app/yap/server/feature/auth/model/`
- [X] T045 Raise the `REFRESH_TOKEN_TTL_SECONDS` default to `7_776_000` (90 days) in `services/server/core-config/src/main/kotlin/app/yap/server/core/config/AppConfigLoader.kt` (FR-025, research.md R6)
- [X] T046 Write the failing error-mapping test in `services/server/app/src/test/kotlin/app/yap/server/app/ErrorMappingTest.kt` — over a probe route that raises each `AuthFailure` subtype, the response carries `400`, `401`, or `503` and the server-only error body, and that body exposes no provider or transport detail. This mapping is project-owned behavior that lives in `app` (T047), so no test inside `feature-auth` can reach it — which is why the route tests below assert the failure rather than the status (FR-014, contracts/auth-api.md Errors, docs/server/001)
- [X] T047 Add `Application.kt` in `services/server/app/src/main/kotlin/app/yap/server/app/Application.kt` — Netty engine, `AppConfigLoader`, `DatabaseFactory.init`, `ContentNegotiation`, a `StatusPages` mapping of `AuthFailure` to `400`/`401`/`503` with a server-only error body, `/health`, graceful shutdown closing `DatabaseFactory`, and feature graph construction — makes T046 pass. `429` is not in that mapping: it is produced by the `RateLimit` plugin (T049), not by an `AuthFailure`
- [X] T048 Write the failing rate-limit test in `services/server/app/src/test/kotlin/app/yap/server/app/RateLimitPluginTest.kt` — over a probe route inside the limited scope, the 101st request from one address in a minute returns `429`, and a request from a second address is unaffected. It lives in `app` because the plugin does, and neither `feature-auth` nor `core-*` may depend on `app` (FR-027, SC-010, research.md R13)
- [X] T049 Install the Ktor `RateLimit` plugin in `services/server/app/src/main/kotlin/app/yap/server/app/Application.kt`, **keyed on the originating IP — `call.request.origin.remoteHost`, not the `RequestConnectionPoint` itself, which carries the ephemeral remote port and would bucket per connection** — and read the threshold from a new `authRateLimitRequestsPerMinute` (default 100) in `services/server/core-config/src/main/kotlin/app/yap/server/core/config/AppConfig.kt` and `AppConfigLoader.kt`; `TRUST_PROXY_HEADERS` is what makes that host the client's rather than the proxy's — makes T048 pass (FR-027, research.md R13)

**Checkpoint**: the app launches to a login-less shell, the server starts and migrates, and every
story below can proceed.

---

## Phase 3: User Story 1 - First login without registration (Priority: P1) 🎯 MVP

**Goal**: a person installs Yap, taps "ВОЙТИ", picks Google, confirms with Google — natively or in
the browser on a device without Google's services — and lands on the main screen with an account
that exists. Apple and T-ID are listed per platform and report that they are not available yet.

**Independent Test**: fresh install, complete the Google confirmation, observe arrival on the main
screen; `users` and `provider_identities` each gain one row.

### Server — verification and the two Google doors

- [X] T050 [US1] Write the failing `GoogleIdentityVerifier` tests with locally signed tokens — accepted token yields `sub`/`email`/`name`/`picture`; bad signature, wrong `aud`, wrong `iss`, expired, nonce mismatch, and absent `sub` are each rejected — in `services/server/feature-auth/src/test/kotlin/app/yap/server/feature/auth/identity/GoogleIdentityVerifierTest.kt` (FR-031, FR-026, research.md R5/R10)
- [X] T051 [US1] Implement `GoogleIdentityVerifier` on java-jwt plus a cached, rate-limited `JwkProvider` in `services/server/feature-auth/src/main/kotlin/app/yap/server/feature/auth/identity/GoogleIdentityVerifier.kt` — makes T050 pass
- [X] T052 [US1] Write the failing `AuthService` tests over thin persistence stubs — a verified identity resolves or creates an account and issues a session; an unverifiable one creates nothing and raises the unverifiable-confirmation `AuthFailure` — in `services/server/feature-auth/src/test/kotlin/app/yap/server/feature/auth/AuthServiceTest.kt` (FR-006, FR-031)
- [X] T053 [US1] Write the failing Testcontainers tests for the persistence adapter itself — an unknown `(google, sub)` inserts one `users` row and one `provider_identities` row in a single transaction, a known one resolves to the existing user without inserting, and issuing a session writes a `sessions` row holding the refresh-token **hash** and never the raw token — in `services/server/feature-auth/src/test/kotlin/app/yap/server/feature/auth/persistence/AuthPersistenceRepositoryIntegrationTest.kt`. T052's stubs prove the service's orchestration, not the SQL beneath it, so without this task T054 would be the only implementation in the feature written before any test that covers it (FR-006, FR-007, docs/testing/003)
- [X] T054 [US1] Add the Exposed persistence adapter `AuthPersistenceRepository` — resolve `(provider, provider_user_id)`, insert `users` + `provider_identities` in one transaction, persist the refresh-token hash as a `sessions` row — in `services/server/feature-auth/src/main/kotlin/app/yap/server/feature/auth/persistence/AuthPersistenceRepository.kt` — makes T053 pass (data-model.md)
- [X] T055 [US1] Add `AuthService` in `services/server/feature-auth/src/main/kotlin/app/yap/server/feature/auth/AuthService.kt`, orchestrating verify → resolve-or-create → `TokenService.issueTokens` — makes T052 pass
- [X] T056 [US1] Write the failing `testApplication` route tests for `POST /v1/auth/google` — a verified credential returns `200` with a `SessionDto` in wire field order; a malformed body never reaches the service; an unverifiable credential raises the unverifiable-confirmation `AuthFailure` **with no `users`/`provider_identities`/`sessions` row written**; an unreachable key set raises the provider-unavailable `AuthFailure` — in `services/server/feature-auth/src/test/kotlin/app/yap/server/feature/auth/api/GoogleAuthRoutesTest.kt`. It asserts the failure raised, not the status it becomes: this harness assembles the feature's routes without `app`'s `StatusPages` and rate limiter, so `401`, `503`, and `429` are proven in T046, T048, and T120 (FR-031, SC-015)
- [X] T057 [US1] Add the `POST /v1/auth/google` route with DTO↔model mapping at the route boundary and the rate-limit scope applied, in `services/server/feature-auth/src/main/kotlin/app/yap/server/feature/auth/api/AuthRoutes.kt` — routes raise `AuthFailure` and never build a status code themselves — makes T056 pass
- [X] T058 [US1] Write the failing `GoogleCodeExchanger` tests against a stubbed token endpoint — success, replayed code, verifier mismatch, Google unreachable — in `services/server/feature-auth/src/test/kotlin/app/yap/server/feature/auth/identity/GoogleCodeExchangerTest.kt` (research.md R14/R10)
- [X] T059 [US1] Implement `GoogleCodeExchanger`, posting `code` + `code_verifier` + `redirect_uri` + the **Android** client ID — this door is Android-only, so no platform selection rule exists — and verifying the returned ID token through `GoogleIdentityVerifier`, in `services/server/feature-auth/src/main/kotlin/app/yap/server/feature/auth/identity/GoogleCodeExchanger.kt` — makes T058 pass (contracts/auth-api.md)
- [X] T060 [US1] Write the failing route tests for `POST /v1/auth/google/code` — `200` on success; an invalid, expired, replayed, or PKCE-mismatched code raises the unverifiable-confirmation `AuthFailure`; an unreachable token endpoint raises the provider-unavailable one — in `services/server/feature-auth/src/test/kotlin/app/yap/server/feature/auth/api/GoogleCodeAuthRoutesTest.kt`; statuses belong to T046 and T120 (contracts/auth-api.md)
- [X] T061 [US1] Add the `POST /v1/auth/google/code` route converging on the same account resolution, in `services/server/feature-auth/src/main/kotlin/app/yap/server/feature/auth/api/AuthRoutes.kt` — makes T060 pass

### Client — platform credential adapters

- [X] T062 [US1] Write the failing `AndroidGoogleCredentialProvider` tests over a stubbed Credential Manager — success returns `GoogleCredential.IdToken`; `GetCredentialProviderConfigurationException` and `NoCredentialException` fall back to the browser flow; `GetCredentialCancellationException` propagates as a cancellation and **never** falls back — in `apps/mobile/feature-auth/impl/src/androidHostTest/kotlin/app/yap/feature/auth/data/identity/AndroidGoogleCredentialProviderTest.kt` (FR-029, FR-013, research.md R1/R10)
- [X] T063 [US1] Implement `AndroidGoogleCredentialProvider` with `GetSignInWithGoogleOption` (web client ID as `serverClientId`, plus the caller-supplied nonce) resolving the Activity through `ActivityProvider`, in `apps/mobile/feature-auth/impl/src/androidMain/kotlin/app/yap/feature/auth/data/identity/AndroidGoogleCredentialProvider.kt` — makes T062 pass (FR-005)
- [X] T064 [US1] Implement the AppAuth PKCE browser fallback returning `GoogleCredential.AuthorizationCode` in `apps/mobile/feature-auth/impl/src/androidMain/kotlin/app/yap/feature/auth/data/identity/GoogleBrowserAuthFlow.kt`, and register the reversed-client-ID redirect intent filter in `apps/mobile/android-app/src/main/AndroidManifest.xml` — Custom Tabs only, never an embedded web view (FR-029, research.md R14)
- [X] T065 [US1] Add the Swift `GoogleCredentialProvider` implementation in `apps/mobile/ios-app/YapApp/GoogleCredentialProviderImpl.swift` — add GoogleSignIn via Swift Package Manager, register the reversed-client-ID URL scheme, forward `application(_:open:options:)` to `GIDSignIn`, return `GoogleCredential.IdToken` only, and pass the implementation into `initIosKoin` (research.md R2)

### Client — data and domain

- [X] T066 [US1] Write the failing remote data source and mapper tests — `SessionDto` maps to `SessionLocal` and domain state; HTTP failures translate to feature failures without leaking transport detail — in `apps/mobile/feature-auth/impl/src/commonTest/kotlin/app/yap/feature/auth/data/remote/AuthRemoteDataSourceTest.kt` (FR-014, research.md R10)
- [X] T067 [US1] Add `AuthRemoteDataSource` in `apps/mobile/feature-auth/impl/src/commonMain/kotlin/app/yap/feature/auth/data/remote/AuthRemoteDataSource.kt` and `SessionMapper` in `.../data/mapper/SessionMapper.kt` — makes T066 pass
- [X] T068 [US1] Write the failing repository login tests — a fresh nonce is generated per attempt and submitted with the credential; a `GoogleCredential.IdToken` calls `/v1/auth/google` and an `AuthorizationCode` calls `/v1/auth/google/code`; success stores the session and publishes `LoggedIn` — in `apps/mobile/feature-auth/impl/src/commonTest/kotlin/app/yap/feature/auth/data/repository/DefaultAuthRepositoryTest.kt` (FR-029, FR-009, contracts/auth-api.md)
- [X] T069 [US1] Implement the login path — generate the nonce, pass it to `GoogleCredentialProvider`, submit it in `GoogleCredentialsDto`, and persist the session so it survives restart — in `apps/mobile/feature-auth/impl/src/commonMain/kotlin/app/yap/feature/auth/data/repository/DefaultAuthRepository.kt` — makes T068 pass (FR-010, research.md R5)
- [X] T070 [US1] Add `DefaultLoginWithGoogleUseCase` in `apps/mobile/feature-auth/impl/src/commonMain/kotlin/app/yap/feature/auth/domain/usecase/DefaultLoginWithGoogleUseCase.kt` — delegation only at this point; it gains its own rule, and its own test, with the 60-second bound in T115

### Client — presentation

- [X] T071 [US1] Write the failing `LoginUiStateMapper` tests — on Android the list is Google and T-ID with Apple absent in every state, on iOS all three appear, only Google is available, an attempt in progress sets `isLoggingIn` while leaving every provider row present, and an unset terms or privacy destination yields a rendered but non-navigating link — in `apps/mobile/feature-auth/impl/src/commonTest/kotlin/app/yap/feature/auth/presentation/login/LoginUiStateMapperTest.kt` (FR-003, FR-004, FR-015, FR-028, SC-004)
- [X] T072 [US1] Implement `LoginUiStateMapper` in `apps/mobile/feature-auth/impl/src/commonMain/kotlin/app/yap/feature/auth/presentation/login/LoginUiStateMapper.kt`, deriving `UiState` from domain state, `Platform`, `MotionPreferences`, and the injected legal destinations — makes T071 pass
- [X] T073 [US1] Write the failing `LoginViewModel` tests through `runViewModelTest` — the primary action opens the provider sheet, choosing Google starts exactly one attempt, and choosing Apple or T-ID emits the not-yet-available notice without starting an attempt or showing progress — in `apps/mobile/feature-auth/impl/src/commonTest/kotlin/app/yap/feature/auth/presentation/login/LoginViewModelTest.kt` (FR-002, FR-004, SC-006)
- [X] T074 [US1] Implement `LoginViewModel` with nested `UiState`, `News`, and `Event` in `apps/mobile/feature-auth/impl/src/commonMain/kotlin/app/yap/feature/auth/presentation/login/LoginViewModel.kt` — makes T073 pass
- [X] T075 [US1] Add the Russian copy — marquee, hero, topic words, body, "ВОЙТИ", the caption, the sheet title, the legal line, and the not-yet-available notice — in `apps/mobile/feature-auth/impl/src/commonMain/composeResources/values/strings.xml` (FR-017, FR-018, FR-028)
- [X] T076 [US1] Add the screen's colour tokens for both light and dark in `apps/mobile/feature-auth/impl/src/commonMain/kotlin/app/yap/feature/auth/presentation/login/LoginColors.kt`, and **record the resolved role→value table in `research.md` R9** so FR-019 is checkable against a committed artefact rather than against a design tool nobody can open in review. They stay in the feature until a second screen needs them (FR-019, research.md R9)
- [X] T077 [P] [US1] Write the failing screen-content test — the marquee band, hero heading, rotating topic, body copy, the "ВОЙТИ" action, the caption, and the legal line are all present in that top-to-bottom order; an attempt in progress shows progress on the primary action; the provider sheet is headed "Способы входа" with one row per provider carrying its own name and mark and dismisses on an outside tap; and a configured terms or privacy destination hands **exactly that URI** to the `UriHandler` when its link is tapped, while an unset one hands over nothing and reports no failure — in `apps/mobile/feature-auth/impl/src/commonTest/kotlin/app/yap/feature/auth/presentation/login/LoginScreenContentTest.kt` (FR-015, FR-017, FR-018, FR-028, SC-011)
- [X] T078 [P] [US1] Write the failing layout-resilience test — both themes, 320 dp of width, 200% font scale, both extremes together, insets respected, and overflowing content scrolling rather than clipping — in `apps/mobile/feature-auth/impl/src/commonTest/kotlin/app/yap/feature/auth/presentation/login/LoginScreenLayoutTest.kt` (FR-019, FR-021, SC-008, SC-011)
- [X] T079 [P] [US1] Write the failing motion test — a reported reduced-motion preference stops the marquee and the rotating topic while leaving all copy readable at rest — in `apps/mobile/feature-auth/impl/src/commonTest/kotlin/app/yap/feature/auth/presentation/login/LoginScreenMotionTest.kt` (FR-020)
- [X] T080 [P] [US1] Write the failing semantics test — every interactive element exposes a spoken name, and each provider row's name identifies the authentication provider it starts — in `apps/mobile/feature-auth/impl/src/commonTest/kotlin/app/yap/feature/auth/presentation/login/LoginScreenSemanticsTest.kt` (FR-022)
- [X] T081 [US1] Implement `LoginScreen` in `apps/mobile/feature-auth/impl/src/commonMain/kotlin/app/yap/feature/auth/presentation/login/LoginScreen.kt` — marquee band, hero heading, rotating topic, body copy, primary action with its progress state, caption, in that order, with system insets respected, overflow scrolling, and animation gated by `UiState.isMotionReduced` — makes T078 and T079 pass and the screen half of T077 (FR-015, FR-017, FR-019, FR-020, FR-021)
- [X] T082 [US1] Implement the "Способы входа" `ModalBottomSheet` in `apps/mobile/feature-auth/impl/src/commonMain/kotlin/app/yap/feature/auth/presentation/login/AuthProviderSheet.kt` — one row per provider with its own name and mark, dismissible by tapping outside — makes the sheet half of T077 pass (FR-018)
- [X] T083 [US1] Implement the legal line beneath the primary action in `apps/mobile/feature-auth/impl/src/commonMain/kotlin/app/yap/feature/auth/presentation/login/LegalLine.kt`, opening each destination through `LocalUriHandler` with no checkbox and no extra tap; what the browser then shows is outside Yap's surface — makes the legal half of T077 pass (FR-028, SC-011)
- [X] T084 [US1] Add the accessibility semantics — a spoken name on the primary action, on each provider row identifying the provider it starts, and on both legal links — in `apps/mobile/feature-auth/impl/src/commonMain/kotlin/app/yap/feature/auth/presentation/login/LoginScreen.kt`, `.../AuthProviderSheet.kt`, and `.../LegalLine.kt`, which is where the two links actually live (T083) — makes T080 pass (FR-022)
- [X] T085 [US1] Extend `featureAuthModule()` in `apps/mobile/feature-auth/impl/src/commonMain/kotlin/app/yap/feature/auth/di/FeatureAuthModule.kt` with `googleServerClientId`, `termsUrl`, and `privacyUrl` parameters, `LoginWithGoogleUseCase`, `LoginViewModel`, `navigation<AuthNavKey.Login>`, and the Android `GoogleCredentialProvider` binding — and widen `appModules(...)`, `initKoin(...)` in `apps/mobile/app-root/src/commonMain/kotlin/app/yap/app/root/di/AppModules.kt`, `initAndroidKoin(...)`, and `initIosKoin(...)` to carry the same values, so Android supplies them exactly as iOS does and neither platform reads them from global state. Android additionally carries `googleAndroidClientId` and `googleRedirectUri`, which default to blank because the browser fallback (T064) exists only there (docs/mobile/003, quickstart.md Configuration)
- [X] T086 [US1] Add the Koin `verify()` test for `featureAuthModule()` in `apps/mobile/feature-auth/impl/src/androidHostTest/kotlin/app/yap/feature/auth/di/FeatureAuthModuleTest.kt` — a wiring **regression guard**, like T031 and T120, not a test-first step. `androidHostTest` for the same JVM-only reason as T031 (research.md R10)

### Client — presentation, made declarative (FR-033)

**Added after the first generation.** T071–T074 and T082 already deliver FR-003 and FR-004, and
their behavior does not change here — Google works, Apple is iOS-only, Apple and T-ID still report
"not available yet". What changes is where those answers come from: two facts declared once per
provider instead of `platform ==` and `provider ==` checks spread across the mapper and the view
model. Run this group after T086.

- [X] T129 [US1] Add the per-provider declaration table in a new `apps/mobile/feature-auth/impl/src/commonMain/kotlin/app/yap/feature/auth/presentation/login/AuthProviderCatalog.kt` — exactly one entry per `AuthProvider` constant, each carrying the platforms it is shown on, whether choosing it starts a real login, and its label resource. Seed it to reproduce today's behavior unchanged: Google shown on both platforms and usable, Apple shown on iOS only and not usable, T-ID shown on both and not usable. A missing entry must be impossible to ship — cover the enum exhaustively rather than by a list the compiler cannot check (FR-033)
- [X] T130 [US1] Write the failing `LoginUiStateMapper` tests driven by declarations rather than by provider names — a provider declared hidden contributes no row on either platform, one declared shown contributes exactly one, the shown and usable facts move independently (usable-but-hidden yields no row at all; shown-but-unusable yields a row with `isAvailable` false), and the order of rows follows the declaration order — in `apps/mobile/feature-auth/impl/src/commonTest/kotlin/app/yap/feature/auth/presentation/login/LoginUiStateMapperTest.kt`, alongside the FR-003/FR-004 cases T071 already proves (FR-033, SC-016)
- [X] T131 [US1] Rewrite `providersFor` and `provider` in `apps/mobile/feature-auth/impl/src/commonMain/kotlin/app/yap/feature/auth/presentation/login/LoginUiStateMapper.kt` as a fold over the T129 declarations, so no `AuthProvider` constant is named anywhere in the file, and rename `fun map` to `operator fun invoke` so the mapper is called as `LoginUiStateMapper(dataState = …, …)`; update the single call site in `.../LoginViewModel.kt` — makes T130 pass and keeps T071 passing
- [X] T132 [US1] Write the failing `LoginViewModel` tests, again driven by declarations rather than by provider names — a provider declared usable starts exactly one attempt through the login path registered for it, one declared unusable emits the not-yet-available notice with no attempt and no progress, and one declared usable but hidden is unreachable through `Event.ProviderChosen` and likewise emits the notice without attempting — in `apps/mobile/feature-auth/impl/src/commonTest/kotlin/app/yap/feature/auth/presentation/login/LoginViewModelTest.kt`, alongside the FR-004 cases T073 already proves (FR-033, SC-016)
- [X] T133 [US1] Replace the `provider != AuthProvider.GOOGLE` branch and the direct `loginWithGoogleUseCase()` call in `apps/mobile/feature-auth/impl/src/commonMain/kotlin/app/yap/feature/auth/presentation/login/LoginViewModel.kt` with a lookup of the login path declared for the chosen provider — injected as a `Map<AuthProvider, LoginUseCase>` so the view model names no provider — falling back to the not-yet-available notice when the chosen provider is not shown, not usable, or has no path registered. Register Google's path in `featureAuthModule()` in `apps/mobile/feature-auth/impl/src/commonMain/kotlin/app/yap/feature/auth/di/FeatureAuthModule.kt`, and rename `LoginWithGoogleUseCase` in `apps/mobile/feature-auth/api/src/commonMain/kotlin/app/yap/feature/auth/api/usecase/` to the provider-neutral contract the map is keyed on, carrying its implementation and stub with it — makes T132 pass (FR-033)
- [X] T134 [US1] Prove the toggle by hand exactly once: flip T-ID's usable fact on in `AuthProviderCatalog.kt`, register a throwaway login path for it, confirm the sheet row starts a real attempt with no other file touched, then revert both edits. Record the file count in the PR description — this is the only direct evidence for SC-016's "declarations alone" claim, and T130/T132 cannot supply it because they exercise the declarations they inject rather than the shipped ones (SC-016)

**Checkpoint**: User Story 1 is fully functional — a fresh install logs in with Google on both
platforms, on devices with and without Google's services, and reaches the main screen. Turning a
second provider on is a change to its own declarations plus its login path.

---

## Phase 4: User Story 2 - Returning with the same provider account (Priority: P2)

**Goal**: the same Google account always opens the same Yap account, with the provider's
descriptive data refreshed and never used to match.

**Independent Test**: log in, clear the session, log in again with the same Google account, and
confirm the same `users.id` comes back with no second row created.

**Dependency note**: this story extends the persistence adapter US1 creates (T054) rather than
standing alone. It is independently *testable* — its scenarios need no login screen — but not
independently implementable before US1.

- [X] T087 [US2] Write the failing Testcontainers test that two concurrent first logins for the same `(google, sub)` produce exactly one `users` row and one `provider_identities` row, coordinated by a latch, in `services/server/feature-auth/src/test/kotlin/app/yap/server/feature/auth/persistence/ProviderIdentityIntegrationTest.kt` (FR-007, docs/testing/003)
- [X] T088 [US2] Catch the unique violation and retry as a lookup so the losing request returns the winner's account, in `services/server/feature-auth/src/main/kotlin/app/yap/server/feature/auth/persistence/AuthPersistenceRepository.kt` — makes T087 pass (contracts/auth-api.md)
- [X] T089 [US2] Write the failing Testcontainers tests for repeat resolution — a second login refreshes `email`, `display_name`, and `avatar_url` without creating a second user, and a token omitting `name` or `picture` stores null and still logs in — in `services/server/feature-auth/src/test/kotlin/app/yap/server/feature/auth/persistence/ProviderIdentityIntegrationTest.kt`. Add alongside them the FR-008 case, two providers reporting the same address yielding two independent users: it is a **regression guard**, not a test-first step — it holds because nothing ever queries by email, so it passes as soon as it is written (FR-026, FR-008, SC-002)
- [X] T090 [US2] Refresh the three descriptive columns on every successful login and map absent claims to null, in `services/server/feature-auth/src/main/kotlin/app/yap/server/feature/auth/persistence/AuthPersistenceRepository.kt` — makes the first two tests of T089 pass

**Checkpoint**: User Stories 1 and 2 both work.

---

## Phase 5: User Story 3 - Session restored on relaunch (Priority: P2)

**Goal**: a returning user goes straight to the main screen with no login screen and no provider
confirmation; the session slides forward through the launch renewal, and only an explicit rejection
ever ends it.

**Independent Test**: log in, force-stop the app, reopen it — the main screen appears with no
login screen in between, offline included.

- [X] T091 [US3] Write the failing launch-decision tests — a stored session inside its window resolves to `LoggedIn` with no request made; one past `refreshTokenExpiresAtEpochSeconds` resolves to `LoggedOut` with no request made; and a device clock moved backwards or forwards changes only which of those two happens, never granting access to an expired session — in `apps/mobile/feature-auth/impl/src/commonTest/kotlin/app/yap/feature/auth/data/repository/DefaultAuthRepositoryTest.kt` (FR-011, SC-003, spec.md Edge Cases)
- [X] T092 [US3] Implement the local expiry check on the storage read path in `apps/mobile/feature-auth/impl/src/commonMain/kotlin/app/yap/feature/auth/data/repository/DefaultAuthRepository.kt` — makes T091 pass
- [X] T093 [US3] Hold the Android system splash until the first non-`Unknown` `AuthState` with `splashScreen.setKeepOnScreenCondition { … }` before `setContent`, in `apps/mobile/android-app/src/main/kotlin/app/yap/android/MainActivity.kt` (FR-024, research.md R12)
- [X] T094 [US3] Hold the iOS launch screen by rendering nothing until the first non-`Unknown` `AuthState`, in `apps/mobile/shared-app/src/commonMain/kotlin/app/yap/shared/app/App.kt` and the Xcode host (FR-024)
- [X] T095 [US3] Write the failing `DefaultAccessTokenProvider` tests over a stubbed data source — `400` and `401` clear storage and publish `LoggedOut`; no network, a timeout, a `5xx`, and a `429` each leave the stored session intact and logged in; concurrent callers serialize on the mutex; a token that arrived while waiting is returned without a second call — in `apps/mobile/feature-auth/impl/src/commonTest/kotlin/app/yap/feature/auth/data/DefaultAccessTokenProviderTest.kt` (FR-012, SC-014, research.md R4/R13)
- [X] T096 [US3] Implement `DefaultAccessTokenProvider` in `apps/mobile/feature-auth/impl/src/commonMain/kotlin/app/yap/feature/auth/data/DefaultAccessTokenProvider.kt` and bind it in `featureAuthModule()` — makes T095 pass
- [X] T097 [US3] Write the failing `DefaultRenewSessionUseCase` tests over stubbed storage and a stubbed `AccessTokenProvider` — a stored access token already expired, or expiring **within five minutes**, triggers exactly one refresh; one further out than that margin triggers none; no stored session triggers none; and every outcome is left to the provider, so the use case itself never clears storage — in `apps/mobile/feature-auth/impl/src/commonTest/kotlin/app/yap/feature/auth/domain/usecase/DefaultRenewSessionUseCaseTest.kt` (FR-032, research.md R15)
- [X] T098 [US3] Implement `DefaultRenewSessionUseCase` in `apps/mobile/feature-auth/impl/src/commonMain/kotlin/app/yap/feature/auth/domain/usecase/DefaultRenewSessionUseCase.kt` — read `accessTokenExpiresAtEpochSeconds`, compare it against the five-minute margin, and force the rotation by calling `AccessTokenProvider.getAccessToken(rejectedAccessToken = <stored token>)` so the seam's own rejection rules (T096) apply unchanged — and bind it in `featureAuthModule()` — makes T097 pass
- [X] T099 [US3] Write the failing launch-renewal test over a stubbed `RenewSessionUseCase` — it is invoked exactly once after auth state first resolves to `LoggedIn`, never while `Unknown`, never for `LoggedOut`, never before the launch decision, and never on the path that renders the main screen, so a hanging or offline renewal cannot delay it — in `apps/mobile/app-root/src/commonTest/kotlin/app/yap/app/root/LaunchRenewalTest.kt` (FR-032, research.md R15)
- [X] T100 [US3] Implement the launch renewal in `apps/mobile/app-root/src/commonMain/kotlin/app/yap/app/root/LaunchRenewal.kt`, firing `RenewSessionUseCase` once per launch after auth state resolves — makes T099 pass (FR-032, SC-009, SC-013)
- [X] T101 [US3] Write the failing `testApplication` route tests for `POST /v1/auth/refresh` — `200` on rotation with the old token now dead; an unknown, expired, or already-rotated session raises the unverifiable-confirmation `AuthFailure`; a malformed token value raises the malformed-input one — in `services/server/feature-auth/src/test/kotlin/app/yap/server/feature/auth/api/RefreshRouteTest.kt`; the statuses those become belong to T046 and T120 (contracts/auth-api.md)
- [X] T102 [US3] Add the `POST /v1/auth/refresh` route and `AuthService.rotate` in `services/server/feature-auth/src/main/kotlin/app/yap/server/feature/auth/api/AuthRoutes.kt` and `.../AuthService.kt` — makes T101 pass
- [X] T103 [US3] Write the failing Testcontainers tests for rotation — two concurrent rotations of one token yield one success and one refusal; a successful rotation pushes `expires_at` 90 days from now rather than from creation, including a rotation on the last day of the window; a session past `expires_at` is rejected — in `services/server/feature-auth/src/test/kotlin/app/yap/server/feature/auth/persistence/SessionRotationIntegrationTest.kt` (FR-025, SC-009, docs/testing/003)
- [X] T104 [US3] Implement rotation inside one transaction — match the stored hash, check `expires_at`, write the new hash and `rotated_at`, push `expires_at` forward by `REFRESH_TOKEN_TTL_SECONDS` — in `services/server/feature-auth/src/main/kotlin/app/yap/server/feature/auth/persistence/AuthPersistenceRepository.kt` — makes T103 pass
- [X] T105 [US3] Write the failing Testcontainers test that a successful login invalidates every earlier session for that account, leaving at most one valid session, in `services/server/feature-auth/src/test/kotlin/app/yap/server/feature/auth/persistence/SessionRotationIntegrationTest.kt` (FR-030, SC-013)
- [X] T106 [US3] Invalidate prior sessions inside the login transaction in `services/server/feature-auth/src/main/kotlin/app/yap/server/feature/auth/persistence/AuthPersistenceRepository.kt` — makes T105 pass
- [X] T107 [US3] Write the failing test that both expiry fields are copied verbatim from `SessionDto` and never computed or advanced on the device, in `apps/mobile/feature-auth/impl/src/commonTest/kotlin/app/yap/feature/auth/data/mapper/SessionMapperTest.kt` (FR-025, data-model.md)
- [X] T108 [US3] Enforce the copy-never-compute rule in `apps/mobile/feature-auth/impl/src/commonMain/kotlin/app/yap/feature/auth/data/mapper/SessionMapper.kt` — makes T107 pass

**Checkpoint**: relaunch, offline launch, expiry, renewal, rotation, and the one-session rule all
hold.

---

## Phase 6: User Story 4 - Cancelling or failing, then retrying (Priority: P3)

**Goal**: cancellation is silent, failure says one plain-language thing, and a second attempt is
always available without restarting the app.

**Independent Test**: cancel at Google's confirmation screen, then start and complete a second
attempt.

- [X] T109 [US4] Write the failing `LoginViewModel` test that cancellation returns the screen to idle with no news emitted, and that an attempt concluding with neither success nor an explicit failure does the same, in `apps/mobile/feature-auth/impl/src/commonTest/kotlin/app/yap/feature/auth/presentation/login/LoginViewModelTest.kt` (FR-013, SC-007)
- [X] T110 [US4] Write the failing `LoginViewModel` test that a failure emits exactly one `News.ShowMessage` and leaves login available, in `apps/mobile/feature-auth/impl/src/commonTest/kotlin/app/yap/feature/auth/presentation/login/LoginViewModelTest.kt` (FR-014)
- [X] T111 [US4] Write the failing `LoginViewModel` tests that no second attempt starts while one is in progress — activating the primary action again, choosing the same provider twice, and choosing a second provider — in `apps/mobile/feature-auth/impl/src/commonTest/kotlin/app/yap/feature/auth/presentation/login/LoginViewModelTest.kt` (FR-015)
- [X] T112 [US4] Write the failing `LoginViewModel` test that dismissing the provider sheet is treated as neither an error nor an attempt, in `apps/mobile/feature-auth/impl/src/commonTest/kotlin/app/yap/feature/auth/presentation/login/LoginViewModelTest.kt` (FR-016)
- [X] T113 [US4] Implement the cancellation, failure, duplicate-attempt, and dismissal event handling in `apps/mobile/feature-auth/impl/src/commonMain/kotlin/app/yap/feature/auth/presentation/login/LoginViewModel.kt` — makes T109–T112 pass (SC-005)
- [X] T114 [US4] Write the failing use-case test on virtual time that a provider call still unresolved after 60 seconds ends as `LoginOutcome.Cancelled` — silently, with no message — and that the provider coroutine is cancelled, in `apps/mobile/feature-auth/impl/src/commonTest/kotlin/app/yap/feature/auth/domain/usecase/DefaultLoginWithGoogleUseCaseTest.kt` (FR-013, SC-007, research.md R16)
- [X] T115 [US4] Wrap the provider call in `withTimeoutOrNull(60.seconds)` in `apps/mobile/feature-auth/impl/src/commonMain/kotlin/app/yap/feature/auth/domain/usecase/DefaultLoginWithGoogleUseCase.kt` — makes T114 pass
- [X] T116 [US4] Write the failing repository test that a provider cancellation maps to `LoginOutcome.Cancelled` and every other failure to `LoginOutcome.Failed`, in `apps/mobile/feature-auth/impl/src/commonTest/kotlin/app/yap/feature/auth/data/repository/DefaultAuthRepositoryTest.kt` (contracts/auth-api.md Client-side ports)
- [X] T117 [US4] Implement that translation in `apps/mobile/feature-auth/impl/src/commonMain/kotlin/app/yap/feature/auth/data/repository/DefaultAuthRepository.kt`, with the Android and iOS adapters signalling dismissal — including a dismissed browser tab — as a cancellation — makes T116 pass (FR-013, FR-029)
- [X] T118 [US4] Write the failing banner test — one `News.ShowMessage` renders exactly one banner; on the test clock it is gone four seconds later; while it is visible a tap lands on the primary action beneath it rather than on the banner; and a second message does not stack a second banner — in `apps/mobile/feature-auth/impl/src/commonTest/kotlin/app/yap/feature/auth/presentation/login/LoginScreenBannerTest.kt` (FR-023)
- [X] T119 [US4] Render `News.ShowMessage` as a banner that dismisses itself after four seconds and does not block the screen beneath it, in `apps/mobile/feature-auth/impl/src/commonMain/kotlin/app/yap/feature/auth/presentation/login/LoginScreen.kt` — makes T118 pass (FR-023)

**Checkpoint**: all four user stories are functional.

---

## Phase 7: Polish & Cross-Cutting Concerns

- [X] T120 [P] Add the application-level wiring test in `services/server/app/src/test/kotlin/app/yap/server/app/AuthEndpointsWiringTest.kt` over the real graph: all three doors — `POST /v1/auth/google`, `POST /v1/auth/google/code`, `POST /v1/auth/refresh` — sit inside the limited scope and return `429` past the threshold, and each returns `401` for a rejected confirmation or a rejected session through the production `StatusPages` mapping. This is where the feature's raised failures and `app`'s mapping are proven to meet; it is a wiring **regression guard** written after the routes exist, not a test-first step — T046 established the mapping and T048 the limiter (FR-027, SC-010, SC-015)
- [X] T121 [P] Add `.env.example` at the repository root listing every variable in `quickstart.md`, with the 90-day TTL and the 100-per-minute limit as defaults
- [X] T122 [P] Correct the two guides this feature contradicts, in the same PR that contradicts them (Principle V): in `docs/mobile/001-feature-boundaries.md` and `docs/mobile/003-dependency-injection.md`, the feature's Koin module function is public in **`impl`**, not in `api` — a function in `api` cannot bind declarations that are `internal` to `impl`, and `app-root` depends on `impl` precisely to load it. State the rule once and let the other guide reference it
- [X] T123 [P] Update `README.md` for the `feature-auth` `api`/`impl` split and the new Android, iOS, and server entry points
- [X] T124 [P] Update `apps/mobile/ios-app/README.md` with the Xcode host steps — GoogleSignIn via SPM, the reversed-client-ID URL scheme, forwarding `application(_:open:options:)`, and passing `GoogleCredentialProvider` into `initIosKoin`
- [X] T125 Run `./gradlew build` — compilation, all tests, and Detekt across every module; if Docker is not running, say plainly that the Testcontainers suite did not run
- [X] T126 Run `./gradlew :apps:mobile:shared-app:compileKotlinIosSimulatorArm64` for the KMP boundary change
- [ ] T127 Walk every scenario in `quickstart.md` by hand, including the emulator image without Google Play services (SC-012), the repeated-launch no-flash check (US3.3), the second-device sign-out (SC-013), the 60-second unresolved attempt (SC-007), the offline / `5xx` / `429` checks that must not log anyone out (SC-014), and the by-hand timing of a first login, which is the only measurement of SC-001 this feature has
- [ ] T128 Record in the PR description the judgement calls this feature makes (Principle V): the three in plan.md's Complexity Tracking — the platform-capability ports in `core-common`, the absence of a `Navigator`, and the client-generated nonce — plus two smaller ones: `News.ShowMessage` carries FR-023's self-dismissing banner where `docs/mobile/presentation/002-ui-compose.md` names the one-shot `ShowSnackbar` (the rule is the same, only the name differs, and the banner is transient rather than resubscription-surviving), and the design project gains the legal line FR-028 requires, which it does not carry today

---

## Dependencies & Execution Order

### Phase dependencies

- **Setup (Phase 1)**: no dependencies — start immediately. T001 precedes T002–T005.
- **Foundational (Phase 2)**: depends on Setup — **blocks every user story**.
- **US1 (Phase 3)**: depends on Foundational. No dependency on US2–US4.
- **US2 (Phase 4)**: depends on Foundational and on US1's `AuthPersistenceRepository` (T054).
- **US3 (Phase 5)**: depends on Foundational. Independent of US2. Shares `DefaultAuthRepository`
  and `AuthPersistenceRepository` with US1, so it follows US1 in a single-developer sequence.
- **US4 (Phase 6)**: depends on Foundational and on US1's `LoginViewModel` (T074), use case (T070),
  repository login path (T069), and `LoginScreen` (T081).
- **US1's FR-033 group (T129–T134)**: follows T086, and T133 renames `LoginWithGoogleUseCase` to a
  provider-neutral contract. T085, T114, and T115 still name it as it is today; whichever runs
  second reads the new name. Run T129–T134 before T113–T119 to keep that one-way, since US4 opens
  more files against that contract than the rename touches.
- **Polish (Phase 7)**: T120 needs all three routes, so it follows US3. T125–T127 come last.

### Within each story

- Every test task precedes the implementation task it names, and must be run red first. The four
  exceptions are labelled in place: the FR-008 case inside T089, the two Koin `verify()` wiring
  guards (T031, T086), and the application-level wiring guard (T120).
- The screen tests (T077–T080) and the banner test (T118) precede the composables they cover, so
  their first red run is a compilation failure naming a composable that does not exist yet. That is
  the intended reason; a test that compiles and passes on its first run has established nothing.
- Models and entities precede services; services precede routes and view models.
- Server route tests need the verifier, the persistence adapter, and the service beneath them.
- The `app`-level error mapping (T046, T047) precedes every route test that raises a failure, so
  the statuses those failures become are already proven when the routes arrive.

### Parallel opportunities

- Setup: T002, T003, T006–T012 all touch different files.
- Foundational: the four declaration groups run together — T013–T014, T015–T017, T018–T021,
  T022–T024 — as do T042–T044 on the server.
- US1: the server track (T050–T061), the platform adapter track (T062–T065), and the presentation
  track (T071–T086) are three developers' worth of independent work once T049 lands. Within the
  presentation track, T077–T080 are four independent test files. The tracks **converge at the end**:
  T085 binds the concrete `AndroidGoogleCredentialProvider` (T063) and `DefaultLoginWithGoogleUseCase`
  (T070), and T086 verifies that graph resolves, so those last two wait on the adapter track and on
  the data-and-domain group (T066–T070) instead of running beside them.
- After US1's checkpoint, US2, US3, and US4 overlap only where their files do not. Two collisions
  decide the split: `DefaultAuthRepository.kt` and its test belong to both US3 (T091–T092) and US4
  (T116–T117), and `AuthPersistenceRepository.kt` belongs to both US2 (T088, T090) and US3 (T104,
  T106). Each of those two files takes one owner at a time — sequence those tasks rather than
  running them side by side. What genuinely runs in parallel is US3's server work (T101–T106)
  against US4's client work (T109–T115, T118–T119); US3's own mapper pair (T107–T108) is mobile
  rather than server, and collides with neither.
- Polish: T121–T124 are independent; T120 waits on all three routes.

## Parallel Example: User Story 1

```bash
# Three tracks in parallel once Foundational is complete:
Task: "T050 Failing GoogleIdentityVerifier tests in services/server/feature-auth/src/test/..."
Task: "T062 Failing AndroidGoogleCredentialProvider tests in apps/mobile/feature-auth/impl/src/androidHostTest/..."
Task: "T071 Failing LoginUiStateMapper tests in apps/mobile/feature-auth/impl/src/commonTest/..."
```

## Implementation Strategy

### MVP (User Story 1 only)

1. Phase 1 Setup → 2. Phase 2 Foundational → 3. Phase 3 US1 → 4. **stop and validate**: fresh
install logs in with Google on Android and iOS, including a device without Google's services, and
reaches the main screen.

### Incremental delivery

1. Setup + Foundational → the app launches and the server migrates and starts.
2. + US1 → login works (MVP).
3. + US2 → returning users keep their account.
4. + US3 → relaunch, offline launch, the launch renewal, and the sliding 90-day window.
5. + US4 → cancellation, the 60-second bound, and failure recovery.
6. + Polish → endpoint wiring, the guide correction, docs, and the two verification commands.

Release to iOS users waits on the Sign in with Apple follow-up; release to any user waits on both
legal destinations being configured (spec.md Assumptions, FR-028).

## Notes

- `[P]` means a different file and no dependency on an incomplete task.
- The Compose UI tests (T077–T080, T118) cover only what a mapper or view-model test cannot prove,
  per `docs/testing/001-structure.md`, and each sits ahead of the composable it covers rather than
  in Polish.
- The absence of a login-screen flash (FR-024) is a timing property no unit test observes; T127 is
  the only proof (research.md R12).
- The iOS host is outside Gradle: T038, T065, T094, and the iOS half of T127 are verified from
  Xcode by hand.
- Commit after each task or logical group; stop at any checkpoint to validate a story on its own.
