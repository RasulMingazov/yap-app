---

description: "Task list for Login UI Package and Comment Cleanup"
---

# Tasks: Login UI Package and Comment Cleanup

**Input**: Design documents from `/specs/002-login-ui-package-cleanup/`

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md),
[data-model.md](data-model.md), [quickstart.md](quickstart.md)

**Tests**: No test tasks. The spec requests no behavior change, and Constitution III forbids creating
artificial tests for a structural or documentation-only change. Existing tests move with their
subjects and are the safety net.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: US1 = Compose code in its own package, US2 = source reads without commentary
- Paths are repository-relative from `/Users/rasulmingazov/Projects/yap-app`

## Path shorthand

- `LOGIN_MAIN` = `apps/mobile/feature-auth/impl/src/commonMain/kotlin/app/yap/feature/auth/presentation/login`
- `LOGIN_TEST` = `apps/mobile/feature-auth/impl/src/commonTest/kotlin/app/yap/feature/auth/presentation/login`
- `LOGIN_HOSTTEST` = `apps/mobile/feature-auth/impl/src/androidHostTest/kotlin/app/yap/feature/auth/presentation/login`

---

## Phase 1: Setup

**Purpose**: Put the change on the right branch and capture the baseline the verification steps
compare against.

- [ ] T001 Create and switch to branch `tech/002-login-ui-package-cleanup` from the current branch (kind prefix `tech/` per Constitution workflow gates — this change alters no behavior) — SKIPPED by user decision on 2026-08-14: git left untouched, work applied on `feature/001-login-screen`
- [X] T002 Run the comment-count command from `specs/002-login-ui-package-cleanup/quickstart.md` step 4 and confirm the baseline it reports — 893 comment lines across 116 Kotlin files — still matches the tree
- [X] T003 Run `./gradlew build` once before touching anything and confirm it is green, so any later failure is attributable to this change

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Make the working tree diffable — every verification step in
[quickstart.md](quickstart.md) reads `git diff`, which is unusable while unrelated edits are staged
alongside.

**⚠️ CRITICAL**: No story work begins until this is done.

- [ ] T004 Commit or stash all unrelated modifications currently in the working tree (see `git status`) so that the diff produced by this change contains only its own edits — SKIPPED by user decision: the 001-login-screen WIP stays uncommitted, so verification used a filesystem snapshot instead of `git diff` (see research R6)

**Checkpoint**: `git status` shows a clean tree; story work can begin.

---

## Phase 3: User Story 1 - Compose code lives in its own package (Priority: P1) 🎯 MVP

**Goal**: Every declaration that draws or styles the login screen sits in
`app.yap.feature.auth.presentation.login.ui`; state-producing code stays one package up.

**Independent Test**: Move the files, leave comments untouched, and confirm
`./gradlew :apps:mobile:feature-auth:impl:build` is green with every existing login test passing and
no rendered output, semantics, or tag value changed.

### Production move

Each move is: `git mv` into `ui/`, change the `package` line to
`app.yap.feature.auth.presentation.login.ui`, add imports for the declarations left behind
(`LoginViewModel`, `LoginUiStateMapper`, `AuthProviderDeclaration`/`AuthProviderCatalog`) as needed,
and keep every declaration `internal` — no visibility widens (research R4).

- [X] T005 [P] [US1] Move `LOGIN_MAIN/LoginScreen.kt` to `LOGIN_MAIN/ui/LoginScreen.kt` and repoint its package and imports
- [X] T006 [P] [US1] Move `LOGIN_MAIN/AuthProviderSheet.kt` to `LOGIN_MAIN/ui/AuthProviderSheet.kt` and repoint its package and imports
- [X] T007 [P] [US1] Move `LOGIN_MAIN/LegalLine.kt` to `LOGIN_MAIN/ui/LegalLine.kt` and repoint its package and imports
- [X] T008 [P] [US1] Move `LOGIN_MAIN/LoginColors.kt` to `LOGIN_MAIN/ui/LoginColors.kt` and repoint its package and imports
- [X] T009 [P] [US1] Move `LOGIN_MAIN/LoginTestTags.kt` to `LOGIN_MAIN/ui/LoginTestTags.kt`, repoint its package, and drop the stray leading space before the `package` keyword
- [X] T010 [US1] Repoint the `LoginScreen` import in `apps/mobile/feature-auth/impl/src/commonMain/kotlin/app/yap/feature/auth/di/FeatureAuthModule.kt:25` to `app.yap.feature.auth.presentation.login.ui.LoginScreen`, leaving the `LoginViewModel` import at line 26 unchanged (depends on T005)
- [X] T011 [US1] Confirm `LOGIN_MAIN/LoginViewModel.kt`, `LOGIN_MAIN/LoginUiStateMapper.kt`, and `LOGIN_MAIN/AuthProviderCatalog.kt` were not moved and still declare package `app.yap.feature.auth.presentation.login`

### Test move

- [X] T012 [US1] Move `LOGIN_TEST/LoginScreenTestHost.kt` to `LOGIN_TEST/ui/LoginScreenTestHost.kt`, repoint its package, and import `LoginViewModel` from the parent package (depends on T005)
- [X] T013 [US1] Move `LOGIN_TEST/LoginScreenContentTest.kt`, `LOGIN_TEST/LoginScreenBannerTest.kt`, `LOGIN_TEST/LoginScreenMotionTest.kt`, `LOGIN_TEST/LoginScreenSemanticsTest.kt`, and `LOGIN_TEST/LoginScreenThemeTest.kt` into `LOGIN_TEST/ui/`, repointing packages and adding an import for `app.yap.feature.auth.presentation.ComposeUiTestCase` (depends on T012)
- [X] T014 [US1] Move `LOGIN_HOSTTEST/LoginScreenExtremesTest.kt` to `LOGIN_HOSTTEST/ui/LoginScreenExtremesTest.kt` and repoint its package and imports (depends on T012)
- [X] T015 [US1] Confirm `LOGIN_TEST/LoginViewModelTest.kt`, `LOGIN_TEST/LoginUiStateMapperTest.kt`, `LOGIN_TEST/StubAuthProviderDeclaration.kt`, and the `ComposeUiTestCase` `expect`/`actual` trio in `.../presentation/` were not moved — an `actual` must keep its `expect`'s package (research R3)

### Documentation

- [X] T016 [P] [US1] Update `docs/mobile/001-feature-boundaries.md:65` so the child-package sentence also states that a slice's Compose code sits in a nested `ui` package beside its state code (FR-011, Constitution V)

### Verify the story

- [X] T017 [US1] Run `./gradlew :apps:mobile:feature-auth:impl:detekt` and `./gradlew :apps:mobile:feature-auth:impl:build`; every previously passing login test must pass, none skipped or weakened
- [X] T018 [US1] Run [quickstart.md](quickstart.md) steps 2 and 3: confirm the diff contains only renames plus `package`/`import` lines, that `LoginTestTags` constants are byte-identical to before, and that the boundary greps print nothing

**Checkpoint**: US1 is complete and independently deliverable — comments are still untouched.

---

## Phase 4: User Story 2 - Source reads without commentary (Priority: P2)

**Goal**: Zero explanatory comments remain in Kotlin sources repository-wide.

**Independent Test**: Strip comments without moving anything else, then confirm `./gradlew build` is
green and `git diff -U0` shows no non-comment line removed.

**Method** (research R6): edit files; never run a repository-wide `sed`. String literals contain
`jdbc:postgresql://…` and `https://…`, and KDoc continuation lines start with `*`. Nothing is
preserved — a survey found zero machine-read comments in the repository (research R5). Delete KDoc
blocks and `//` lines whole, including any now-orphaned blank line pattern, and change no other line.

- [X] T019 [P] [US2] Remove all comments from `apps/mobile/feature-auth/impl/src/commonMain/kotlin/app/yap/feature/auth/data/**` and `.../domain/**` (12 files, 116 lines)
- [X] T020 [P] [US2] Remove all comments from `apps/mobile/feature-auth/impl/src/commonMain/kotlin/app/yap/feature/auth/di/FeatureAuthModule.kt` and `.../src/iosMain/kotlin/app/yap/feature/auth/di/FeatureAuthModule.ios.kt` (19 lines)
- [X] T021 [P] [US2] Remove all comments from the login presentation files in `LOGIN_MAIN/` and `LOGIN_MAIN/ui/` — `AuthProviderCatalog.kt`, `LoginUiStateMapper.kt`, `LoginViewModel.kt`, `ui/LoginScreen.kt`, `ui/AuthProviderSheet.kt`, `ui/LegalLine.kt`, `ui/LoginColors.kt`, `ui/LoginTestTags.kt` (103 lines; `LoginColors`' KDoc fact is already committed in `specs/001-login-screen/research.md` R9 — research R8)
- [X] T022 [P] [US2] Remove all comments from `apps/mobile/feature-auth/impl/src/androidMain/**` and `.../src/iosMain/kotlin/app/yap/feature/auth/data/**` (5 files, 36 lines)
- [X] T023 [P] [US2] Remove all comments from the `feature-auth/impl` test source sets: `src/commonTest/**`, `src/androidHostTest/**`, `src/iosTest/**` including the moved `presentation/login/ui/**` tests (22 files, 125 lines)
- [X] T024 [P] [US2] Remove all comments from `apps/mobile/feature-auth/api/src/**` (9 files, 65 lines)
- [X] T025 [P] [US2] Remove all comments from `apps/mobile/app-root/src/**` (9 files, 71 lines)
- [X] T026 [P] [US2] Remove all comments from `apps/mobile/core-common/src/**`, `apps/mobile/core-design/src/**`, and `apps/mobile/core-test/src/**` (6 files, 31 lines)
- [X] T027 [P] [US2] Remove all comments from `apps/mobile/shared-app/src/**` and `apps/mobile/android-app/src/**` (4 files, 29 lines)
- [X] T028 [P] [US2] Remove all comments from `shared/contract/auth/src/**` (3 files, 18 lines)
- [X] T029 [P] [US2] Remove all comments from `services/server/app/src/**` (8 files, 66 lines)
- [X] T030 [P] [US2] Remove all comments from `services/server/feature-auth/src/main/**` (13 files, 125 lines)
- [X] T031 [P] [US2] Remove all comments from `services/server/feature-auth/src/test/**` (12 files, 78 lines) — note the `jdbc:postgresql://` literal in `persistence/PostgresTestSupport.kt` must survive intact
- [X] T032 [P] [US2] Remove all comments from `services/server/core-config/src/**` (3 files, 11 lines) — note the `jdbc:postgresql://` literals in `AppConfigLoader.kt` and `DatabaseConfig.kt` must survive intact
- [X] T033 [P] [US2] Confirm `convention-plugins/src/main/kotlin/**` (12 build-logic files) is still comment-free — it is in scope under FR-007 and carried 0 comment lines at baseline, so this is a check, not an edit
- [X] T034 [US2] Confirm nothing outside Kotlin sources was touched: `docs/**`, `*.gradle.kts`, `config/detekt/detekt.yml` (which keeps its own comment), `composeResources/**`, `robolectric.properties`, and the Android/iOS project files are unchanged (FR-010)
- [X] T035 [US2] Run [quickstart.md](quickstart.md) step 4: the comment count must report `0`, the machine-read-form grep must return nothing, and the `git diff -U0` filter must print no removed non-comment line (FR-009)

**Checkpoint**: US2 is complete; both stories are done.

---

## Phase 5: Polish & Verification

- [X] T036 Run `./gradlew build` from the repository root and confirm compilation, tests, and Detekt pass across all modules
- [X] T037 Run `./gradlew :apps:mobile:shared-app:compileKotlinIosSimulatorArm64` — required because the moved files are `commonMain` and the KMP boundary is touched
- [X] T038 Run [quickstart.md](quickstart.md) step 6 (`grep -rn 'presentation/login' docs/ CLAUDE.md README.md`) and confirm no guide contradicts the new layout, leaving `specs/001-login-screen/*` as the historical record it is
- [ ] T039 Write the PR description stating both halves of the change, the verification commands actually run, and — per Constitution V — that `docs/mobile/001-feature-boundaries.md` was updated alongside the code rather than worked around — NOT DONE: no branch or PR exists while git is untouched; the description text is drafted in the implementation report

---

## Dependencies & Execution Order

### Phase dependencies

- **Setup (T001–T003)**: no dependencies.
- **Foundational (T004)**: depends on Setup; blocks both stories.
- **US1 (T005–T018)**: depends on T004.
- **US2 (T019–T035)**: depends on T004 only — it is independently deliverable. It is sequenced after
  US1 because T021 and T023 name post-move paths; if US2 runs first, use the pre-move paths under
  `LOGIN_MAIN/` and `LOGIN_TEST/` instead.
- **Polish (T036–T039)**: depends on whichever stories were delivered.

### Within US1

Production moves (T005–T009) → import repoint (T010) → test moves (T012–T014) → verification
(T017–T018). T011, T015, and T016 are independent checks.

### Within US2

All fourteen cleanup tasks touch disjoint file sets and are fully parallel. Verification
(T034–T035) runs after all of them.

### Parallel opportunities

- T005–T009 (five different files, same destination package)
- T016 (documentation) alongside any US1 code task
- T019–T032 — the widest fan-out in this change: fourteen disjoint module or source-set scopes

---

## Parallel Example: User Story 2

```bash
# Fourteen disjoint scopes, no shared file between them:
Task: "Remove comments from apps/mobile/feature-auth/api/src/**"
Task: "Remove comments from apps/mobile/app-root/src/**"
Task: "Remove comments from services/server/app/src/**"
Task: "Remove comments from services/server/feature-auth/src/main/**"
Task: "Remove comments from services/server/feature-auth/src/test/**"
Task: "Remove comments from shared/contract/auth/src/**"
# ...and the eight remaining scopes from T019–T032
```

---

## Implementation Strategy

### MVP (User Story 1 only)

1. Setup (T001–T003) → Foundational (T004) → US1 (T005–T018).
2. **Stop and validate**: module build green, diff is renames plus package/import lines only, tags
   byte-identical.
3. This is shippable on its own — the package split is the change the user asked for first.

### Incremental delivery

Commit US1 and US2 separately. A single commit mixing renames with 893 deleted lines is unreviewable;
split, the first commit is a rename set and the second is a pure deletion, and `git diff -M` proves
each claim on its own.

---

## Notes

- No new test is written; Constitution III forbids artificial tests for a change with no behavior.
- No `@Suppress` is added or removed, and `config/detekt/detekt.yml` is not modified (research R9).
- Commit after each task or logical group; stop at the US1 checkpoint to validate independently.
- Report verification honestly — a skipped or failing command is stated, not omitted.
