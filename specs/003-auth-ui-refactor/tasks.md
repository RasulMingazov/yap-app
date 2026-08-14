---
description: "Task list for Auth Provider Selection and Login Theming Refactor"
---

# Tasks: Auth Provider Selection and Login Theming Refactor

**Input**: Design documents from `/specs/003-auth-ui-refactor/`

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md),
[data-model.md](data-model.md), [contracts/](contracts/)

**Tests**: Required, not optional. Constitution Principle III is non-negotiable and FR-025 restates
it: a behaviour change lands as a focused test that fails for the intended reason before the
implementation. Type changes, dependency bumps, and file moves carry no test of their own — the
constitution forbids artificial tests for build-only changes.

**Organization**: Tasks are grouped by user story so each can be implemented and verified on its own.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (US1…US6)
- Every task names the exact file it touches

## Path Conventions

Kotlin Multiplatform monorepo. Mobile code lives under `apps/mobile/<module>/src/<sourceSet>/kotlin/`,
resources under `apps/mobile/<module>/src/commonMain/composeResources/`. Tests mirror the source
package. Abbreviations used below:

- `API` = `apps/mobile/feature-auth/api/src/commonMain/kotlin/app/yap/feature/auth/api`
- `IMPL` = `apps/mobile/feature-auth/impl/src/commonMain/kotlin/app/yap/feature/auth`
- `TEST` = `apps/mobile/feature-auth/impl/src/commonTest/kotlin/app/yap/feature/auth`
- `RES` = `apps/mobile/feature-auth/impl/src/commonMain/composeResources`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Move Navigation 3 to the release that carries the official result API, and verify it
links, before any code depends on it.

- [X] T001 Split the `navigation3` version reference into `navigation3Runtime = "1.2.0-alpha04"` and `navigation3Ui = "1.2.0-alpha02"` in `gradle/libs.versions.toml`, repointing the `navigation3-runtime` and `navigation3-ui` library entries. The `app.yap.navigation3` convention plugin refers to the aliases, not the versions, so `convention-plugins/src/main/kotlin/app/yap/convention/Navigation3ComposePlugin.kt` needs no edit.
- [X] T002 Run `./gradlew build` and `./gradlew :apps:mobile:shared-app:compileKotlinIosSimulatorArm64` to prove the upgrade links, then inspect `./gradlew :apps:mobile:app-root:dependencies --configuration androidRuntimeClasspath` for unexpected conflict resolution. **If `koin-compose-navigation3` 4.2.2 or `lifecycle-viewmodel-navigation3` 2.11.0 fails against the new runtime, STOP and escalate** — Koin 4.2.2 is the newest release and no fallback is pre-authorised (spec Clarifications, 2026-08-14).
- [X] T003 [P] Add `api(libs.navigation3.runtime)` to `apps/mobile/core-common/build.gradle.kts` so `NavKey` may appear on that module's API surface.
- [X] T004 [P] Apply `alias(libs.plugins.yap.navigation3)` in `apps/mobile/core-design/build.gradle.kts` so the shared module can host a scene strategy.

**Checkpoint**: the build is green on the new navigation version and nothing depends on it yet.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Reshape the one type that User Stories 1, 2, and 3 all sit on. Build-only — no test of
its own; the behaviour it enables is tested in the stories that add it.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete.

- [X] T005 Replace the `AuthProvider` enum with the sealed hierarchy in `API/entity/AuthProvider.kt`: `sealed interface AuthProvider` with `isEnabled`/`isVisible` and the `Apple`, `Google`, `TId` data classes, per [contracts/feature-auth-api.md](contracts/feature-auth-api.md).
- [X] T006 Keep the build green at the old call sites: have `IMPL/presentation/login/AuthProviderCatalog.kt` construct instances from its existing `isUsable`/`shownOn` declarations, and update `TEST/presentation/login/StubAuthProviderDeclaration.kt` to match. This is a deliberate two-phase shim — the catalogue is deleted in T042.

**Checkpoint**: Foundation ready — user story implementation can begin.

---

## Phase 3: User Story 1 - One way in, whichever provider (Priority: P1) 🎯 MVP

**Goal**: The login state holder depends on exactly one login entry point that takes a provider, and
adding a provider is a registration rather than an edit to branching.

**Independent Test**: Sign in with Google end to end, then register a second `ProviderLogin` and
confirm it runs with no edit to `LoginViewModel`.

### Tests for User Story 1 ⚠️

> Write these first and confirm each fails for the intended reason.

- [X] T007 [P] [US1] `TEST/domain/usecase/DefaultLoginUseCaseTest.kt` — a registered provider runs its handler; an unregistered one returns `Unavailable` without invoking anything; a provider the roster marks `isEnabled = false` returns `Unavailable` without invoking its handler even when one is registered (FR-005); the attempt bound turns a hanging handler into `Cancelled`.
- [X] T008 [P] [US1] Extend `TEST/presentation/login/LoginViewModelTest.kt` — `Failed` and `Unavailable` each raise their message, `Success` and `Cancelled` raise none, and a second choice during an in-flight login is ignored.
- [X] T009 [P] [US1] Add `TEST/domain/provider/StubProviderLogin.kt` for handler-level stubbing, mirroring the source package. `TEST/domain/usecase/StubLoginUseCase.kt` is **not** deleted — `LoginViewModelTest` still stubs the entry point; update it to the new `invoke(provider)` signature and have it record the provider it was called with, which is what T027 asserts against.

### Implementation for User Story 1

- [X] T010 [P] [US1] Add `data object Unavailable` to `API/entity/LoginOutcome.kt`.
- [X] T011 [P] [US1] Change `API/usecase/LoginUseCase.kt` to `suspend operator fun invoke(provider: AuthProvider): LoginOutcome`.
- [X] T012 [US1] Create the `ProviderLogin` port with `val provider: KClass<out AuthProvider>` in `IMPL/domain/provider/ProviderLogin.kt`.
- [X] T013 [US1] Move `IMPL/domain/usecase/GoogleLoginUseCase.kt` to `IMPL/domain/provider/GoogleProviderLogin.kt` as a `ProviderLogin`, dropping its own timeout, and delete the old file. Rename `TEST/domain/usecase/GoogleLoginUseCaseTest.kt` to `TEST/domain/provider/GoogleProviderLoginTest.kt` so the test mirrors the new source package, dropping its timeout case — the bound now lives in `DefaultLoginUseCase` and is covered by T007.
- [X] T014 [US1] Implement `IMPL/domain/usecase/DefaultLoginUseCase.kt` — index the injected `List<ProviderLogin>` by `provider`, apply the 60-second attempt bound to every provider, return `Unavailable` when no handler matches **or when the chosen provider is not `isEnabled`**. FR-005 makes selectability a login-time check, not only a wiring fact: the roster owns the flag and this use case is the one place that reads it, so a registered-but-disabled provider can never sign in.
- [X] T015 [US1] Remove `loginUseCases`, `platform`, and `declarations` from `IMPL/presentation/login/LoginViewModel.kt`; it now calls `loginUseCase(provider)` and keeps the existing `Event.ProviderChosen(provider)` path.
- [X] T016 [US1] In `IMPL/di/FeatureAuthModule.kt` replace the `factory<Map<AuthProvider, LoginUseCase>>` with `single<ProviderLogin> { GoogleProviderLogin(...) }` and `factory<LoginUseCase> { DefaultLoginUseCase(providerLogins = getAll()) }`.
- [X] T017 [US1] Add a guard to `apps/mobile/feature-auth/impl/src/androidHostTest/kotlin/app/yap/feature/auth/di/FeatureAuthModuleTest.kt`: every provider the roster marks `isEnabled` resolves a `ProviderLogin` from the graph.

**Checkpoint**: US1 is functional — Google sign-in works through the single entry point and the view
model holds no provider collection.

---

## Phase 4: User Story 2 - Providers are decided in one place (Priority: P1)

**Goal**: One observable domain source decides which providers exist and how each may be used.

**Independent Test**: Change the source so a provider becomes hidden or unselectable and confirm the
surface follows without a UI edit; confirm no UI resource is reachable from domain or data.

### Tests for User Story 2 ⚠️

- [X] T018 [P] [US2] `TEST/domain/usecase/DefaultObserveAuthProvidersUseCaseTest.kt` — emission order is Google, Apple, T-ID; `Apple.isVisible` is true on iOS and false on Android; only Google is `isEnabled`.
- [X] T019 [P] [US2] `TEST/domain/usecase/StubObserveAuthProvidersUseCase.kt`, replacing `TEST/presentation/login/StubAuthProviderDeclaration.kt`.

### Implementation for User Story 2

- [X] T020 [US2] Add the `ObserveAuthProvidersUseCase` contract to `API/usecase/ObserveAuthProvidersUseCase.kt`.
- [X] T021 [US2] Implement `IMPL/domain/usecase/DefaultObserveAuthProvidersUseCase.kt` taking `Platform` and returning a cold `Flow<List<AuthProvider>>` — no port and no data source, per [research.md](research.md) R10.
- [X] T022 [US2] Bind it in `IMPL/di/FeatureAuthModule.kt`.
- [X] T023 [US2] Update `TEST/presentation/login/LoginUiStateMapperTest.kt` first — the mapper's output changes, which Principle III counts as a behaviour change — then strip the platform and availability rules out of `IMPL/presentation/login/AuthProviderCatalog.kt`, leaving only the label lookup the sheet still uses until T042, and update `IMPL/presentation/login/LoginUiStateMapper.kt` accordingly.

**Checkpoint**: the roster is the single source of provider visibility and selectability.

---

## Phase 5: User Story 3 - Choosing a provider is its own screen (Priority: P2)

**Goal**: Provider selection is a real destination with its own state, mapping, and state holder, and
it hands the choice back to the login screen.

**Independent Test**: Open the selection destination, choose a provider, confirm the login screen
starts the login; confirm `LoginViewModel` exposes no provider list.

### Tests for User Story 3 ⚠️

- [X] T024 [P] [US3] Extend the existing `apps/mobile/app-root/src/commonTest/kotlin/app/yap/app/root/navigation/RootBackStackTest.kt` — `navigate` pushes, `back` pops, a change of auth state resets the tail, and `back` leaves the tail without producing a choice. That last case is as far as an automated test reaches into FR-014's dismissal half; swipe and scrim dismissal stay manual (quickstart scenario 5).
- [X] T025 [P] [US3] `TEST/presentation/selectprovider/SelectAuthProviderUiStateMapperTest.kt` — invisible providers are dropped, `isEnabled` is copied, and each provider maps to its design label and mark with the right `isMonochrome`.
- [X] T026 [P] [US3] `TEST/presentation/selectprovider/SelectAuthProviderViewModelTest.kt` — the roster reaches `UiState`; `ProviderChosen` navigates back.
- [X] T027 [P] [US3] Extend `TEST/presentation/login/LoginViewModelTest.kt` — `PrimaryActionClicked` navigates to `AuthNavKey.SelectAuthProvider` while idle and is ignored during a login; `Unavailable` raises `login_provider_soon` carrying `AuthProviderResources.labelOf(provider)` as its argument, replacing the `login_provider_not_available` assertion T008 left in the same file. The second case is the failing test T045 must satisfy — the copy change is a behaviour change and Principle III admits no exception for it.
- [X] T028 [P] [US3] `TEST/presentation/selectprovider/ui/SelectAuthProviderScreenTest.kt` — rows render name and mark, and every visible row is tappable.

### Implementation for User Story 3

- [X] T029 [P] [US3] Create the `Navigator` contract in `apps/mobile/core-common/src/commonMain/kotlin/app/yap/core/common/navigation/Navigator.kt`.
- [X] T030 [P] [US3] Create `BottomSheetSceneStrategy` and `bottomSheetScene()` in `apps/mobile/core-design/src/commonMain/kotlin/app/yap/core/design/navigation/BottomSheetSceneStrategy.kt`, following the `AnimatedBottomSheetSample` shape — an `OverlayScene` whose `onRemove()` awaits `sheetState.hide()`.
- [X] T031 [US3] Give `apps/mobile/app-root/src/commonMain/kotlin/app/yap/app/root/navigation/RootBackStack.kt` a mutable tail over its auth-derived base and have it implement `Navigator`.
- [X] T032 [US3] Change the `RootBackStack` binding to `single` and bind it as `Navigator` in `apps/mobile/app-root/src/commonMain/kotlin/app/yap/app/root/di/AppRootModule.kt`.
- [X] T033 [US3] Wire `apps/mobile/app-root/src/commonMain/kotlin/app/yap/app/root/App.kt`: `onBack`, `sceneStrategies = listOf(BottomSheetSceneStrategy(), SinglePaneSceneStrategy())`, and `entryDecorators` listing both `rememberSaveableStateHolderNavEntryDecorator()` and `rememberResultEventBusNavEntryDecorator()` — the parameter replaces the default list rather than extending it.
- [X] T034 [US3] Add `data object SelectAuthProvider` to `API/AuthNavKey.kt`.
- [X] T035 [US3] Create the one provider-to-display-data table in `IMPL/presentation/AuthProviderResources.kt` with `labelOf` and `markOf`, branching with `is` over the sealed members.
- [X] T036 [US3] Create `IMPL/presentation/selectprovider/SelectAuthProviderUiStateMapper.kt` — drop invisible providers, copy `isEnabled`, read label and mark from T035.
- [X] T037 [US3] Create `IMPL/presentation/selectprovider/SelectAuthProviderViewModel.kt` subscribing to `ObserveAuthProvidersUseCase` and navigating back on `ProviderChosen`.
- [X] T038 [US3] Create `IMPL/presentation/selectprovider/ui/SelectAuthProviderScreen.kt` and `.../ui/SelectAuthProviderTestTags.kt` — the sheet body only; the scene owns `ModalBottomSheet`. The click handler calls `resultEventBus.sendResult<AuthProvider>(provider)` with the explicit type argument, then raises the event.
- [X] T039 [US3] In `IMPL/presentation/login/ui/LoginScreen.kt` remove the sheet block and add `ResultEffect<AuthProvider> { onEvent(Event.ProviderChosen(it)) }`.
- [X] T040 [US3] In `IMPL/presentation/login/LoginViewModel.kt` make `PrimaryActionClicked` navigate through `Navigator`, and drop `isProviderSheetVisible`, `providers`, and `ProviderSheetDismissed` from `DataState`, `UiState`, and `Event`. Update `TEST/presentation/login/ui/LoginScreenTestHost.kt` in the same step — it builds `UiState` with those fields, so the test source set stops compiling otherwise.
- [X] T041 [US3] In `IMPL/di/FeatureAuthModule.kt` add `viewModel { SelectAuthProviderViewModel(...) }` and `navigation<AuthNavKey.SelectAuthProvider>(metadata = bottomSheetScene()) { SelectAuthProviderScreen() }`.
- [X] T042 [US3] Delete `IMPL/presentation/login/AuthProviderCatalog.kt` and `IMPL/presentation/login/ui/AuthProviderSheet.kt`, and move the row assertions out of `TEST/presentation/login/ui/LoginScreenContentTest.kt` and `.../LoginScreenSemanticsTest.kt` into the new slice's tests.

### Design assets and copy for User Story 3

- [X] T043 [P] [US3] Add `RES/drawable/ic_provider_google.xml`, `ic_provider_apple.xml`, and `ic_provider_t_id.xml`, converted from `screen_login.dc.html` per [research.md](research.md) R9 — brand colours live in the assets, never in the theme.
- [X] T044 [US3] Render rows per the design in `IMPL/presentation/selectprovider/ui/SelectAuthProviderScreen.kt`: 52 dp minimum height, 12 dp gap, 4 dp side padding, 16 sp weight 600 in `onSurface` with `accent` when pressed, and tint only when `isMonochrome`. Apply the sheet chrome — 24 dp top corners, 1 dp top border, 36 × 4 dp handle, design paddings, and the uppercase 13 sp section label — inside `BottomSheetSceneStrategy`, reading `outline`, `handle`, `scrim`, and `sectionLabel` from `YapTheme.colors`. T047 and T048 land before this task (see Dependencies), so no colour literal is written into `core-design` or the new slice even temporarily.
- [X] T045 [US3] Replace `login_provider_not_available` with `login_provider_soon` ("Вход через %1$s скоро появится") in `RES/values/strings.xml`, add the optional `argument: StringResource?` to `News.ShowMessage` in `IMPL/presentation/login/LoginViewModel.kt`, and fill it from `AuthProviderResources.labelOf(provider)` on `Unavailable`. The behaviour lands test-first — its failing case is written in T027.

**Checkpoint**: the sheet is its own destination, matches the design, and the login screen owns the
login call.

---

## Phase 6: User Story 4 - One colour language, no literals on the screen (Priority: P2)

**Goal**: Every login colour lives in the shared theme under a role name, and the message colour is
the same in both themes.

**Independent Test**: Render the screen in both themes and confirm the only intended difference from
today is the dark-theme message colour.

### Tests for User Story 4 ⚠️

- [X] T046 [P] [US4] Extend `TEST/presentation/login/ui/LoginScreenThemeTest.kt` — the message background and text resolve to the same values in light and dark. Written before T047, which changes the dark `notice` value from `26232C` to `5E3689`: that is a visible behaviour change, so it lands red first.

### Implementation for User Story 4

- [X] T047 [US4] Create `apps/mobile/core-design/src/commonMain/kotlin/app/yap/core/design/theme/YapColors.kt` with the eighteen roles and both palettes lifted from the design, per [research.md](research.md) R7. Purely additive, and the one file where colour literals belong — it lands before T044 so the sheet chrome and the selection rows have roles to read.
- [X] T048 [US4] In `apps/mobile/core-design/src/commonMain/kotlin/app/yap/core/design/theme/YapTheme.kt` provide `LocalYapColors`, expose `YapTheme.colors`, and derive the Material `ColorScheme` from the same values. Additive as well, so it lands with T047 before T044; `LocalIsDarkTheme` is deleted in T049 together with its only consumer.
- [X] T049 [US4] Repoint the remaining login composables — `IMPL/presentation/login/ui/LoginScreen.kt` and `LegalLine.kt` — at `YapTheme.colors` (the selection slice already reads them, per T044), then delete `IMPL/presentation/login/ui/LoginColors.kt` and `LocalIsDarkTheme` from `.../theme/YapTheme.kt`.
- [X] T050 [US4] Widen the SC-002 check to the module that now carries login chrome: run `rg 'Color\(0x' apps/mobile/feature-auth apps/mobile/core-design/src/commonMain/kotlin/app/yap/core/design/navigation --glob '!**/build/**'` and update the structural check in [quickstart.md](quickstart.md), whose grep covers `feature-auth` alone. `core-design/theme` is deliberately outside the check — the palette is where the literals live.

**Checkpoint**: no colour literal remains in login UI code.

---

## Phase 7: User Story 5 - The message leaves the way it should (Priority: P3)

**Goal**: The message uses the standard state holder for queueing and dismissal, and leaves upward.

**Independent Test**: Fail a login twice quickly; both messages appear in order and each leaves by
moving up.

### Tests for User Story 5 ⚠️

- [X] T051 [P] [US5] `TEST/presentation/login/ui/LoginScreenSnackbarTest.kt` replacing `LoginScreenBannerTest.kt` — a message shows, leaves after 2600 ms, and a second message follows the first without either being dropped.
- [X] T052 [P] [US5] Extend `TEST/presentation/login/ui/LoginScreenMotionTest.kt` — with reduced motion the message appears and leaves without motion and still shows for its full duration.

### Implementation for User Story 5

- [X] T053 [US5] Create `IMPL/presentation/login/ui/LoginSnackbarHost.kt` — driven by `SnackbarHostState`, replicating the duration timer the private Material host owns, with vertical enter and upward exit and a reduced-motion path.
- [X] T054 [US5] In `IMPL/presentation/login/ui/LoginScreen.kt` replace `rememberTransientMessage` and `TransientBanner` with the host, resolving each `News.ShowMessage` (and its argument) to text before calling `showSnackbar`, and apply the design geometry — top anchored, 10 dp below the safe area, 20 dp margins, 14 dp radius.
- [X] T055 [US5] Rename `BANNER` to `SNACKBAR` in `IMPL/presentation/login/ui/LoginTestTags.kt` and update every reference.

**Checkpoint**: message motion, queueing, and timing all match the design.

---

## Phase 8: User Story 6 - A leaner auth data layer, agreed before it changes (Priority: P3)

**Goal**: The data layer is simplified only after a written proposal is approved.

**Independent Test**: The proposal exists and is approved, and no data-layer commit precedes it.

- [X] T056 [US6] Write `specs/003-auth-ui-refactor/data-layer-proposal.md`. No research item covers the data layer — the review happens here, by reading `IMPL/data/` in full. Cover at least these candidates: the Google-specific method on `domain/repository/AuthRepository.kt` and `data/repository/DefaultAuthRepository.kt`, the single-implementation `data/CurrentTime.kt` and `data/identity/NonceGenerator.kt` ports, the identical `data/local/SessionLocal.kt`/`SessionDto` pair behind `data/mapper/SessionMapper.kt`, `data/AuthStateSource.kt`, the `hasReadStorage` mutex, and the `Lazy<AuthRemoteDataSource>` cycle break — each with what changes, why the current form is excessive, the resulting structure, and the risks.
- [ ] T057 [US6] **GATE**: present the proposal and wait for approval. No file under `apps/mobile/feature-auth/impl/src/*/kotlin/app/yap/feature/auth/data/` may change before this task completes.
- [ ] T058 [US6] Implement the approved simplification across `IMPL/data/`, test-first for anything whose behaviour changes.
- [ ] T059 [US6] Confirm login, session renewal, and access-token behaviour are unchanged by running `TEST/data/` and `TEST/domain/` in full.

**Checkpoint**: the data layer is simplified with the requester's agreement on record.

---

## Phase 9: Polish & Cross-Cutting Concerns

- [X] T060 [P] Update `docs/mobile/003-dependency-injection.md` for the mutable back-stack tail, the `Navigator` binding, scene strategies, and entry decorators — Principle V requires the guide and the code to agree in the same PR.
- [X] T061 [P] Record the pre-release navigation dependency and its escalation rule in the PR description under Principle V.
- [X] T062 Run the structural greps from [quickstart.md](quickstart.md) — no colour literals, no catalogue references, no UI resource reachable from domain or data — and the SC-008 branch-history check from the same file: the first commit touching `*/feature/auth/data/*` must come after the commit that added `data-layer-proposal.md`.
- [X] T063 Verify SC-001 with the scratch-commit check in [quickstart.md](quickstart.md): enabling T-ID touches neither `LoginViewModel.kt` nor `LoginScreen.kt`.
- [X] T064 Run `./gradlew build` and `./gradlew :apps:mobile:shared-app:compileKotlinIosSimulatorArm64` and report the result plainly, including any failure.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: no dependencies. T002 is a gate — a failure stops the feature.
- **Foundational (Phase 2)**: depends on Setup. Blocks US1, US2, and US3.
- **US1 (Phase 3)** and **US2 (Phase 4)**: both depend only on Foundational and touch different files;
  they can run in parallel.
- **US3 (Phase 5)**: depends on US1 (the single entry point) and US2 (the roster it renders).
- **US4 (Phase 6)**: partly inverted. T047 and T048 are additive and MUST land **before** T044,
  because the sheet chrome and the selection rows read their roles from the theme and no colour
  literal may be parked in `core-design` as a stopgap. T046 moves with them, ahead of T047: the dark
  message colour changes value, which is a behaviour change and needs its failing test first. T049
  and T050 follow US3 as written.
- **US5 (Phase 7)**: independent of every other story — only the login screen's message changes.
- **US6 (Phase 8)**: independent, but gated on approval at T057.
- **Polish (Phase 9)**: after the stories that are being shipped.

### Within Each User Story

- Tests first, failing for the intended reason, then the smallest change that passes them.
- Contracts (`API/`) before implementations (`IMPL/`).
- Domain before presentation; presentation before Koin wiring.
- Deletions last, so the build stays green at every step.

### Parallel Opportunities

- T003 and T004 are different build files.
- Every test task inside a story is marked [P] — different test files, no shared state.
- T010 and T011 are different API files; T029 and T030 are different modules; T043 is independent of
  all Kotlin work in US3.
- US1 and US2 can be developed by different people at the same time; US5 can run alongside either.

---

## Parallel Example: User Story 3

```bash
# Tests first, all four in parallel:
Task: "RootBackStackTest in apps/mobile/app-root/src/commonTest/.../RootBackStackTest.kt"
Task: "SelectAuthProviderUiStateMapperTest in TEST/presentation/selectprovider/"
Task: "SelectAuthProviderViewModelTest in TEST/presentation/selectprovider/"
Task: "LoginViewModelTest navigation cases in TEST/presentation/login/"

# Then the two independent infrastructure pieces:
Task: "Navigator in apps/mobile/core-common/.../navigation/Navigator.kt"
Task: "BottomSheetSceneStrategy in apps/mobile/core-design/.../navigation/BottomSheetSceneStrategy.kt"
```

---

## Implementation Strategy

### MVP (User Story 1)

1. Phase 1 — upgrade and verify. A failure here stops everything by design.
2. Phase 2 — reshape `AuthProvider`.
3. Phase 3 — the single login entry point.
4. **Stop and validate**: Google sign-in still works, `LoginViewModel` holds no provider collection,
   and registering a second handler needs no edit to it.

That alone satisfies `fix.md` item 3 and the two readiness criteria about the view model, on a
screen that still looks exactly as it does today.

### Incremental Delivery

1. Setup + Foundational → the type and the toolchain are in place.
2. US1 → the architecture claim is true and demonstrable.
3. US2 → provider decisions leave presentation.
4. US3 → the sheet becomes a screen and matches the design.
5. US4 → the remaining login colours move into the theme; its palette (T047, T048) landed earlier,
   before the sheet chrome that reads it.
6. US5 → the message behaves.
7. US6 → the data layer, once agreed.

Every step leaves a shippable app.

---

## Notes

- [P] means different files with no dependency on an incomplete task.
- Phase numbers are local to this file (1–9). The plan's implementation phases are numbered P0–P7 and
  carry their mapping to these in their own table; the `Phase 0` / `Phase 1` labels in `plan.md`'s
  documentation tree mean the spec-kit research and design steps, which are neither of those.
- Two tasks are deliberate gates: T002 stops on a link failure, T057 stops until the data-layer
  proposal is approved. Neither may be worked around.
- T006 is a knowingly temporary shim, removed by T042. It exists so Phase 2 leaves a green build.
- `isEnabled` has exactly two readers: `DefaultLoginUseCase` (T014), which decides the outcome, and
  the wiring guard (T017). `UiState.Provider.isEnabled` is carried for the mapper's contract and is
  never rendered — the design makes every visible row tappable, and nothing in the sheet may branch
  on it.
- The `sendResult<AuthProvider>` type argument in T038 is load-bearing: the result bus keys by
  `T::class.toString()`, and an inferred subclass would key the result where nothing is listening.
- Commit after each task or logical group; report verification results plainly, including failures.

---

## Post-implementation audit

A six-lens adversarial audit ran over the finished change (contracts, requirements, navigation,
domain/DI, presentation and tests, design and guides), and every finding was handed to an
independent verifier told to refute it. Twenty findings were raised, six survived, and all six were
fixed with the same test-first discipline as the tasks above:

| Finding | Fix |
| --- | --- |
| `RootBackStack.keys` is cold, so its tail reset fired on every fresh subscription and the open sheet was dropped on any lifecycle STOP-START | The base a destination was pushed onto is held on the singleton and compared, so only a real change drops the tail (`RootBackStackTest`) |
| Two unqualified `single<ProviderLogin>` definitions share one Koin index key and override each other | Each login path is declared by its own type and `bind ProviderLogin::class`; the wiring guard now also asserts no two handlers claim one provider |
| `verify()` examines no dependency of a definition written as `factory<Contract> { Default(...) }` — an interface has no constructors | `FeatureAuthModuleTest` resolves `LoginUseCase` and `ObserveAuthProvidersUseCase` for real; the guard was proved to fail without the `Platform` binding |
| The scene-strategy and decorator lists were rebuilt on every composition, re-keying `rememberSceneState` and tearing down an open sheet | Both lists are `remember`ed, and `BottomSheetScene` gained the value `equals`/`hashCode` that `DialogScene` has |
| `rememberViewModelStoreNavEntryDecorator()` was missing, so no view model was scoped to — or cleared with — its entry | Added to `entryDecorators`; the contract and `docs/mobile/003-dependency-injection.md` record it |
| A queued message replaced the one on screen without the first playing its upward exit | The host owns a visibility flag and lets the exit finish before dismissing, so the queue is one-out-one-in (`LoginScreenSnackbarTest`) |
| `navigate` appended unconditionally, so a repeated key put two entries with one content key on the stack | A destination already on top is not pushed again (`RootBackStackTest`) |
| The sheet's section label was rendered in mixed case | Upper-cased at the point of use, as the design specifies (`SelectAuthProviderScreenTest`) |
