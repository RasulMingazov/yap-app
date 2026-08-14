# Implementation Plan: Login Screen

**Branch**: `feature/001-login-screen` | **Feature ID**: `001-login-screen` | **Date**: 2026-08-13
| **Spec**: [spec.md](spec.md)

The branch carries the feature number after the kind prefix `CLAUDE.md` requires, so it reads as
the feature id it belongs to. The two are still separate things: spec-kit locates the feature
through `.specify/feature.json`, never through the branch name, and the field labelled `BRANCH` in
`check-prerequisites.sh` and `setup-plan.sh` output is the feature id — the spec directory's
basename — which is why it prints without the prefix.

**Input**: Feature specification from `/specs/001-login-screen/spec.md`

## Summary

Deliver the whole authentication slice end to end: a Compose Multiplatform login screen that
matches `screen_login.dc.html`, working Google login on Android and iOS, and a Ktor server
that verifies the Google ID token, resolves or creates the account, and issues a session that
survives restart. Apple and T-ID appear in the provider sheet and report that they are not
available yet.

The repository is scaffolding only — no entry points, no feature source, no wire DTOs, no
server routes — so this feature also builds the first vertical: Android `MainActivity`, the
shared `App()` root with a Navigation 3 back stack driven by auth state, an Xcode host that
embeds `YapShared.framework`, and the server process itself.

Technical shape: `feature-auth` splits into `api`/`impl` as the mobile guides require. Google
credential retrieval is a single port declared in `api`. On Android it is implemented with
Credential Manager and falls back on its own to a PKCE browser flow when the device has no Google
services, so a de-Googled phone can still log in; on iOS it is supplied from Swift through a
`shared-app` entry helper. The two Android paths return different artefacts — an ID token or an
authorization code — which is why the server exposes two Google doors onto one account
resolution. The session
lives in Keystore-encrypted DataStore on Android and the Keychain on iOS, and is published to
`core-network` through the existing `AccessTokenProvider` seam. `app-root` observes auth state
and swaps the back stack, so no view model navigates on login success. Auth state is
tri-valued — unknown, logged out, logged in — and the platform splash screen stays up while it is
unknown, which is what keeps the login screen from flashing past a logged-in user on every
launch.

One rule shapes the session code on both sides and is easy to get silently wrong: **the server is
the only thing that can start or end a session.** A successful token rotation is the only event
that renews the 90-day window (FR-025), and only an explicit rejection — a `400` or `401` from
refresh, or a local expiry already passed — signs the user out. An unreachable server, a `5xx`, or
a rate-limit `429` leaves the stored session alone and is retried later (FR-012, SC-014), so
losing signal never costs a user their account. The decision lives in one place, the
`AccessTokenProvider` implementation, rather than at every call site.

Two consequences of that rule need their own mechanism, or they stay aspirational. First, **nothing
else in this feature contacts the server after login** — all three endpoints are the
unauthenticated front door — so without a deliberate renewal every session would quietly age out
at 90 days and no user would ever discover a session superseded on another device. `app-root`
therefore fires a launch renewal once auth state has resolved to logged-in, by calling
`RenewSessionUseCase` — a contract in `feature-auth/api`, because the stored token and both expiry
dates are `internal` to `impl` and `app-root` has no business reading them. The use case owns the
condition: it renews only when the stored access token has expired or is within **five minutes** of
expiring (FR-032), and delegates the call itself to the `AccessTokenProvider` that already owns
refresh and the sign-out decision. It runs *after* the local launch decision, so FR-011's
"no request before deciding" rule is untouched and an offline launch still reaches the main screen.
Second, an attempt that never comes back needs an end: `LoginWithGoogleUseCase` bounds the
provider call at 60 seconds and reports the expiry as a cancellation, which is what turns SC-007's
number into something a test can observe.

## Technical Context

**Language/Version**: Kotlin 2.4.0 — `commonMain`/`androidMain`/`iosMain` for the client, JVM 17
for the server; Swift for the iOS host application.

**Primary Dependencies**: Compose Multiplatform 1.11.1 with Material3 1.9.0, Navigation 3 1.1.1,
Koin 4.2.2, Ktor 3.5.0 (client and server), kotlinx.serialization 1.11.0, Exposed 0.61.0,
Flyway 12.0.3, HikariCP, auth0 java-jwt 4.5.0. New: `androidx.credentials` +
`com.google.android.libraries.identity.googleid`, `net.openid:appauth` (Android only),
`androidx.datastore` (Android only), `androidx.core:core-splashscreen` (Android only),
`com.auth0:jwks-rsa`, `ktor-server-status-pages`, `ktor-server-rate-limit`, `ktor-client-cio` (the
server's own client, for Google's token endpoint and key set), GoogleSignIn iOS SDK via Swift
Package Manager.

**Storage**: PostgreSQL 17 on the server (`users`, `provider_identities`, `sessions`, owned by
`services/server/feature-auth` with forward-only Flyway migrations). On device: Android Keystore
AES-GCM over DataStore Preferences; iOS Keychain.

**Testing**: `kotlin-test`, `kotlinx-coroutines-test`, `stubcall` 0.1.0, `runViewModelTest` from
`core-test`, Koin `verify()`. New: `ktor-server-test-host` for route tests and Testcontainers
PostgreSQL 17 for the persistence behavior that `docs/testing/003-backend-integration.md`
requires a real database to prove.

**Target Platform**: Android `minSdk` 24 / `compileSdk` 37, iOS (`iosArm64`, `iosSimulatorArm64`),
JVM 17 server.

**Project Type**: Kotlin Multiplatform mobile client plus modular Kotlin/JVM server in one Gradle
monorepo, with platform-neutral wire contracts between them.

**Performance Goals**: Every login attempt resolves to success, idle, or a message within 60
seconds (SC-007). The marquee and rolling-topic animations run without dropping frames, and stop
entirely under reduced motion.

**Constraints**: Russian copy only. Both light and dark themes. No analytics — SC-001's 15 seconds
is observed by hand, not measured. Apple never shown on Android. Layout intact at 320 dp of width
and 200% font scale, including both at once (SC-008). All three unauthenticated endpoints rate
limited to 100 requests per minute per originating IP (FR-027). Sessions renew only through the
server and end only on an explicit rejection (FR-012, FR-025), and the launch renewal — fired only
when the stored access token is expired or within five minutes of expiring — is the only thing that
reaches the server after login (FR-032). No login attempt runs longer than 60 seconds (FR-013,
SC-007).

**Scale/Scope**: One screen, one bottom sheet, three HTTP endpoints, three tables, two platform
credential adapters.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Gate | Status |
| --- | --- | --- |
| I. Feature-First Module Boundaries | `feature-auth` is exactly `api` + `impl`; `api` depends only on `core-*`; declarations default to `internal` | PASS — the split is created by this feature (`:apps:mobile:feature-auth:api`, `:apps:mobile:feature-auth:impl`); `api` publishes `AuthNavKey`, the use-case contracts, their entities, and `GoogleCredentialProvider`, while `featureAuthModule()` is public in `impl`, because a function in `api` cannot bind declarations that are `internal` to `impl` — which is also why `docs/mobile/001-feature-boundaries.md` has `app-root` depend on both modules. The two guides that place the module function in `api` are corrected by T122 in this PR rather than worked around |
| II. Layered Dependencies | `presentation → domain ← data`; platform source sets carry narrow adapters; view models report intent rather than navigating | PASS — `LoginViewModel` depends on use cases only; login success is published as auth state and `app-root` swaps the back stack; Credential Manager and Keychain code stays in `androidMain`/`iosMain` |
| III. Test-First for Behavior Change | A failing focused test precedes each behavior change | PASS — `tasks.md` orders every behavioral unit test before its implementation; PostgreSQL-dependent invariants go to Testcontainers, not a fake |
| IV. Wire Contracts at the Boundary | Wire types live in `shared/contract/auth`, carry the `Dto` suffix, and are mapped at the repository and route boundaries | PASS — `GoogleCredentialsDto`, `GoogleAuthorizationCodeDto`, `RefreshCredentialsDto`, `SessionDto`; mobile maps in `data/mapper`, server maps in its route layer |
| V. Documented Rules Govern | No silent exceptions to the guides | PASS — all three judgement calls (`core-common` gains platform-capability ports; no `Navigator` type of any kind yet; the login nonce is generated on the client) are recorded in Complexity Tracking, and T128 carries them into the PR description together with the two smaller departures it names |

Re-checked after Phase 1 design: unchanged, all PASS. Re-checked again after the second
`/speckit-clarify` pass added FR-031 and tightened FR-012, FR-025, FR-026, and FR-027: still all
PASS. Those clarifications changed rules inside boundaries this plan already draws — the sign-out
decision stays in `AccessTokenProvider`, verification stays in the server feature's `identity`
package, and the rate limit stays in `app` beside the other shared plugins — so no module gained a
dependency and no new layer was introduced.

Re-checked a third time after `/speckit-analyze` added FR-032 and the 60-second bound in FR-013:
still all PASS. The launch renewal is triggered by `app-root`, which already collects auth state,
and executed by the `AccessTokenProvider` that already owns the refresh decision; the timeout sits
inside `LoginWithGoogleUseCase`. Neither introduces a module, a layer, or a dependency edge. The
same pass moved two things back inside the rules rather than around them: `featureAuthModule()` is
public in `impl` rather than in `api`, which is what Principle I's module graph actually permits,
and the three `GOOGLE_*_CLIENT_ID` values are read by `AppConfigLoader` rather than by `app`,
because the constitution fixes `core-config` as the owner of environment loading. The
provider-specific *type* still lives in the feature, as `AuthConfig`'s KDoc requires — `core-config`
loads the values, `feature-auth` gives them meaning.

Re-checked a fourth time after the second `/speckit-analyze` pass: still all PASS, and two of its
findings were Principle violations fixed here rather than argued away. Principle I: the launch
renewal reached for `SessionLocal` and both expiry dates, which are `internal` to `impl`, so the
condition moved behind a `RenewSessionUseCase` contract in `api` and `app-root` now owns only the
trigger. Principle III: three Compose UI tests sat in Phase 7, *after* the Phase 3 code they cover,
and the four-second banner (FR-023) had no test at all — the UI tests moved into US1 ahead of their
implementations, and the banner gained one. No module, layer, or dependency edge changed.

Re-checked a fifth time after the third `/speckit-analyze` pass: still all PASS, and it produced
one repository change and one new task. Principle I: `app-root` held `RootNavKey.Auth`, a key no
module composed, while the feature registered its own `AuthNavKey.Login` — the scaffolded key is
deleted, the logged-out root is the feature's, and `app-root` keeps only `RootNavKey.Main`, which
`appRootModule()` registers so `koinEntryProvider()` can resolve it. Principle V: two guides state
that a feature's Koin module function is public in `api`, which a real `api`/`impl` split makes
impossible — a function in `api` cannot bind declarations that are `internal` to `impl`. Rather
than working around them silently, T121 corrects both guides in this PR, which is what "one of the
two is changed in the same PR" requires. Principle III gained one task: the Exposed persistence
adapter was the single implementation in the feature written before any test that covered it, so a
Testcontainers test for resolve-or-create and session insertion now precedes it.

Re-checked a sixth time after the fourth `/speckit-analyze` pass: still all PASS, with one more
Principle III gap closed. The `AuthFailure` → HTTP mapping is project-owned behavior that lives in
`app`, and no test reached it: the feature's route tests asserted `401` and `503` in a
`testApplication` that — by the same argument the tasks already made for `429` — assembles the
feature's routes without `app`'s plugins. The boundary is now drawn once and stated in tasks.md:
a route test inside `feature-auth` asserts the failure the route **raises** and the rows it writes,
while every status code is proven in `app` — the mapping by a failing test that precedes it, and the
three doors end to end by the wiring guard in Polish. No module gained a dependency; the fix is
that each side now tests what it owns.

## Project Structure

### Documentation (this feature)

```text
specs/001-login-screen/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output
│   └── auth-api.md
├── checklists/
│   └── requirements.md
└── tasks.md             # Phase 2 output (/speckit-tasks — NOT created here)
```

### Source Code (repository root)

```text
shared/contract/auth/src/commonMain/kotlin/app/yap/contract/auth/
├── GoogleAuthorizationCodeDto.kt
├── GoogleCredentialsDto.kt
├── RefreshCredentialsDto.kt
└── SessionDto.kt

apps/mobile/core-common/src/
├── commonMain/kotlin/app/yap/core/common/platform/
│   ├── MotionPreferences.kt          # expect
│   └── Platform.kt                   # expect
├── androidMain/kotlin/app/yap/core/common/platform/   # Settings.Global, ActivityProvider
└── iosMain/kotlin/app/yap/core/common/platform/       # UIAccessibility

apps/mobile/feature-auth/
├── api/src/commonMain/kotlin/app/yap/feature/auth/api/
│   ├── AuthNavKey.kt
│   ├── GoogleCredentialProvider.kt   # implemented on Android and by Swift on iOS
│   ├── entity/                       # AuthState, AuthProvider, LoginOutcome
│   └── usecase/                      # ObserveAuthState, RenewSession, LoginWithGoogle
└── impl/src/
    ├── commonMain/kotlin/app/yap/feature/auth/
    │   ├── domain/{repository,usecase}/
    │   ├── data/{identity,local,mapper,remote,repository}/
    │   ├── presentation/login/        # LoginViewModel, LoginUiStateMapper, LoginScreen
    │   └── di/FeatureAuthModule.kt    # public; app-root loads it from impl
    ├── commonMain/composeResources/values/strings.xml
    ├── androidMain/                   # Credential Manager + AppAuth fallback, Keystore+DataStore
    └── iosMain/                       # Keychain session storage

apps/mobile/app-root/src/commonMain/   # App, RootBackStack, RootNavKey.Main, appRootModule, LaunchRenewal
apps/mobile/shared-app/src/            # initAndroidKoin, initIosKoin, mainViewController (no commonMain)
apps/mobile/android-app/src/main/      # MainActivity, splash, OAuth redirect intent filter
apps/mobile/ios-app/                   # Xcode host: YapShared.framework, GoogleSignIn SPM

services/server/feature-auth/src/main/
├── kotlin/app/yap/server/feature/auth/
│   ├── AuthService.kt
│   ├── api/                           # routes and server-only HTTP contracts
│   ├── identity/                      # GoogleIdentityVerifier, GoogleCodeExchanger, GoogleAuthConfig
│   ├── model/                         # AuthenticatedSession, AuthFailure
│   └── persistence/                   # Exposed tables and repository
└── resources/db/migration/V1__auth.sql

services/server/app/src/main/kotlin/app/yap/server/app/
└── Application.kt                     # lifecycle, plugins, error mapping, health, graph
```

**Structure Decision**: The module layout in `README.md` is already correct; this feature fills it
in. The one structural change is splitting `:apps:mobile:feature-auth` into `api` and `impl`
sub-modules as `docs/mobile/001-feature-boundaries.md` requires, updating `settings.gradle.kts`
and `app-root`'s dependencies accordingly. The empty `apps/mobile/feature-auth/test-fixtures/`
directory tree is removed: stubs for this feature's own use cases live in `impl`'s `commonTest`,
and no second module consumes them yet.

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
| --- | --- | --- |
| Platform-capability ports (`Platform`, `MotionPreferences`, `ActivityProvider`) added to `core-common` rather than to the feature | The login mapper needs the running platform to hide Apple on Android (FR-003) and the reduced-motion preference to freeze animation (FR-020); both are process-wide facts, not auth concepts, and every later screen needs the same two answers | Keeping them in `feature-auth/impl` would make the second feature either duplicate the `expect`/`actual` pair or reach into an unrelated feature's `impl`, which Principle I forbids |
| `app-root` owns the Navigation 3 back stack directly as `RootBackStack`, with no `Navigator` type or interface anywhere | `docs/mobile/003-dependency-injection.md` describes `app-root` owning the single `Navigator`; here that role is only "hold the back stack and swap its root on auth state", and no view model in this feature navigates — login success is published as auth state instead | Introducing a `Navigator` now, in `app-root` or as an interface in `core-common`, would create an abstraction with no second implementation and no injected consumer, which Principle I's "no layer before it owns behavior" rule prohibits. It arrives when the first feature actually injects it |
| Login nonce is generated on the client rather than issued by the server | Google's Android guidance recommends binding a nonce into the ID token request, and the client-generated value still prevents a token minted for one in-flight request being swapped into another | A server-issued challenge is stronger and `TokenService.createChallenge` already exists, but it adds a third endpoint and a round trip before the user has done anything. Recorded in `research.md` as the upgrade path if the threat model demands it |
