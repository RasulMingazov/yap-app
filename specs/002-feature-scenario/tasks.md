# Tasks: Scenarios and Progress

**Input**: Design documents from `specs/002-feature-scenario/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/, quickstart.md

**Tests**: included — constitution III makes test-first NON-NEGOTIABLE for behavior changes:
each test task is written first and run red before its implementation task starts. PostgreSQL
semantics run against Testcontainers per `docs/testing/003-backend-integration.md`.

**Organization**: grouped by user story (US1–US5 from spec.md) after shared Setup and
Foundational phases. Paths follow plan.md's Project Structure; Kotlin packages shorten
`apps/mobile/feature-scenario/impl/src/commonMain/kotlin/app/yap/feature/scenario/` to
`impl:…` (and likewise `api:…`, `commonTest` as `impl-test:…`), server
`services/server/feature-scenario/src/main/kotlin/app/yap/server/feature/scenario/` to
`server:…` (`src/test/kotlin/…` as `server-test:…`).

## Format: `[ID] [P?] [Story] Description`

## Phase 1: Setup

**Purpose**: the four new modules exist and the build stays green.

- [X] T001 Register `:shared:contract:scenario`, `:apps:mobile:feature-scenario:api`,
      `:apps:mobile:feature-scenario:impl`, `:services:server:feature-scenario` in
      `settings.gradle.kts`
- [X] T002 [P] Create the three mobile/contract module skeletons with `build.gradle.kts`
      mirroring `feature-auth` counterparts (convention plugins only; `impl` depends on its
      `api`, `api` on `core-*` + `navigation3-runtime`) under `shared/contract/scenario/` and
      `apps/mobile/feature-scenario/{api,impl}/`
- [X] T003 [P] Create `services/server/feature-scenario/build.gradle.kts` mirroring
      `services/server/feature-auth` (Exposed, Ktor routes, Testcontainers test deps)
- [X] T004 Run `./gradlew build` — empty modules compile, Detekt green

---

## Phase 2: Foundational (blocking all stories)

**Purpose**: wire DTOs, schema + seed, the state endpoint, the mobile aggregate pipeline, and
the tab shell — everything every story reads.

**⚠️ CRITICAL**: no user-story phase starts before this phase completes.

### Wire contract

- [X] T005 [P] Add `ScenarioStateDto`, `ScenarioDto`, `ObjectiveDto`,
      `ReportProgressRequestDto` per `contracts/scenario-api.md` in
      `shared/contract/scenario/src/commonMain/kotlin/app/yap/contract/scenario/`
- [X] T006 [P] Add `ACCESS_REQUIRED`, `SLOT_LIMIT_REACHED`, `NOT_FOUND` to `ApiErrorCode` in
      `shared/contract/common`

### Server foundation (test-first)

- [X] T007 Write red Testcontainers test: migration applies, seed holds 20 scenarios in
      `position` order with exactly one `is_free`, each with 5–6 objectives — in
      `server-test:persistence/ScenarioSchemaTest.kt`
- [X] T008 Create `V2__scenario.sql` (tables `scenario`, `scenario_objective`,
      `user_scenario`, `user_objective`, `practice_day`, `user_stats`, `progress_report` per
      data-model.md + the 20-scenario seed from the design list) in
      `services/server/feature-scenario/src/main/resources/db/migration/`
- [X] T009 [P] Exposed tables + `ScenarioRepository` (feature-owned persistence per
      `docs/server/002-persistence.md`) in `server:persistence/`
- [X] T010 [P] `AccessPolicy` port + `FreeOnlyAccessPolicy` in `server:access/`
- [X] T011 Write red integration test for `GET /v1/scenarios/state?today=`: fresh user →
      20 scenarios, 1 free available, 19 locked, 0 slots, streak 0; malformed `today` → 400 —
      in `server-test:api/ScenarioStateRouteTest.kt`
- [X] T012 Implement `ScenarioService.state(userId, today)` (status × locked pair, slots,
      streak anchored today/yesterday per data-model.md, minutes) in
      `server:ScenarioService.kt` + DTO mapping and route in `server:api/ScenarioRoutes.kt`
- [X] T013 Wire `ScenarioService`, `FreeOnlyAccessPolicy`, and `scenarioRoutes` into the graph
      in `services/server/app/src/main/kotlin/app/yap/server/app/Application.kt`

### Mobile foundation (test-first)

- [X] T014 [P] `AnalyticsTracker` + `AnalyticsEvent` in
      `apps/mobile/core-common/src/commonMain/kotlin/app/yap/core/common/analytics/`;
      `LoggingAnalyticsTracker` bound in `apps/mobile/shared-app` Koin
- [X] T015 [P] Extend `YapColors`/`YapTheme` with the main-flow tokens lifted from
      `feature_main.dc.html` per research R9 in
      `apps/mobile/core-design/src/commonMain/kotlin/app/yap/core/design/theme/`
- [X] T016 [P] `api` surface: `HomeNavKey`, `ScenariosNavKey`, entities (`ScenarioOverview`,
      `Scenario` with orthogonal `status`/`locked`, `Objective`, `Streak`, `SlotUsage`,
      `OverviewState`), `ObserveScenarioOverviewUseCase` in `api:…` per
      `contracts/feature-scenario-api.md`
- [X] T017 Write red repository tests: snapshot → `Ready` immediately + silent refresh;
      no snapshot + fetch in flight → `Loading`; no snapshot + failure → `Unavailable`;
      mutation response replaces state and snapshot; snapshot keyed by account and cleared
      on logout — in `impl-test:data/DefaultScenarioRepositoryTest.kt` (stubcall per
      `docs/testing/002-stubs.md`)
- [X] T018 Implement `impl:domain/` ports (`ScenarioRepository`, `SessionGateway`,
      `AccessGateway`, `PaywallGateway`, `ActivationResult`) and `impl:data/`
      (`ScenarioRemoteDataSource` via `core-network`, `OverviewSnapshotStore` on DataStore,
      `DefaultScenarioRepository`, DTO↔domain mappers) until T017 is green
- [X] T019 Feature Koin module binding domain/data + `navigation<HomeNavKey>` /
      `navigation<ScenariosNavKey>` destinations in `impl:di/ScenarioModule.kt`, registered
      in `apps/mobile/shared-app` module list
- [X] T020 Write red `RootBackStack`/scaffold test: logged-in state shows the tab shell with
      Главная selected; tab switch preserves each tab's stack — extend
      `apps/mobile/app-root/src/commonTest/kotlin/app/yap/app/root/navigation/RootBackStackTest.kt`
- [X] T021 Replace `MainPlaceholderScreen` with `MainScaffold` (floating 3-tab bar per
      `contracts/app-shell.md`, collapsing title bar, re-tap scrolls to top,
      `ProfilePlaceholderScreen`) in `apps/mobile/app-root/src/commonMain/kotlin/app/yap/app/root/navigation/`

**Checkpoint**: `./gradlew build` green; app shows the tab shell with empty states; server
serves `state`.

---

## Phase 3: US1 — Continue or start practice from Home (P1) 🎯 MVP

**Goal**: one primary action on Home starts the free scenario or resumes the last open one;
offline shows cache; no-cache failure shows retry.

**Independent test**: quickstart app walkthrough steps 1–3, 7–8 with only foundational code —
fresh account sees «Начать» (step 1), progressed account sees «Продолжить» + «Шаг X из N»,
airplane mode renders cache, first-load failure shows «Повторить».

- [X] T022 [P] [US1] Write red `HomeViewModel` tests: hero variants startFree / continue /
      pick-from-Scenarios (subscriber, no active); `home_view` once per show;
      `continue_click` then `SessionGateway.open` on primary; offline decline message;
      `Unavailable` → retry event refreshes — in `impl-test:presentation/home/HomeViewModelTest.kt`
- [X] T023 [P] [US1] Write red `OpenScenarioUseCase` test: active scenario → `SessionGateway.open`
      directly, records `scenario_open` — in `impl-test:domain/OpenScenarioUseCaseTest.kt`
- [X] T024 [US1] Implement `OpenScenarioUseCase` (active branch) + `RefreshOverviewUseCase` in
      `impl:domain/`; `ComingSoonSessionGateway` placeholder sheet in `impl:placeholder/`
- [X] T025 [US1] Implement `HomeUiState`, mapper, `HomeViewModel` (events: primary, card tap,
      add-slot → `ScenariosNavKey`, retry) in `impl:presentation/home/`
- [X] T026 [US1] Compose Home screen: hero block with step pips, active-scenario card rail +
      free-slot card or full-slots notice (FR-012), streak-week section, loading indicator
      (accent lime/black token), full-screen retry — in `impl:presentation/home/ui/` per
      `tab_home.dc.html`
- [X] T027 [US1] Verify: `./gradlew build` + walkthrough steps 1–3, 7–8 on emulator

**Checkpoint**: MVP — the core loop (open → practice entry point → resume) demonstrable.

---

## Phase 4: US2 — Pick a scenario on the Scenarios screen (P2)

**Goal**: grouped list with filters; tapping activates (slot) and opens; server enforces
slots and access.

**Independent test**: quickstart walkthrough step 4 + API walkthrough activate calls; each
filter narrows; activation moves the row and bumps the slot count on both screens.

- [X] T028 [P] [US2] Write red Testcontainers tests for `POST /v1/scenarios/{id}/activate`:
      success occupies slot; re-activate idempotent 200; unknown id 404; locked without
      access 403 `access_required`; sixth 409 `slot_limit_reached`; two concurrent
      activations at 4/5 → exactly one 409 (per-user `FOR UPDATE` lock, research R5) — in
      `server-test:api/ScenarioActivateRouteTest.kt`
- [X] T029 [US2] Implement activation in `ScenarioService` (lock → access check → slot count
      → upsert `user_scenario`) + route until T028 green
- [X] T030 [P] [US2] Write red `ScenariosViewModel` tests: grouping from status×locked pair
      (Активные + «n / 5», Доступные, По подписке, Завершённые; empty hidden); filter rail
      counts + `scenarios_filter_select`; row taps per FR-022 incl. slot-full decline +
      `slot_limit_reached`; `scenarios_view` once — in
      `impl-test:presentation/scenarios/ScenariosViewModelTest.kt`
- [X] T031 [US2] Extend `OpenScenarioUseCase` (available → `repository.activate` then open;
      `SlotLimitReached` → snackbar event) and repository `activate` mapping 403/409 to
      `ActivationResult`
- [X] T032 [US2] Implement `ScenariosUiState`, `ScenariosViewModel`, filter state in
      `impl:presentation/scenarios/`
- [X] T033 [US2] Compose Scenarios screen: sticky filter rail, grouped rows with status meta
      («k из n шагов», «Бесплатно · n шагов», lock, done badge, «Нет свободного слота»),
      row press states — in `impl:presentation/scenarios/ui/` per `tab_catalog.dc.html`
- [X] T034 [US2] Verify: `./gradlew build` + walkthrough step 4; activation reflected on Home

---

## Phase 5: US3 — Completion updates progress, slot, streak, minutes (P3)

**Goal**: the progress endpoint with all four guards; streak/minutes correct on both screens.

**Independent test**: quickstart API walkthrough progress calls + server suite — replay,
time-only, stale attempt, completion, streak anchor/gap/week-edge all pass; Home hero flips
to «Продолжить»/«Шаг 2 из N» after an external report.

- [X] T035 [P] [US3] Write red Testcontainers tests for `POST /v1/scenarios/{id}/progress`
      per `contracts/scenario-api.md`: replayed `reportId` no-op (minutes once); time-only
      report → minutes yes, streak day no; stale `attempt` 400; out-of-order 400; achievement
      advances `current_objective` monotonically; last objective completes + frees slot in
      the same transaction — in `server-test:api/ScenarioProgressRouteTest.kt`
- [X] T036 [P] [US3] Write red streak unit tests: anchor today when today practised, else
      yesterday, else 0; gap breaks run; week-edge dates; two-device date union — in
      `server-test:StreakCalculationTest.kt`
- [X] T037 [US3] Implement progress handling in `ScenarioService` (reportId ledger → attempt
      check → idempotent insert → `GREATEST` → completion → `practice_day` on new
      achievement only → seconds) + streak calculation used by `state` until T035/T036 green
- [X] T038 [P] [US3] Write red Home mapper tests: allDone hero (programme done → Scenarios);
      completed scenario leaves active rail; streak block states (safe / one objective
      needed / at-risk note) — extend `impl-test:presentation/home/HomeViewModelTest.kt`
- [X] T039 [US3] Implement allDone hero + streak-note mapping in `impl:presentation/home/`
- [X] T040 [US3] Verify: `./gradlew :services:server:feature-scenario:test` + API walkthrough;
      foreground refresh shows «Шаг 2 из N» and streak 1 (walkthrough step 6)

---

## Phase 6: US4 — Locked scenario leads to subscription (P4)

**Goal**: freeDone promo state on Home, locked previews and rows route to the paywall
placeholder; lapsed-access rendering per R12.

**Independent test**: free user with free scenario completed sees promo hero + «Откроется с
подпиской» row; any locked tap logs `locked_content_click` and opens the placeholder sheet;
an `active + locked` scenario stays in Активные with a lock and keeps its slot.

- [X] T041 [P] [US4] Write red tests: `OpenScenarioUseCase` locked branch →
      `PaywallGateway.open(PaywallSource(scenarioId, origin))` + `locked_content_click` with
      `scenario_id`/`origin` params; Home freeDone mapper → promo hero + locked preview row;
      Scenarios grouping for `active+locked` (Активные, lock badge, slot still counted) —
      extend home/scenarios test files
- [X] T042 [US4] Implement `FreeAccessGateway` + `PlaceholderPaywall` sheet in
      `impl:placeholder/`, locked branches in `OpenScenarioUseCase`, promo hero + locked
      preview row (`lockedPreview`, «Все 19») in `impl:presentation/home/`
- [X] T043 [US4] Server test + fix if needed: `state` for a user with `active` scenarios and
      no access answers `status="active", locked=true` and still counts their slots — extend
      `server-test:api/ScenarioStateRouteTest.kt`
- [X] T044 [US4] Verify: walkthrough step 5; freeDone state via completing the free scenario
      through the API

---

## Phase 7: US5 — Repeat a completed scenario (P5)

**Goal**: confirmed restart; attempt increments; streak/minutes untouched.

**Independent test**: complete the free scenario, tap it under Завершённые, confirm —
step 1 current, streak/minutes unchanged; a stale report from attempt 1 is rejected.

- [X] T045 [P] [US5] Write red Testcontainers tests for `POST /v1/scenarios/{id}/repeat`:
      attempt+1, current objective 1, status active, `practice_day` + minutes preserved;
      not-completed 409; slot and access rules apply — in
      `server-test:api/ScenarioRepeatRouteTest.kt`
- [X] T046 [US5] Implement repeat in `ScenarioService` + route until T045 green
- [X] T047 [P] [US5] Write red mobile tests: `RepeatScenarioUseCase` (confirm → repeat →
      `SessionGateway.open`); confirmation sheet result flow; cancel changes nothing — in
      `impl-test:domain/RepeatScenarioUseCaseTest.kt` + scenarios VM test
- [X] T048 [US5] Implement repeat-confirmation bottom-sheet destination (reset/kept copy per
      design, `BottomSheetSceneStrategy` + result API) in `impl:presentation/common/` and
      `RepeatScenarioUseCase` in `impl:domain/`
- [ ] T049 [US5] Verify: walkthrough repeat flow end to end on emulator

---

## Phase 8: Polish & cross-cutting

- [X] T050 [P] Audit FR-050 events against the debug log: each of the seven fires exactly
      once per triggering action (SC-005), fix duplicates at their view-model source
- [X] T051 [P] Snapshot-on-logout integration: hook `OverviewSnapshotStore.clear()` into the
      001 logout path in `apps/mobile/shared-app` wiring; test in
      `impl-test:data/DefaultScenarioRepositoryTest.kt` if not already covered by T017
- [X] T052 Run `./gradlew build` and
      `./gradlew :apps:mobile:shared-app:compileKotlinIosSimulatorArm64` (KMP boundary
      changed); fix anything red
- [ ] T053 Full quickstart.md walkthrough (API + app, steps 1–8) on emulator and record
      deviations in the PR description per constitution V

---

## Dependencies

- Phase 1 → Phase 2 → all story phases; Phase 8 last.
- **US1** needs only Foundational. **US2** needs Foundational (not US1). **US3** needs US2's
  activate endpoint (T029) to set up progress; its mobile tasks need US1's Home mapper.
  **US4** needs US2 grouping + US1 hero. **US5** needs US3's completion to produce a
  Завершённые row.
- Within every story: red test task(s) strictly before their implementation task
  (constitution III).

## Parallel examples

- Phase 2: T005/T006 ∥; then T007 ∥ T014/T015/T016; T009 ∥ T010; mobile chain T017→T018→T019
  runs ∥ server chain T011→T012→T013.
- US1: T022 ∥ T023 (different test files), then T024→T025→T026 sequential (same packages).
- US2: T028 (server) ∥ T030 (mobile tests); T029 ∥ T031/T032.
- US3: T035 ∥ T036 ∥ T038; T037 after T035+T036.

## Implementation strategy

MVP = Phases 1–3 (US1): tab shell, state pipeline, Home with start/continue against the
placeholder session — demonstrable and independently testable. Then US2 unlocks the full
list, US3 makes progress real (server-first — the app renders whatever the aggregate says),
US4 adds the conversion path, US5 the retention loop. Each story phase ends green on
`./gradlew build` before the next starts.
