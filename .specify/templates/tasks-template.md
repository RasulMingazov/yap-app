---
description: "Task list template for feature implementation"
---

# Tasks: [FEATURE NAME]

**Input**: Design documents from `/specs/[###-feature-name]/`

**Prerequisites**: `spec.md` and `plan.md`; include other artifacts only when the
plan actually requires them.

## Format

Use `[ID] [P?] [Story] Description with exact repository path`.

- **[P]** means the task can run in parallel because it changes different files
  and has no unfinished dependency.
- **[Story]** maps the task to a user story, such as `[US1]`.
- Every behavior task starts with a test that is observed failing for the expected
  reason. Do not add artificial tests for documentation, build-only changes,
  generated code, or trivial delegation.
- Use only real paths under `apps/mobile`, `services/server`, `shared`, or another
  directory selected in `plan.md`.
- Do not create empty modules, layers, packages, or infrastructure.

## Phase 1: Required setup

Include this phase only for prerequisites the selected user stories actually need.

- [ ] T001 [exact setup task and path]

**Checkpoint**: Required setup is verified; no speculative foundation was added.

---

## Phase 2: User Story 1 — [Title] (P1)

**Goal**: [Observable value delivered by this story]

**Independent Test**: [How this story is verified on its own]

### Failing tests

- [ ] T002 [P] [US1] Add [behavior/regression] test in [exact test path]
- [ ] T003 [US1] Run the targeted test and confirm it fails for [expected reason]

### Implementation

- [ ] T004 [US1] Implement the smallest passing change in [exact source path]
- [ ] T005 [US1] Refactor with the targeted test remaining green
- [ ] T006 [US1] Run the relevant module checks

**Checkpoint**: User Story 1 works and is independently testable.

---

## Phase 3: User Story 2 — [Title] (P2)

Repeat the same order: failing test → minimal implementation → refactor → module
verification. Omit the phase when the feature has no second independently valuable
story.

- [ ] T007 [P] [US2] Add a focused failing test in [exact test path]
- [ ] T008 [US2] Confirm the expected failure
- [ ] T009 [US2] Implement the smallest passing change in [exact source path]
- [ ] T010 [US2] Run the relevant module checks

---

## Final verification

- [ ] TXXX Verify every acceptance scenario from `spec.md`
- [ ] TXXX Run `./gradlew build`
- [ ] TXXX For KMP boundary changes, run `./gradlew :apps:mobile:shared-app:compileKotlinIosSimulatorArm64`
- [ ] TXXX Update only documentation affected by the delivered behavior

## Execution Rules

- Tests for a behavior are written and observed failing before its implementation.
- Preserve user-story independence; state real dependencies instead of hiding them.
- Parallel tasks never modify the same file or depend on unfinished work.
- Test behavior once at the boundary that owns it; follow `docs/testing/README.md`.
- Do not commit or push unless the user explicitly requests it.
