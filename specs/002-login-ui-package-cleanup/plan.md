# Implementation Plan: Login UI Package and Comment Cleanup

**Branch**: `tech/002-login-ui-package-cleanup` | **Date**: 2026-08-14 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/002-login-ui-package-cleanup/spec.md`

## Summary

Two behavior-neutral changes to the same working tree. First, the login slice's Compose code moves
from `app.yap.feature.auth.presentation.login` into a nested `…presentation.login.ui` package —
`LoginScreen`, `AuthProviderSheet`, `LegalLine`, `LoginColors`, `LoginTestTags`, and the Compose test
helper and screen tests follow — while `LoginViewModel`, `LoginUiStateMapper`, and
`AuthProviderCatalog` stay where they are. Second, every explanatory comment is deleted from Kotlin
sources repository-wide: 893 comment lines across 116 of 170 files, of which a survey found **none**
are machine-read, so nothing needs preserving. Only one import site (`FeatureAuthModule`) and one
sentence in `docs/mobile/001-feature-boundaries.md` reference the old layout. No production behavior
changes and no test is added; the existing suite plus Detekt is the safety net.

## Technical Context

**Language/Version**: Kotlin 2.x, Kotlin Multiplatform (`commonMain`/`androidMain`/`iosMain`) plus
Kotlin/JVM on the server

**Primary Dependencies**: Compose Multiplatform, Koin, Navigation 3 (mobile); Ktor, Exposed (server) —
none change

**Storage**: N/A — no data-layer change

**Testing**: `kotlin.test` + Compose UI tests on the Robolectric-backed Android host compilation
(`androidHostTest`), `iosTest` for the iOS half; server JUnit and Testcontainers tests unaffected in
content, only stripped of comments

**Target Platform**: Android and iOS via KMP, JVM server

**Project Type**: Gradle monorepo — mobile KMP client, modular JVM server, shared wire contracts

**Performance Goals**: N/A — no runtime path changes

**Constraints**: No non-comment line may change during the cleanup; no rendered output, semantics,
resource, or test tag value may change during the move; no declaration's visibility may widen

**Scale/Scope**: 8 production files and 8 test files in the login slice; 116 of the repository's 170
Kotlin files carry 893 comment lines, all of them under `apps/mobile/*`, `services/server/*`, and
`shared/contract/*` — the 12 build-logic files in `convention-plugins` are in scope but already carry
none

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-checked after Phase 1 design.*

| Principle | Gate | Status |
|-----------|------|--------|
| I. Feature-First Module Boundaries | No module is added or split; the new package is internal to `feature-auth/impl`. All moved declarations stay `internal` — Kotlin `internal` is module-scoped, so a nested package needs no widening (research R4). | PASS |
| II. Layered Dependencies | The split reinforces the layering: drawing code depends on the view model's nested `UiState`/`Event`/`News`, never the reverse. No `presentation → data` edge is created. | PASS |
| III. Test-First for Behavior Change | No behavior changes, so the constitution's own carve-out applies: artificial tests MUST NOT be created for a structural or documentation-only change. Existing tests move with their subjects and must keep passing unmodified in intent. | PASS |
| IV. Wire Contracts | Untouched — `shared/contract/*` is only stripped of comments. | PASS |
| V. Documented Rules Govern | `docs/mobile/001-feature-boundaries.md:65` states child presentation slices live in packages such as `presentation/login`; it is updated in the same change (research R7). Removing KDoc conflicts with no documented rule — no guide requires it and Detekt configures no comment rule (research R9). | PASS |
| Workflow gates | Branch kind is `tech/...` (no behavior change). Verification is `./gradlew build` plus the iOS target compile, since `commonMain` changes. | PASS |

No violations; Complexity Tracking is empty and omitted.

## Project Structure

### Documentation (this feature)

```text
specs/002-login-ui-package-cleanup/
├── plan.md              # This file
├── research.md          # Phase 0 output — decisions R1–R10
├── data-model.md        # Phase 1 output — file→package mapping and cleanup inventory
├── quickstart.md        # Phase 1 output — verification guide
├── checklists/
│   └── requirements.md  # Spec quality checklist
├── spec.md
└── tasks.md             # Phase 2 output (/speckit-tasks — NOT created here)
```

No `contracts/` directory: this change exposes no new interface to another module, feature, or the
wire. Every moved declaration is `internal` to `feature-auth/impl`, and the only cross-package
reference is an import inside the same module.

### Source Code (repository root)

```text
apps/mobile/feature-auth/impl/src/
├── commonMain/kotlin/app/yap/feature/auth/
│   ├── di/FeatureAuthModule.kt                    # two imports repointed
│   └── presentation/login/
│       ├── AuthProviderCatalog.kt                 # stays (state)
│       ├── LoginUiStateMapper.kt                  # stays (state)
│       ├── LoginViewModel.kt                      # stays (state)
│       └── ui/                                    # NEW
│           ├── AuthProviderSheet.kt               # moved
│           ├── LegalLine.kt                       # moved
│           ├── LoginColors.kt                     # moved
│           ├── LoginScreen.kt                     # moved
│           └── LoginTestTags.kt                   # moved
├── commonTest/kotlin/app/yap/feature/auth/presentation/
│   ├── ComposeUiTestCase.kt                       # stays (shared base, `presentation` package)
│   └── login/
│       ├── LoginUiStateMapperTest.kt              # stays
│       ├── LoginViewModelTest.kt                  # stays
│       ├── StubAuthProviderDeclaration.kt         # stays
│       └── ui/                                    # NEW
│           ├── LoginScreenBannerTest.kt           # moved
│           ├── LoginScreenContentTest.kt          # moved
│           ├── LoginScreenMotionTest.kt           # moved
│           ├── LoginScreenSemanticsTest.kt        # moved
│           ├── LoginScreenTestHost.kt             # moved
│           └── LoginScreenThemeTest.kt            # moved
└── androidHostTest/kotlin/app/yap/feature/auth/presentation/
    ├── ComposeUiTestCase.android.kt               # stays (actual must match expect package)
    └── login/ui/LoginScreenExtremesTest.kt        # moved

docs/mobile/001-feature-boundaries.md              # line 65 reconciled with the nested ui package

# Comment cleanup touches Kotlin sources only, across:
apps/mobile/{android-app,app-root,core-common,core-design,core-test,shared-app,feature-auth/{api,impl}}
services/server/{app,core-config,feature-auth}
shared/contract/auth
convention-plugins/src/main                        # in scope, already comment-free
```

**Structure Decision**: The existing KMP feature-module layout is kept unchanged. The only structural
addition is the `ui` sub-package under the login presentation package (and its mirror in the two test
source sets), which draws the line the project's own presentation guide already draws in prose —
state on one side, drawing on the other — into the directory tree.

## Phase 0 — Research

Complete. See [research.md](research.md): package name and boundary (R1–R3), visibility safety (R4),
comment inventory and the finding that no machine-read comment exists (R5), a safe removal method
given `//` inside string literals (R6), documentation reconciliation (R7), the one comment whose fact
lives elsewhere (R8), Detekt impact (R9), and the verification set (R10).

## Phase 1 — Design

Complete. [data-model.md](data-model.md) carries the authoritative file→package mapping, the
classification rule that decides each file, and the per-module comment inventory.
[quickstart.md](quickstart.md) is the verification guide: how to prove the move changed nothing and
the cleanup touched only comments.

## Post-Design Constitution Re-Check

Re-evaluated after Phase 1: unchanged, all gates PASS. The design adds no module, no abstraction, and
no public surface; it moves five files, mirrors six test files, repoints two imports, deletes
comments, and corrects one documentation sentence.
