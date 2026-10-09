# Implementation Plan: Scenarios and Progress

**Branch**: `feature/002-feature-scenario` | **Feature ID**: `002-feature-scenario`
| **Date**: 2026-10-09 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/002-feature-scenario/spec.md`

## Summary

The second vertical slice: the main tab shell with the Home and Scenarios screens, the scenario
list with per-user progress, five activation slots, the daily streak, and practice minutes —
mobile and server. The practice conversation and the paywall do not exist yet; this feature ends
at two owned seams (`SessionGateway`, access/paywall ports) wired to explicit placeholders, the
same "coming soon" pattern 001 used for Apple and T-ID.

Technical shape:

- `feature-scenario` splits into `api`/`impl`. The domain aggregate is one `ScenarioOverview`
  (scenario list + per-scenario progress + slots + streak + minutes) observed by both screens; the
  repository holds it in memory, snapshots it to DataStore for offline, and refreshes it from
  one `GET /v1/scenarios/state` call (stale-while-revalidate). FR-004/FR-006 states fall out of
  cache presence: no cache + loading → progress indicator, no cache + failure → full-screen
  retry, cache → content.
- `app-root` replaces `MainPlaceholderScreen` with the main scaffold: the floating three-tab
  bar from the design. Home and Scenarios are destinations declared in `feature-scenario`'s
  Koin module (`HomeNavKey`, `ScenariosNavKey` in `api`); the Profile tab stays an `app-root`
  placeholder until `feature-profile`.
- The server gains `feature-scenario`: the scenario list seeded by Flyway migration (new scenarios
  ship without an app release), per-user progress tables, and four authenticated endpoints —
  read state, activate, repeat, report progress. Progress mutations serialize on a per-user
  row lock; the 5-slot limit, report idempotency (`reportId`), and attempt binding are
  transactional rules verified against real PostgreSQL semantics.
- Progress only moves forward (FR-005): objective achievements are insert-only facts keyed by
  `(user, scenario, attempt, objective)`; repeating a scenario increments `attempt` instead of
  deleting anything, which is also what keeps streak days and minutes intact (FR-034). Streak
  days are the set of device-local dates the client reports with each **achieved objective**
  (FR-033) — time-only reports never earn one; the server never converts time zones.
- Access is a read-only seam: mobile `ObserveAccessUseCase` and server `AccessPolicy` ports
  default to "free" until `feature-subscription` provides the real implementations. A locked
  tap records `locked_content_click` and opens the placeholder paywall sheet.
- Analytics lands as a port: `AnalyticsTracker` in `core-common` with a logging implementation
  wired in `shared-app`; the seven FR-050 events are fired from view models.

## Technical Context

**Language/Version**: Kotlin 2.4.0 — `commonMain`/`androidMain`/`iosMain` for the client, JVM 17
for the server. No new Swift: this feature adds no platform capability the host must bridge.

**Primary Dependencies**: unchanged from 001 — Compose Multiplatform 1.11.1 + Material3, Koin
4.2.2, Navigation 3 (runtime 1.2.0-alpha04 / ui 1.2.0-alpha02), Ktor 3.5.0 client/server,
kotlinx.serialization, DataStore (snapshot cache), Exposed 0.61.0, Flyway, HikariCP,
PostgreSQL. No new third-party dependency is introduced.

**Storage**: server PostgreSQL (scenario list + progress, source of truth); mobile DataStore JSON
snapshot of the last `ScenarioOverview` for offline rendering, plain (not encrypted — no
credentials in it).

**Testing**: mobile — kotlin-test + coroutines-test + stubcall builders per `docs/testing/*`;
server — unit tests plus Testcontainers PostgreSQL for slot concurrency, merge idempotency, and
migrations per `docs/testing/003-backend-integration.md`.

**Target Platform**: Android (compileSdk 37) and iOS via KMP; server Linux/JVM 17.

**Project Type**: mobile app + server in one monorepo (established layout).

**Performance Goals**: hero visible from cache without network wait on warm start; state fetch
p95 < 1 s on Wi-Fi; tab switches and Scenarios-screen scrolling at 60 fps.

**Constraints**: offline read of cached progress (FR-004); no cache → indicator/error states
(FR-006); slot invariant ≤ 5 under concurrency (SC-004); progress merge monotonic (FR-005).

**Scale/Scope**: 20 scenarios × 5–6 objectives, single-digit-KB state payloads; 2 screens +
2 dialogs/sheets; 4 endpoints; 2 new mobile modules, 1 new server module, 1 new contract module.

## Constitution Check

*GATE: evaluated before Phase 0; re-checked after Phase 1 design.*

- **I. Feature-first boundaries — PASS.** New modules: `:apps:mobile:feature-scenario:api` +
  `:impl`, `:services:server:feature-scenario`, `:shared:contract:scenario`. No feature
  depends on another feature's `impl`; the session/subscription seams are ports owned by this
  feature's domain, not reaches into future modules. `api` exposes only nav keys, entities,
  and use-case interfaces other features will need (Profile reads the same figures later).
- **II. Layered dependencies — PASS.** `presentation → domain ← data`; DTOs and DataStore
  types stay in `data`; placeholders for session/paywall are adapters in `impl`, injected via
  domain ports. View models report navigation intent through `Navigator`.
- **III. Test-first — PASS (process).** Each behavior lands red-first; PostgreSQL semantics
  (slot limit under concurrent activation, attempt reset, idempotent achievement insert) are
  verified against Testcontainers, not mocks.
- **IV. Wire contracts — PASS.** All serialized types live in `shared/contract/scenario` with
  the `Dto` suffix; mobile maps DTO→domain in the repository, server maps DTO→feature model at
  the routes; errors reuse `shared/contract/common` `ErrorResponseDto` codes.
- **V. Documented rules govern — PASS.** Deviations and additions are listed in Complexity
  Tracking; none silently bends a guide.

**Post-design re-check**: no new violations introduced by Phase 1 artifacts; the three
Complexity Tracking rows below remain the complete list.

## Project Structure

### Documentation (this feature)

```text
specs/002-feature-scenario/
├── plan.md              # This file
├── research.md          # Phase 0: decisions R1–R11
├── data-model.md        # Phase 1: server schema + domain model + state machine
├── quickstart.md        # Phase 1: build/run/verify walkthrough
├── contracts/
│   ├── scenario-api.md          # HTTP endpoints + shared DTOs
│   ├── feature-scenario-api.md  # mobile api module surface + ports/seams
│   └── app-shell.md             # app-root tab scaffold + core-common analytics
└── tasks.md             # Phase 2 (/speckit-tasks — not created here)
```

### Source Code (repository root)

```text
shared/contract/scenario/src/commonMain/kotlin/…/contract/scenario/
└── ScenarioStateDto.kt, ScenarioDto.kt, ObjectiveDto.kt, ScenarioProgressDto.kt,
    StreakDto.kt, ReportProgressRequestDto.kt            # wire types only

apps/mobile/feature-scenario/api/src/commonMain/kotlin/…/feature/scenario/api/
├── HomeNavKey.kt, ScenariosNavKey.kt
├── entity/{ScenarioOverview,Scenario,ScenarioStatus,Objective,Streak,SlotUsage}.kt
└── usecase/ObserveScenarioOverviewUseCase.kt            # what Profile reads later

apps/mobile/feature-scenario/impl/src/commonMain/kotlin/…/feature/scenario/
├── domain/        # use cases: refresh, activate, open, repeat; ports: ScenarioRepository,
│                  #   SessionGateway, AccessGateway, PaywallGateway
├── data/          # DefaultScenarioRepository, ScenarioRemoteDataSource, OverviewSnapshotStore,
│                  #   DTO↔domain mappers
├── presentation/
│   ├── home/      # HomeViewModel, HomeUiState, mapper; ui/ (hero, active cards, streak week)
│   ├── scenarios/ # ScenariosViewModel, filter state; ui/ (filter rail, grouped rows)
│   └── common/    # shared row/pips models, repeat-confirmation sheet
├── placeholder/   # ComingSoonSessionGateway, FreeAccessGateway, PlaceholderPaywall
└── di/            # feature module: bindings + navigation<HomeNavKey>/<ScenariosNavKey>

apps/mobile/app-root/                  # MainPlaceholderScreen → MainScaffold (3-tab bar,
                                       #   Profile tab placeholder), tab back stacks
apps/mobile/core-common/               # + analytics/AnalyticsTracker.kt
apps/mobile/core-design/               # + palette tokens lifted from feature_main.dc.html
apps/mobile/shared-app/                # + LoggingAnalyticsTracker binding

services/server/feature-scenario/src/main/
├── kotlin/…/server/feature/scenario/{ScenarioService,api,model,persistence,access}
│   # routes, service (state/activate/repeat/report), AccessPolicy port + FreeOnlyAccessPolicy
└── resources/db/migration/V2__scenario.sql   # schema + 20-scenario seed
services/server/app/                   # graph: wire ScenarioService + routes
```

**Structure Decision**: same shape as 001 — the feature is two mobile modules plus one server
module plus one contract module, presentation split per screen with rendering in nested `ui`
packages. `app-root` owns the tab shell because it already owns the back-stack base; Home and
Scenarios destinations stay declared in the feature's own Koin module.

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
| --- | --- | --- |
| `AnalyticsTracker` port in `core-common` rather than the feature | FR-050 requires seven events now, and every later feature (session, subscription, profile) has its own mandatory event tables in Notion | A feature-owned port would be copied by the very next feature; promotion to `core-*` for independent reuse is the documented rule |
| Placeholder adapters for session, access, and paywall inside `feature-scenario/impl` | The spec's core loop ends in screens owned by features that do not exist yet; the seams must exist for this slice to be testable end to end | Creating `feature-session:api`/`feature-subscription:api` now would put empty modules and speculative contracts in the graph before any behavior owns them |
| Migration numbered `V2__scenario.sql` inside a different module than `V1__auth.sql` | Flyway keeps one version history across all classpath migration locations | Per-module numbering restarts would collide at `V1`; renumbering 001's migration rewrites applied history |
