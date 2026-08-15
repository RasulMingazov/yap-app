# Implementation Plan: Login Screen

**Branch**: `feature/001-login-screen` | **Feature ID**: `001-login-screen`
| **Refreshed**: 2026-08-15 | **Spec**: [spec.md](spec.md)

The branch carries the feature number after the kind prefix `CLAUDE.md` requires. spec-kit locates
the feature through `.specify/feature.json`, never through the branch name.

## Summary

The whole authentication slice, end to end: a Compose Multiplatform login screen matching
`screen_login.dc.html`, working Google login on Android and iOS, and a Ktor server that verifies
the Google credential, resolves or creates the account, and issues a session that survives restart.
Apple and T-ID appear in the provider selection destination and report that logging in through them
is coming soon.

The repository was scaffolding only, so this feature also builds the first vertical: Android
`MainActivity`, the shared `App()` root with a Navigation 3 back stack driven by session state, an
Xcode host embedding `YapShared.framework`, and the server process itself.

Technical shape:

- `feature-auth` splits into `api`/`impl`. Google credential retrieval is one port declared in
  `api`. On Android it is Credential Manager with a PKCE browser fallback for devices without
  Google's services; on iOS it is supplied from Swift. The two Android paths return different
  artefacts — an ID token or an authorization code — which is why the server exposes two Google
  doors onto one account resolution.
- The session lives in Keystore-encrypted DataStore on Android and the Keychain on iOS, behind
  `SessionStore`, which is the single owner of the stored record and of the published
  `AuthSessionState`. It reaches `core-network` through the `AccessTokenProvider` seam.
- `app-root` observes session state and swaps the back stack's base, so no view model navigates on
  login success. The state is tri-valued — unknown, logged out, logged in — and the platform splash
  stays up while it is unknown, which is what keeps the login screen from flashing past a
  logged-in user.
- Provider knowledge lives in the domain layer: `ObserveAuthProvidersUseCase` is the roster, a
  `ProviderLogin` port collected with Koin's `getAll` gives one `LoginUseCase(provider)`, and
  provider selection is a real Navigation 3 destination rendered as a bottom sheet with its own
  state, mapper, and view model.
- Two pieces of shared infrastructure appear because this is the first feature that needs them: a
  `Navigator` contract in `core-common` and a `BottomSheetSceneStrategy` in `core-design`, which
  Navigation 3 ships at no version. One dependency moves against R11's pinning rule: Navigation 3
  rises to the pre-release carrying the official result API.
- HTTP outcomes are typed once, in `core-network`: `ApiClient` returns `ApiResult` with an
  `ApiError` of `Rejected` / `Unauthorized` / `Unavailable` / `Malformed`, and the error body is a
  shared contract (`shared/contract/common`). Features consume values, never status codes.

One rule shapes the session code on both sides and is easy to get silently wrong: **the server is
the only thing that can start or end a session.** A successful token rotation is the only event
that renews the 90-day window (FR-026), and only an explicit rejection — `ApiError.Rejected` or
`ApiError.Unauthorized` from refresh, or a local expiry already passed — signs the user out. An
unreachable server, a `5xx`, or a rate-limit `429` maps to `ApiError.Unavailable` and leaves the
stored session alone (FR-025, SC-014). The decision lives in one place,
`DefaultAccessTokenProvider`.

Two consequences of that rule need their own mechanism, or they stay aspirational. First, nothing
else in this feature contacts the server after login — all three endpoints are the unauthenticated
front door — so without a deliberate refresh every session would quietly age out at 90 days and no
user would discover a session superseded on another device. `app-root` therefore runs
`LaunchSessionRefresh` once session state resolves to logged-in, calling `RefreshSessionUseCase`; the
five-minute margin is applied inside `impl`, in `DefaultAuthSessionRepository`, because the stored
expiry dates are `internal` there. Second, an attempt that never comes back needs an end:
`DefaultLoginUseCase` bounds every provider call at 60 seconds and reports the expiry as a
cancellation.

## Technical Context

**Language/Version**: Kotlin 2.4.0 — `commonMain`/`androidMain`/`iosMain` for the client, JVM 17
for the server; Swift for the iOS host.

**Primary Dependencies**: Compose Multiplatform 1.11.1 with Material3 1.9.0, Koin 4.2.2, AndroidX
Lifecycle 2.11.0, Ktor 3.5.0 (client and server), kotlinx.serialization 1.11.0, kotlinx-coroutines
1.11.0, Exposed 0.61.0, Flyway 12.0.3, HikariCP, auth0 java-jwt 4.5.0. Navigation 3 is split into
`navigation3-runtime` **1.2.0-alpha04** and `navigation3-ui` **1.2.0-alpha02**
([research.md](research.md) R19). Added by this feature: `androidx.credentials` +
`com.google.android.libraries.identity.googleid`, `net.openid:appauth`, `androidx.datastore`,
`androidx.core:core-splashscreen` (Android only), `com.auth0:jwks-rsa`, `ktor-server-status-pages`,
`ktor-server-rate-limit`, `ktor-client-cio`, `ktor-client-mock` (tests), GoogleSignIn iOS SDK via
Swift Package Manager.

**Storage**: PostgreSQL 17 on the server (`users`, `provider_identities`, `sessions`, owned by
`services/server/feature-auth` with forward-only Flyway migrations). On device: Android Keystore
AES-GCM over DataStore Preferences; iOS Keychain.

**Testing**: `kotlin-test`, `kotlinx-coroutines-test`, `stubcall` 0.1.0, `runViewModelTest` from
`core-test`, Koin `verify()` plus real resolution, `ktor-client-mock` for `ApiClient`,
`ktor-server-test-host` for route tests, Testcontainers PostgreSQL 17 for the persistence behaviour
`docs/testing/003-backend-integration.md` requires a real database to prove. Compose UI tests run on
the host through Robolectric in `androidHostTest`.

**Target Platform**: Android `minSdk` 24 / `compileSdk` 37, iOS (`iosArm64`, `iosSimulatorArm64`),
JVM 17 server.

**Design source**: Claude Design project `0c49e08b-d7ab-4cd3-88be-8483024790e5`,
`screen_login.dc.html` — palette, sheet chrome, provider marks, message geometry and timing.

**Constraints**: Russian copy only. Both themes. No analytics. Apple never shown on Android. Layout
intact at 320 dp and 200% font scale together (SC-008). All three unauthenticated endpoints rate
limited at 100 requests per minute per IP (FR-033). Sessions renew only through the server and end
only on an explicit rejection (FR-025, FR-026); the launch refresh — fired only when the access
token is expired or within five minutes of expiring — is the only thing reaching the server after
login (FR-028). No attempt runs longer than 60 seconds (FR-029). No colour literal in login UI code;
no UI resource in domain or data; no explanatory comment in any Kotlin source.

**Scale/Scope**: One login screen, one selection destination, three HTTP endpoints, three tables,
two platform credential adapters.

## Constitution Check

| Principle | Gate | Status |
| --- | --- | --- |
| I. Feature-First Module Boundaries | `feature-auth` is exactly `api` + `impl`; `api` depends only on `core-*`; declarations default to `internal` | PASS — `api` publishes `AuthNavKey`, the use-case contracts, their entities, and `GoogleCredentialProvider`; `featureAuthModule()` is public in `impl`, because a function in `api` cannot bind declarations `internal` to `impl`. `core-common` gains `Navigator`, `core-design` gains `BottomSheetSceneStrategy`; neither names a feature type |
| II. Layered Dependencies | `presentation → domain ← data`; platform source sets carry narrow adapters; view models report intent rather than navigating | PASS — both view models depend on use cases and `Navigator` only; login success is published as session state and `app-root` swaps the back stack's base; Credential Manager and Keychain code stays in platform source sets; `ProviderLogin` is a domain port; `AuthProvider` carries a type and two booleans |
| III. Test-First for Behavior Change | A failing focused test precedes each behavior change | PASS — behavioural units are tested at the boundary that owns them ([research.md](research.md) R10). Dependency bumps, file moves, and the comment cleanup carry no test, which is the constitution's own carve-out |
| IV. Wire Contracts at the Boundary | Wire types live in `shared/contract/*`, carry the `Dto` suffix, and are mapped at the repository and route boundaries | PASS — `GoogleCredentialsDto`, `GoogleAuthorizationCodeDto`, `RefreshCredentialsDto`, `SessionDto` in `shared/contract/auth`; `ErrorResponseDto` and `ApiErrorCode` in `shared/contract/common`, shared by the server's `StatusPages` mapping and the client's `ApiClient` |
| V. Documented Rules Govern | No silent exceptions to the guides | PASS — judgement calls are recorded in Complexity Tracking; guides corrected in this PR: the two placing a feature's Koin module function in `api`, `docs/mobile/001-feature-boundaries.md` on the nested `ui` package, and `docs/mobile/003-dependency-injection.md` on the mutable back-stack tail, the `Navigator` binding, scene strategies, and entry decorators. `docs/001-code-conventions.md` gains the declaration-order rule the data layer follows, and `config/detekt/detekt.yml` excludes guard clauses from `ReturnCount` so the rule and the convention agree |

## Project Structure

```text
gradle/libs.versions.toml                                    # navigation3 split into runtime + ui

shared/contract/auth/…/app/yap/contract/auth/                # GoogleCredentialsDto, GoogleAuthorizationCodeDto,
                                                             # RefreshCredentialsDto, SessionDto
shared/contract/common/…/app/yap/contract/common/            # ErrorResponseDto, ApiErrorCode

apps/mobile/core-common/src/
├── commonMain/…/navigation/Navigator.kt
└── commonMain/…/platform/{MotionPreferences,Platform}.kt    # expect, with per-platform adapters

apps/mobile/core-network/src/commonMain/…/core/network/
├── ApiClient.kt, ApiRequests.kt, ApiResult.kt               # typed HTTP outcomes
└── NetworkClient.kt, NetworkModule.kt, AccessTokenModifier.kt

apps/mobile/core-design/src/commonMain/…/core/design/
├── navigation/BottomSheetSceneStrategy.kt                    # scene + design chrome
└── theme/{YapColors,YapTheme}.kt

apps/mobile/feature-auth/
├── api/…/feature/auth/api/
│   ├── AuthNavKey.kt                                         # Login + SelectAuthProvider
│   ├── GoogleCredentialProvider.kt, LoginCancelledException.kt
│   ├── entity/                                               # AuthProvider, AuthProviderType,
│   │                                                         # AuthSessionState, LegalLinks,
│   │                                                         # LoginOutcome, UserId
│   └── usecase/                                              # GetLegalLinks, Login,
│                                                             # ObserveAuthProviders,
│                                                             # ObserveAuthSessionState, RefreshSession
└── impl/src/
    ├── commonMain/composeResources/                          # provider drawables + strings
    ├── commonMain/…/feature/auth/
    │   ├── data/{CurrentTime,DefaultAccessTokenProvider,SessionStore}.kt
    │   ├── data/{identity,local,mapper,remote}/
    │   ├── data/repository/{DefaultAuthSessionRepository,DefaultGoogleAuthRepository}.kt
    │   ├── domain/provider/{ProviderLogin,GoogleProviderLogin}.kt
    │   ├── domain/repository/{AuthSessionRepository,GoogleAuthRepository}.kt
    │   ├── domain/usecase/                                   # one Default per api contract
    │   ├── presentation/common/{AuthProviderUi,AuthProviderUiMapper,AuthResultKeys}.kt
    │   ├── presentation/login/{LoginViewModel,LoginUiStateMapper,LoginNewsMapper}.kt
    │   ├── presentation/login/ui/{LoginScreen,LoginSnackbarHost,LegalLine,LoginTestTags}.kt
    │   ├── presentation/selectprovider/{SelectAuthProviderViewModel,…UiStateMapper}.kt
    │   ├── presentation/selectprovider/ui/{SelectAuthProviderScreen,…TestTags}.kt
    │   └── di/FeatureAuthModule.kt                           # public; app-root loads it from impl
    ├── androidMain/                                          # Credential Manager + AppAuth, Keystore+DataStore
    └── iosMain/                                              # Keychain session storage

apps/mobile/app-root/src/commonMain/…/app/root/
├── App.kt                                                    # onBack, sceneStrategies, entryDecorators
├── LaunchSessionRefresh.kt
├── di/{AppModules,AppRootModule}.kt                          # RootBackStack is a single, bound as Navigator
└── navigation/{RootBackStack,RootNavKey,MainPlaceholderScreen}.kt

apps/mobile/shared-app/            # App(), initAndroidKoin, initIosKoin, mainViewController
apps/mobile/android-app/           # MainActivity, splash, OAuth redirect intent filter
apps/mobile/ios-app/               # Xcode host: YapShared.framework, GoogleSignIn SPM

services/server/feature-auth/src/main/
├── kotlin/…/server/feature/auth/{AuthService,api,identity,model,persistence}
└── resources/db/migration/V1__auth.sql

services/server/app/src/main/kotlin/…/server/app/
└── Application.kt, ErrorMapping.kt                           # lifecycle, plugins, health, graph
```

**Structure Decision**: The module layout in `README.md` is already correct; this feature fills it
in. The one module change is splitting `:apps:mobile:feature-auth` into `api` and `impl`, plus the
new `:shared:contract:common` for the wire types both sides share. Inside `impl`, presentation is
two slices — `login` and `selectprovider` — each with its state code at the top and its rendering in
a nested `ui` package, alongside a `common` package for what both share.

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
| --- | --- | --- |
| Platform-capability ports (`Platform`, `MotionPreferences`, `ActivityProvider`) in `core-common` rather than the feature | The roster needs the running platform to hide Apple on Android (FR-004) and the login mapper needs the reduced-motion preference (FR-045); both are process-wide facts every later screen needs | Keeping them in `feature-auth/impl` would make the second feature duplicate the `expect`/`actual` pair or reach into an unrelated feature's `impl` |
| Login nonce generated on the client rather than issued by the server | Google's Android guidance recommends binding a nonce into the ID token request, and the client value still prevents a token minted for one in-flight request being swapped into another | A server-issued challenge is stronger but adds a third endpoint and a round trip before the user has done anything (R5 records the upgrade path) |
| A pre-release navigation dependency, against R11's pinning rule | The official result API exists in no stable release, and the requester chose it over a project-owned carrier | A feature-owned channel plus two use cases was rejected as an invented mechanism |
| Two catalogue references for one library | `navigation3-ui` 1.2.0-alpha02 declares runtime 1.2.0-alpha04, and JetBrains publishes no runtime artefact | One shared version would pin the runtime below what the UI was built against or rely on conflict resolution |
| A `Navigator` in `core-common` | The selection destination is the first consumer, and the presentation guide already prescribes the contract | Keeping the sheet as a `UiState` flag is what the requester asked to remove and what the UI guide forbids |
| `core-common` gains `navigation3-runtime` | `Navigator.navigate(key: NavKey)` puts `NavKey` on the module's API surface | A `String` or feature-local key type loses type safety |
| A hand-written bottom-sheet scene strategy in `core-design` | Navigation 3 ships none at any version; `app-root` cannot host it because features may not depend on it | Leaving the sheet inside the login screen fails FR-035 |
| A feature-owned snackbar host | Material 3's transition is private and marked TODO upstream; the design requires upward exit and 2600 ms | Wrapping the stock host leaves its fade running under the slide |
| `isMonochrome` on the row model | The design's marks are not uniform — two carry brand colours, one follows the theme | Branching on the provider inside the composable is what the UI guide forbids |
| `AuthProviderUiMapper` shared by two slices rather than owned by the selection mapper | The login screen needs the provider's name for the coming-soon message after the sheet has closed | A provider `when` in `LoginViewModel` would break FR-011 |
| A second view model in the feature | The selection sheet is its own destination with its own lifecycle | Keeping the state in `LoginViewModel` fails SC-020 |
| An explicit shared result key rather than the result bus's inferred type key | The bus keys by type name; an inferred key would deliver the result where nothing is listening | Relying on every call site writing the type argument correctly is a rule the compiler cannot enforce |
| `Lazy<AuthRemoteDataSource>` in `DefaultAccessTokenProvider` | `NetworkClient` → `AccessTokenProvider` → `AuthRemoteDataSource` → `NetworkClient` is a real cycle, closed by deferring one Koin lookup | Breaking it inside `coreNetworkModule` was reviewed and dropped: it moves a feature's wiring problem into a `core-*` module |

Two smaller departures are recorded in the PR description: `News.ShowMessage` carries the transient
message where `docs/mobile/presentation/002-ui-compose.md` names the one-shot `ShowSnackbar` (same
rule, different name), and the design project gains the legal line FR-051 requires.
