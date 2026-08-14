# Data Model: Login UI Package and Comment Cleanup

This change has no runtime entities. The model below is the authoritative inventory of the source
artefacts it touches: what moves, what stays, and how much comment text each module carries.

## Classification rule

A declaration belongs to `…presentation.login.ui` when it **draws or styles the screen, or names a
drawn element**. It stays in `…presentation.login` when it **produces or describes state**. The rule
is decided per file, and every current file falls cleanly on one side (research R2).

## Production files — `feature-auth/impl/src/commonMain/kotlin/app/yap/feature/auth/`

| File | Compose imports / `@Composable` | Destination | Why |
|------|--------------------------------|-------------|-----|
| `presentation/login/LoginScreen.kt` | 52 / 11 | `presentation/login/ui/` | Draws the screen |
| `presentation/login/AuthProviderSheet.kt` | 25 / 3 | `presentation/login/ui/` | Draws the provider sheet |
| `presentation/login/LegalLine.kt` | 13 / 2 | `presentation/login/ui/` | Draws the legal line |
| `presentation/login/LoginColors.kt` | 2 / 1 | `presentation/login/ui/` | Styles the screen; resolves colour roles per theme |
| `presentation/login/LoginTestTags.kt` | 0 / 0 | `presentation/login/ui/` | Names rendered elements; read only by composables and UI tests |
| `presentation/login/LoginViewModel.kt` | 0 / 0 | stays | Owns state, events, news |
| `presentation/login/LoginUiStateMapper.kt` | 0 / 0 | stays | Derives `UiState` from `DataState` |
| `presentation/login/AuthProviderCatalog.kt` | 0 / 0 | stays | Declares providers; feeds the mapper |
| `di/FeatureAuthModule.kt` | — | stays | Two imports repointed (`LoginScreen`, `LoginViewModel` — only the first changes package) |

## Test files — `feature-auth/impl/src/`

| File | Source set | Destination |
|------|-----------|-------------|
| `…/presentation/login/LoginScreenTestHost.kt` | `commonTest` | `…/presentation/login/ui/` |
| `…/presentation/login/LoginScreenContentTest.kt` | `commonTest` | `…/presentation/login/ui/` |
| `…/presentation/login/LoginScreenBannerTest.kt` | `commonTest` | `…/presentation/login/ui/` |
| `…/presentation/login/LoginScreenMotionTest.kt` | `commonTest` | `…/presentation/login/ui/` |
| `…/presentation/login/LoginScreenSemanticsTest.kt` | `commonTest` | `…/presentation/login/ui/` |
| `…/presentation/login/LoginScreenThemeTest.kt` | `commonTest` | `…/presentation/login/ui/` |
| `…/presentation/login/LoginScreenExtremesTest.kt` | `androidHostTest` | `…/presentation/login/ui/` |
| `…/presentation/login/LoginViewModelTest.kt` | `commonTest` | stays |
| `…/presentation/login/LoginUiStateMapperTest.kt` | `commonTest` | stays |
| `…/presentation/login/StubAuthProviderDeclaration.kt` | `commonTest` | stays |
| `…/presentation/ComposeUiTestCase.kt` (+ `.android.kt`, `.ios.kt`) | `commonTest` / `androidHostTest` / `iosTest` | stays — an `actual` must keep its `expect`'s package (research R3) |

**Invariants for the move**: no visibility widens (`internal` is module-scoped); no test tag string,
string resource, semantics label, or rendered element changes; file contents change only in the
`package` line, the import list, and — under the cleanup — comment lines.

## Comment cleanup inventory

893 comment lines across 116 of the 158 Kotlin files under `apps/`, `services/`, and `shared/` (170
including the build-logic tree). Nothing is preserved: a search for machine-read comment forms
returned zero hits (research R5).

| Module | Comment lines | Files with comments / total |
|--------|--------------:|----------------------------:|
| `apps/mobile/feature-auth/impl` | 399 | 49 / 57 |
| `services/server/feature-auth` | 203 | 25 / 26 |
| `apps/mobile/app-root` | 71 | 9 / 12 |
| `services/server/app` | 66 | 8 / 8 |
| `apps/mobile/feature-auth/api` | 65 | 9 / 10 |
| `apps/mobile/core-common` | 23 | 4 / 12 |
| `shared/contract/auth` | 18 | 3 / 4 |
| `apps/mobile/shared-app` | 16 | 3 / 3 |
| `apps/mobile/android-app` | 13 | 1 / 1 |
| `services/server/core-config` | 11 | 3 / 5 |
| `apps/mobile/core-design` | 4 | 1 / 2 |
| `apps/mobile/core-test` | 4 | 1 / 4 |
| `apps/mobile/core-network`, `services/server/core-database`, `services/server/core-security` | 0 | 0 |
| `convention-plugins/src/main` (Gradle build logic) | 0 | 0 / 12 |

**Out of scope**: every non-Kotlin file — `docs/*.md`, `*.gradle.kts`, `config/detekt/detekt.yml`
(which carries its own explanatory comment), `composeResources/**`, `robolectric.properties`, iOS and
Android project files.

## Documentation touched

| File | Change |
|------|--------|
| `docs/mobile/001-feature-boundaries.md:65` | Extend the child-package sentence to state that a slice's Compose code sits in a nested `ui` package beside its state code (research R7) |
