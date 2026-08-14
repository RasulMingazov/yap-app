# Quickstart: validating the refactor

Prerequisites: a clone with `git config core.hooksPath .githooks` already run, and the branch
`feature/003-auth-ui-refactor` checked out.

## Commands

| Purpose | Command |
| --- | --- |
| Everything — compilation, tests, Detekt | `./gradlew build` |
| The auth slice alone, while iterating | `./gradlew :apps:mobile:feature-auth:impl:allTests :apps:mobile:feature-auth:impl:detekt` |
| Theme and navigation modules | `./gradlew :apps:mobile:core-design:build :apps:mobile:core-common:build :apps:mobile:app-root:build` |
| KMP boundary — required; `api`, `core-common`, and the navigation dependency all change | `./gradlew :apps:mobile:shared-app:compileKotlinIosSimulatorArm64` |
| Wiring guard | `./gradlew :apps:mobile:feature-auth:impl:androidHostTest --tests '*FeatureAuthModuleTest*'` |

`./gradlew build` must pass before the work is reported complete. A failing or skipped run is stated
plainly, not omitted.

## Phase 0 gate — the navigation upgrade

Run **before** any code depends on it, because third-party artefacts were built against the older
runtime:

```bash
./gradlew build
./gradlew :apps:mobile:shared-app:compileKotlinIosSimulatorArm64
./gradlew :apps:mobile:app-root:dependencies --configuration androidRuntimeClasspath | rg navigation3
```

Expect `navigation3-runtime:1.2.0-alpha04` and `navigation3-ui:1.2.0-alpha02` with no conflict
resolution notes, and `koin-compose-navigation3` and `lifecycle-viewmodel-navigation3` resolving
against them without error. If Koin's entry DSL fails to link, stop here — the rest of the plan
depends on it.

## Scenario checks

Each maps to a success criterion in [spec.md](spec.md). Automated where the column says so; the rest
are run on a device or emulator.

| # | Scenario | Expected | Automated by |
| --- | --- | --- | --- |
| 1 | Tap the primary action | the selection sheet opens as its own destination, sliding up | UI test on `LoginScreen`; manual for the animation |
| 2 | Choose Google | the sheet animates closed, the button shows progress, sign-in runs | `LoginViewModelTest`, manual end to end |
| 3 | Look for Apple on Android | it is not in the list at all | `DefaultObserveAuthProvidersUseCaseTest` |
| 4 | Choose T-ID | no login runs and the "not yet available" message appears | `LoginViewModelTest`, `DefaultLoginUseCaseTest` |
| 5 | Dismiss the sheet — system back, swipe, or scrim | it closes, nothing is returned, no login starts, the login screen is idle | `RootBackStackTest` for system back; manual for swipe and scrim — neither reaches the view model, so no test can observe them |
| 6 | Fail a login twice quickly | two messages in order, neither dropped | `LoginViewModelTest` for the news, UI test for sequencing |
| 7 | Watch a message dismiss | it leaves upward, not by fading in place, after 2600 ms | UI test asserting the transition and timing |
| 8 | Switch to dark theme | the message keeps the light-theme colour; nothing else changes tone | `LoginScreenThemeTest` |
| 9 | Enable reduced motion | the message appears and leaves without motion, still readable for its full duration | `LoginScreenMotionTest` |
| 10 | Compare the sheet against `screen_login.dc.html` | marks, handle, border, radius, paddings, and label tracking match | `SelectAuthProviderUiStateMapperTest` for the marks; manual for the chrome |
| 11 | Count the taps from the login screen to a completed Google sign-in | two, exactly as today — SC-007 | manual |
| 12 | Rotate the device with the sheet open | the sheet survives with its roster, and no login starts twice | manual — the back stack is a Koin `single`; process death deliberately returns to the login screen instead |
| 13 | Look for a message raised behind the sheet | none exists: the sheet closes before any login runs | manual review that no path reports an outcome while the sheet is up |

The remaining edge case — the roster changing while the sheet is open — is unreachable today: the
local roster is a pure function of `Platform` and re-emits nothing after its first value. It becomes
testable with the remote roster, alongside the empty-roster state `spec.md` puts out of scope.

## Structural checks

Cheap greps that stand in for the structural success criteria:

```bash
# SC-002 — no colour literals in login UI (brand colours live inside the drawables, not Kotlin).
# The sheet chrome sits in core-design/navigation, so it is in scope; core-design/theme is not —
# YapColors is the one file where literals belong.
rg 'Color\(0x' apps/mobile/feature-auth \
  apps/mobile/core-design/src/commonMain/kotlin/app/yap/core/design/navigation \
  --glob '!**/build/**'                                                # expect: no matches

# FR-009 — the roster is the single source of provider visibility and selectability
rg 'AuthProvider' \
  apps/mobile/feature-auth/impl/src/commonMain/kotlin/app/yap/feature/auth/presentation/login   # expect: no matches

# SC-001 — exactly one table turns a provider into display data, so adding one edits one file
rg -l 'is AuthProvider\.' apps/mobile --glob '!**/build/**'   # expect: AuthProviderResources.kt alone

# SC-005 — no provider knowledge left in the login state holder
rg 'AuthProvider\.|Catalog|providers' \
  apps/mobile/feature-auth/impl/src/commonMain/kotlin/app/yap/feature/auth/presentation/login/LoginViewModel.kt

# FR-010 — the catalogue is gone
rg 'AuthProviderCatalog|AuthProviderDeclaration' apps/mobile --glob '!**/build/**'   # expect: no matches

# FR-009 — no UI resource reachable from domain or data
rg 'generated.resources|StringResource|DrawableResource' \
  apps/mobile/feature-auth/impl/src/commonMain/kotlin/app/yap/feature/auth/{domain,data}
```

## SC-001 — adding a provider costs nothing upstream

The claim is checkable, not rhetorical. On a scratch commit, give T-ID a `ProviderLogin` binding and
mark it enabled in the roster, then confirm:

```bash
git diff --name-only
```

lists only the new login implementation, the Koin module, and the roster — and neither
`LoginViewModel.kt` nor `LoginScreen.kt`. Discard the commit afterwards.

## Gate for the data-layer work

`specs/003-auth-ui-refactor/data-layer-proposal.md` must exist and be approved before any file under
`apps/mobile/feature-auth/impl/src/*/kotlin/app/yap/feature/auth/data/` changes. Verify with:

```bash
git log --oneline --name-only feature/003-auth-ui-refactor -- '*/feature/auth/data/*'
```

The first such commit must come after the commit that added the proposal.
