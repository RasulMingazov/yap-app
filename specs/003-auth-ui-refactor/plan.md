# Implementation Plan: Auth Provider Selection and Login Theming Refactor

**Branch**: `feature/003-auth-ui-refactor` | **Date**: 2026-08-14 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/003-auth-ui-refactor/spec.md`

## Summary

The login slice keeps provider knowledge, colour literals, and a hand-timed banner in presentation.
This feature moves each to the layer that owns it: a `ProviderLogin` port collected with Koin's
`getAll` gives one `LoginUseCase(provider)`; `ObserveAuthProvidersUseCase` becomes the single source
of which providers exist and how they may be used, each carrying its own visibility and
selectability; provider selection becomes a real Navigation 3 destination rendered as a bottom sheet
with its own state, mapper, and view model; the palette, the sheet chrome, the provider marks, and
the message's timing and motion are all lifted from the design; and the auth data layer is reviewed
and simplified last, behind a written proposal the requester approves before any `data/**` file
changes.

Two pieces of shared infrastructure appear because this is the first feature that needs them: a
`Navigator` contract, which the presentation guide already prescribes but no module provides, and a
`BottomSheetSceneStrategy`, which Navigation 3 does not ship at any version. One dependency moves:
Navigation 3 rises to the pre-release that carries the official result API.

## Technical Context

**Language/Version**: Kotlin 2.4.0, JVM target 17

**Primary Dependencies**: Compose Multiplatform 1.11.1 with Material 3 1.9.0, Koin 4.2.2, AndroidX
Lifecycle 2.11.0, kotlinx-coroutines 1.11.0, and Navigation 3 — `navigation3-runtime` **1.1.1 →
1.2.0-alpha04** and `navigation3-ui` **1.1.1 → 1.2.0-alpha02**, split into two catalogue references
([research.md](research.md) R3)

**Storage**: none added. `SessionStorage` (DataStore on Android, Keychain on iOS) is untouched by
P1–P6 and only reviewed in P7.

**Testing**: `kotlin-test` with `runViewModelTest` from `core-test`, `stubcall` stubs, Compose UI
tests executed on the host through Robolectric in `androidHostTest`, Koin `verify()` for wiring

**Target Platform**: Android minSdk 24 / compileSdk 37, iOS arm64 and simulator arm64

**Design source**: Claude Design project `0c49e08b-d7ab-4cd3-88be-8483024790e5`,
`screen_login.dc.html` — palette, sheet chrome, provider marks, message geometry and timing

**Performance Goals**: no additional work on the login critical path — the roster is a cold flow that
reads no storage and makes no network call. Motion uses standard Compose animations; no frame-rate
target is asserted, because nothing in this feature measures one

**Constraints**: no colour literal may remain in login UI code; `AuthProvider` carries no UI
resource; the iOS target must compile because `api` and `core-common` both change and the navigation
dependency moves; no `data/**` edit may precede the approved proposal

**Scale/Scope**: one feature slice and three shared modules — roughly 28 files touched, 15 added, and
7 deleted, moved, or renamed (all named under Project Structure)

## Constitution Check

*GATE: passed before `/speckit-plan` Phase 0 research; re-evaluated after its Phase 1 design — see
below. Implementation phases are P0–P7 and are numbered separately.*

### I. Feature-First Module Boundaries

| Check | Verdict |
| --- | --- |
| New behaviour stays in `feature-auth/impl` and `internal` | Pass — only `AuthNavKey.SelectAuthProvider`, `ObserveAuthProvidersUseCase`, `AuthProvider`, and the `LoginUseCase` signature widen, each a real boundary |
| `core-*` does not depend on features | Pass — `Navigator` and `BottomSheetSceneStrategy` name no feature type |
| A feature does not depend on `app-root` | Pass — this is why the scene strategy sits in `core-design` |
| No abstraction before it owns behaviour | Pass — `Navigator` and `ProviderLogin` each gain a consumer here; the roster deliberately gets no port, and the result travels on the library's own bus rather than a project-owned carrier |

### II. Layered Dependencies Within a Feature

| Check | Verdict |
| --- | --- |
| `presentation → domain`, never `data` | Pass — view models take use cases and `Navigator` only |
| `data` implements ports owned by `domain` | Pass — `ProviderLogin` is a domain port |
| No framework or transport type in domain models | Pass — `AuthProvider` carries two booleans and nothing else |
| A view model reports navigation intent | Pass — `Navigator`, replacing the `isProviderSheetVisible` flag that `docs/mobile/presentation/002-ui-compose.md` already forbids |

The chosen provider reaches `LoginViewModel` as an ordinary `Event` raised by the composable, which
reads it from the navigation result bus. No data source is injected into any view model.

### III. Test-First for Behaviour Change (NON-NEGOTIABLE)

Every behaviour change lands as a failing test first: roster emission and flags per platform,
registry lookup and `Unavailable`, outcome-to-news mapping, the mapper's marks and labels, snackbar
sequencing, timing, and exit motion, and the theme's single message colour. Dependency-version edits
and file moves carry no test of their own — [research.md](research.md) R11 lists which existing tests
change and why.

### IV. Wire Contracts at the Boundary

No `shared/contract/*` type changes. `SessionDto` stays a wire type and is only discussed in the
P7 proposal.

### V. Documented Rules Govern, Exceptions Are Explicit

| Item | Resolution |
| --- | --- |
| `fix.md` asked for both flags on the provider | Now satisfied literally — the sealed shape carries both, and both stay runtime values |
| The guides prescribe a `Navigator` that no module provides | The rule and the code disagree; this PR adds it, closing the contradiction |
| `UiState` currently carries a sheet visibility flag, which the UI guide forbids | Removed here |
| `docs/mobile/003-dependency-injection.md` describes a derived back stack only | Updated in this PR to cover the mutable tail, the `Navigator` binding, scene strategies, and entry decorators |
| Material 3 offers no customisable snackbar transition | Not a rule departure — upstream marks it a TODO; [research.md](research.md) R8 records the consequence |
| A pre-release dependency enters the build | Deliberate and requested: the official result API exists in no stable release. Recorded here so it is a decision, not a drift |

**Post-design re-evaluation**: improved. Adopting the library's result API removed a feature-owned
carrier and two use cases that existed only to keep it away from view models. No module was added,
no port has a single speculative implementation, and no `core-*` module depends on a feature.

## Project Structure

### Documentation (this feature)

```text
specs/003-auth-ui-refactor/
├── plan.md                     # This file
├── spec.md
├── research.md                 # /speckit-plan Phase 0 — R1..R12
├── data-model.md               # /speckit-plan Phase 1
├── quickstart.md               # /speckit-plan Phase 1
├── contracts/                  # /speckit-plan Phase 1
│   ├── core-modules.md
│   ├── feature-auth-api.md
│   └── presentation.md
├── data-layer-proposal.md      # P7 gate artefact, not yet written
├── checklists/requirements.md
└── tasks.md                    # /speckit-tasks output, not created here
```

### Source Code (repository root)

```text
gradle/libs.versions.toml                                    # navigation3 split into runtime + ui

apps/mobile/
├── core-common/src/commonMain/kotlin/app/yap/core/common/
│   └── navigation/Navigator.kt                              # new
├── core-design/src/commonMain/kotlin/app/yap/core/design/
│   ├── navigation/BottomSheetSceneStrategy.kt               # new — scene + design chrome
│   └── theme/
│       ├── YapColors.kt                                     # new — 18 roles, lifted
│       └── YapTheme.kt                                      # provides LocalYapColors; LocalIsDarkTheme removed
├── app-root/src/commonMain/kotlin/app/yap/app/root/
│   ├── App.kt                                               # onBack, sceneStrategies, entryDecorators
│   ├── di/AppRootModule.kt                                  # RootBackStack becomes a single, bound as Navigator
│   └── navigation/RootBackStack.kt                          # auth-derived base + mutable tail
└── feature-auth/
    ├── api/src/commonMain/kotlin/app/yap/feature/auth/api/
    │   ├── AuthNavKey.kt                                    # + SelectAuthProvider
    │   ├── entity/{AuthProvider,LoginOutcome}.kt            # sealed with two flags; Unavailable
    │   └── usecase/{LoginUseCase,ObserveAuthProvidersUseCase}.kt
    └── impl/src/commonMain/
        ├── composeResources/drawable/                        # new — three provider marks
        │   ├── ic_provider_apple.xml
        │   ├── ic_provider_google.xml
        │   └── ic_provider_t_id.xml
        └── kotlin/app/yap/feature/auth/
            ├── domain/
            │   ├── provider/{GoogleProviderLogin,ProviderLogin}.kt
            │   └── usecase/{DefaultLoginUseCase,DefaultObserveAuthProvidersUseCase}.kt
            ├── presentation/
            │   ├── AuthProviderResources.kt                   # new — provider → label + mark, one table
            │   ├── login/                                    # AuthProviderCatalog.kt deleted
            │   │   ├── LoginUiStateMapper.kt
            │   │   ├── LoginViewModel.kt
            │   │   └── ui/{LoginScreen,LoginSnackbarHost,LoginTestTags,LegalLine}.kt
            │   └── selectprovider/                           # new slice
            │       ├── SelectAuthProviderUiStateMapper.kt
            │       ├── SelectAuthProviderViewModel.kt
            │       └── ui/{SelectAuthProviderScreen,SelectAuthProviderTestTags}.kt
            └── di/FeatureAuthModule.kt
```

Deleted: `presentation/login/AuthProviderCatalog.kt`, `presentation/login/ui/AuthProviderSheet.kt`,
`presentation/login/ui/LoginColors.kt`, and the tests
`presentation/login/StubAuthProviderDeclaration.kt` and
`presentation/login/ui/LoginScreenBannerTest.kt`. Moved: `domain/usecase/GoogleLoginUseCase.kt` to
`domain/provider/GoogleProviderLogin.kt`, and with it
`commonTest/.../domain/usecase/GoogleLoginUseCaseTest.kt` to
`commonTest/.../domain/provider/GoogleProviderLoginTest.kt`. Tests mirror the source split, so the new
slice gets `commonTest/.../presentation/selectprovider/`.

**Structure Decision**: the existing two-module feature shape is kept. The one structural addition is
a second presentation slice, `presentation/selectprovider`, alongside `presentation/login` — the
package shape `docs/mobile/001-feature-boundaries.md` prescribes for a screen with its own state,
mapper, and view model, with rendering in a nested `ui` package.

## Implementation Phases

Ordered so each phase leaves the build green and the app working.

Phases here are numbered P0–P7 to keep them apart from `tasks.md`'s own Phase 1–9 and from the
`/speckit-plan` documentation phases above; the last column maps each one.

| Phase | Content | Spec story | tasks.md |
| --- | --- | --- | --- |
| P0 | Raise Navigation 3, split the catalogue reference, run `./gradlew build` and the iOS compile before any code depends on it. **If it does not link, stop and escalate** — no fallback is pre-authorised | — | Phase 1, T001–T004 |
| P1 | `ProviderLogin`, `DefaultLoginUseCase`, `LoginUseCase(provider)`, `LoginOutcome.Unavailable`; `LoginViewModel` drops its provider map | US1 | Phase 3, T007–T017 |
| P2 | `AuthProvider` becomes sealed with both flags; `ObserveAuthProvidersUseCase` takes over the catalogue's platform rules and ordering | US2 | Phase 2 (T005–T006) + Phase 4 (T018–T023) |
| P3 | `Navigator`, mutable `RootBackStack`, `BottomSheetSceneStrategy`, `AuthNavKey.SelectAuthProvider`, the result bus wiring, the new presentation slice; `AuthProviderCatalog` and `AuthProviderSheet` deleted | US3 | Phase 5, T024–T042 |
| P4 | Provider marks as drawables and the sheet chrome, both from the design — the chrome reads P5's palette, which lands first | US3 | Phase 5, T043–T045 |
| P5 | `YapColors` and the `YapTheme` provision (before P4), then the derived `ColorScheme` and the login UI reading roles; `LoginColors` and `LocalIsDarkTheme` deleted | US4 | Phase 6, T046–T050 |
| P6 | `LoginSnackbarHost` on `SnackbarHostState`, design geometry and 2600 ms timing, upward exit, reduced-motion path; the hand-timed banner deleted | US5 | Phase 7, T051–T055 |
| P7 | `data-layer-proposal.md`, **approval gate**, then the agreed simplification | US6 | Phase 8, T056–T059 |

Two seams the mapping makes visible: P2's sealed reshape is pulled ahead of P1 in `tasks.md`, into its
Foundational phase, because US1's code compiles against the new type; and `tasks.md` Phase 9 — docs,
PR record, structural checks, the full build — has no phase of its own here, since it belongs to no
single story.

P0 comes first because everything in P3 depends on the upgrade linking — including third-party
artefacts built against the older runtime. Koin 4.2.2 is the latest release, so there is no newer Koin
to move to if its navigation DSL fails to link; the requester chose to be asked at that point rather
than to pre-authorise any fallback, so the phase either passes or the feature pauses. P1 and P2 are
independent of P5 and P6 and may be reordered; P3 depends on P0, P1, and P2; P4 depends on P3 and on
the palette half of P5 — `YapColors` and its provision are additive and land before the sheet chrome,
so no colour literal is parked in `core-design`, while the rest of P5 (repointing the login screen,
deleting `LoginColors` and `LocalIsDarkTheme`) follows P4; P7 starts only after its proposal is
approved.

## The unavailable-provider path

Settled: the design's flow exactly, and the decision with its rejected alternatives is recorded once,
in [research.md](research.md) R12. Two consequences the rest of this plan depends on: the selection
screen reports the choice and nothing else — no use case, no outcome, no message — and the per-provider
wording is one parameterised string whose argument comes from `AuthProviderResources`, so no provider
branch enters `LoginViewModel`. `DefaultLoginUseCase` returns `Unavailable` for a provider that has no
handler **or** that the roster marks `isEnabled = false`, which is what FR-005 asks for.

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
| --- | --- | --- |
| A pre-release navigation dependency | The official result API exists in no stable release, and the requester chose it over a project-owned carrier | A feature-owned channel plus two use cases was the alternative, explicitly rejected as an invented mechanism |
| Two catalogue references for one library | `navigation3-ui` 1.2.0-alpha02 declares runtime 1.2.0-alpha04, and JetBrains publishes no runtime artefact | One shared version would either pin the runtime below what the UI was built against or rely on conflict resolution to silently fix it |
| `core-common` gains `navigation3-runtime` | `Navigator.navigate(key: NavKey)` puts `NavKey` on the module's API surface | A `String` or feature-local key type would lose type safety and duplicate what `feature-auth/api` already does with the same dependency |
| `core-design` gains the `yap.navigation3` plugin | `BottomSheetSceneStrategy` is a Compose scene both `app-root` and `feature-auth` need | A new `core-navigation` module would add a Gradle module for two files; `app-root` cannot host it because features may not depend on it |
| A hand-written bottom-sheet scene strategy | Navigation 3 ships none at any version; the official sample is a custom `OverlayScene` | Keeping the sheet as a `UiState` flag is what the requester asked to remove and what the UI guide forbids |
| A feature-owned snackbar host | Material 3's transition is private and marked TODO upstream; the design requires upward exit and 2600 ms | Wrapping the stock host leaves its fade running under the slide |
| `isMonochrome` on the row model | The design's marks are not uniform — two carry brand colours, one follows the theme | Branching on the provider inside the composable is exactly what the UI guide forbids |
| `AuthProviderResources` shared by two slices rather than owned by the sheet's mapper | The login screen needs the provider's name for the "not yet available" message after the sheet has closed | A provider `when` in `LoginViewModel` would break the criterion that adding a provider leaves it untouched; per-provider message strings would grow with every provider |
| A second view model in the feature | The selection sheet is its own destination with its own lifecycle — the condition the view-model guide names for extracting one | Keeping the state in `LoginViewModel` fails the readiness criterion that provider logic leaves it |
