---
description: "Task list for the Login Screen feature"
---

# Tasks: Login Screen

**Branch**: `feature/001-login-screen` | **Feature ID**: `001-login-screen` | **Refreshed**: 2026-08-15

**Status**: all work below is delivered. The per-task list that drove the implementation (T001 …
T237) has served its purpose and is replaced by this record; the artefacts that still govern the
code are [spec.md](spec.md), [plan.md](plan.md), [research.md](research.md),
[data-model.md](data-model.md), and [contracts/](contracts/).

**Tests**: Constitution Principle III is non-negotiable and FR-066 restates it — each behavioural
unit landed test-first, at the boundary [research.md](research.md) R10 assigns it. Build files,
dependency bumps, file moves, `expect`/`actual` wrappers with no project-owned branch, and the
comment cleanup carry no test of their own, which is the constitution's own carve-out.

## What was delivered

| # | Tranche | Stories | Result |
| --- | --- | --- | --- |
| 1 | Module split, dependencies, entry points | — | `feature-auth` → `api` + `impl`; `MainActivity`, `App()`, the Xcode host, and the server process created |
| 2 | Wire contracts, platform ports, session storage, app shell, server schema | — | `shared/contract/auth`, `Platform`/`MotionPreferences`, `SessionStorage`, `V1__auth.sql` |
| 3 | Google verification, the two Google doors, credential adapters, the login screen | US1 | Login works on both platforms, including a device without Google's services |
| 4 | Repeat resolution and descriptive data | US2 | `unique (provider, provider_user_id)`; email, name, avatar refreshed and never matched on |
| 5 | Launch decision, splash, refresh seam, rotation, one-session rule | US3 | Tri-valued session state, `LaunchSessionRefresh`, sliding 90-day window |
| 6 | Cancellation, the 60-second bound, failure recovery, the message | US4 | Every attempt resolves within 60 seconds |
| 7 | Rendering packages and the repository-wide comment cleanup | — | `presentation/<slice>/ui`; zero comment lines in Kotlin sources |
| 8 | Navigation 3 upgrade, then the provider architecture | US5, US6 | One `LoginUseCase(provider)` over `getAll<ProviderLogin>()`; the roster owns visibility and selectability |
| 9 | The selection destination | US7 | `AuthNavKey.SelectAuthProvider`, `BottomSheetSceneStrategy`, `Navigator`, the result bus |
| 10 | Theme and message | US8, US9 | `YapColors` in `core-design`; `LoginSnackbarHost` with the design's 2600 ms and upward exit |
| 11 | Data layer | — | Two repositories, `SessionStore`, and typed HTTP outcomes in `core-network` ([research.md](research.md) R26) |
| 12 | Guide reconciliation and verification | — | Four guides and the Detekt config brought into agreement with the code (R29) |

## Remaining before release

- [ ] Configure both legal destinations; the app must not reach users while either is unset
      (FR-051, SC-011).
- [ ] Sign in with Apple, as its own feature, before the iOS App Store submission (spec.md
      Assumptions).
- [ ] Manual passes no automated check replaces: the absence of a login-screen flash on both
      platforms, the browser fallback on an image without Play services, and the iOS host built and
      run from Xcode.

## Post-implementation audit

A six-lens adversarial audit ran over the finished refactor, and every finding was handed to an
independent verifier told to refute it. Twenty findings were raised, eight survived, and all eight
were fixed test-first:

| Finding | Fix |
| --- | --- |
| `RootBackStack.keys` is cold, so its tail reset fired on every fresh subscription and an open sheet was dropped on any lifecycle STOP-START | The base a destination was pushed onto is held and compared, so only a real change drops the tail (`RootBackStackTest`) |
| Two unqualified `single<ProviderLogin>` definitions share one Koin index key and override each other | Each login path is declared by its own type and `bind ProviderLogin::class`; the wiring guard asserts no two handlers claim one provider |
| `verify()` examines no dependency of a definition written as `factory<Contract> { Default(...) }` | `FeatureAuthModuleTest` resolves `LoginUseCase` and `ObserveAuthProvidersUseCase` for real; the guard was proved to fail without the `Platform` binding |
| Scene-strategy and decorator lists were rebuilt on every composition, re-keying `rememberSceneState` and tearing down an open sheet | Both lists are `remember`ed, and `BottomSheetScene` gained the value `equals`/`hashCode` `DialogScene` has |
| `rememberViewModelStoreNavEntryDecorator()` was missing, so no view model was scoped to — or cleared with — its entry | Added to `entryDecorators`; the contract and `docs/mobile/003-dependency-injection.md` record it |
| A queued message replaced the one on screen without the first playing its upward exit | The host owns a visibility flag and lets the exit finish before dismissing (`LoginScreenSnackbarTest`) |
| `navigate` appended unconditionally, so a repeated key put two entries with one content key on the stack | A destination already on top is not pushed again (`RootBackStackTest`) |
| The sheet's section label rendered in mixed case | Uppercased at the point of use, as the design specifies (`SelectAuthProviderScreenTest`) |

## Notes for the next feature

- `isEnabled` has exactly two readers: `DefaultLoginUseCase`, which decides the outcome, and the
  wiring guard. Nothing renders it — the design makes every visible row tappable — so the selection
  row carries the provider rather than a copy of the flag.
- The shared result key is load-bearing: the result bus otherwise keys by type name, and an inferred
  key would deliver the result where nothing is listening.
- Adding a provider is one `ProviderLogin` binding, one roster entry, one `AuthProviderUiMapper`
  branch, and one drawable.
- The empty-roster state and a roster that re-emits are out of scope until the roster is fed
  remotely; both arrive with it.
