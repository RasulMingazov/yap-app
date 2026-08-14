# Research: Login UI Package and Comment Cleanup

Findings are from the working tree at `feature/001-login-screen`, 2026-08-14.

## R1 — Package name and placement

**Decision**: `app.yap.feature.auth.presentation.login.ui`, nested under the existing login package,
mirrored as `.../presentation/login/ui/` in `commonMain`, `commonTest`, and `androidHostTest`.

**Rationale**: The user asked for `presentation.login.ui` by name. Nesting keeps the slice's identity
(`login`) above the layer split, so a future second slice repeats the pattern rather than competing
for a shared `ui` namespace. `docs/mobile/001-feature-boundaries.md` already endorses focused child
packages under `presentation`.

**Alternatives considered**: `presentation.ui.login` — inverts slice and layer, breaks the one-slice-
one-directory reading. A separate Gradle module — Constitution I forbids a layer that owns no boundary.

## R2 — What counts as "Compose stuff"

**Decision**: A file moves when it draws or styles the screen, or names a drawn element. Measured
against the tree: `LoginScreen.kt` (52 Compose imports, 11 `@Composable`), `AuthProviderSheet.kt`
(25/3), `LegalLine.kt` (13/2), `LoginColors.kt` (2/1 — a `@Composable` accessor resolving theme
colour roles), `LoginTestTags.kt` (0/0, but every constant names a rendered element and every reader
is a composable or a UI test).

**Stay**: `LoginViewModel.kt`, `LoginUiStateMapper.kt`, `AuthProviderCatalog.kt` — zero Compose
imports, zero composables, and all three are exercised by plain unit tests.

**Rationale**: The split matches the pipeline `docs/mobile/presentation/002-ui-compose.md` states:
domain → `DataState` → mapper → `UiState` → Compose. Everything left of the last arrow stays;
everything right of it moves. `LoginTestTags` is the one judgement call: it holds no Compose import,
but it is a vocabulary of rendered elements, and keeping it beside the mapper would leave the UI
tests importing from two packages for one concern.

**Alternatives considered**: Moving only files with Compose imports (leaves `LoginTestTags` orphaned);
moving the whole slice including the view model (deletes the distinction the change exists to create).

## R3 — Test placement

**Decision**: `LoginScreenTestHost.kt` and the six screen tests (`…ContentTest`, `…BannerTest`,
`…MotionTest`, `…SemanticsTest`, `…ThemeTest` in `commonTest`, `…ExtremesTest` in `androidHostTest`)
move into the mirrored `login/ui` test package. `LoginViewModelTest`, `LoginUiStateMapperTest`, and
`StubAuthProviderDeclaration` stay.

**Constraint found**: `ComposeUiTestCase` is an `internal expect abstract class` in package
`app.yap.feature.auth.presentation`, with `actual`s in `androidHostTest` and `iosTest`. An `actual`
must declare the same package as its `expect`, so this trio does **not** move; the relocated tests
import it instead.

**Rationale**: Tests follow their subject, which is what makes the new package readable as a unit.

## R4 — Visibility safety

**Decision**: No visibility widens. Every moved declaration keeps `internal`.

**Rationale**: Kotlin's `internal` is module-scoped, not package-scoped, so `FeatureAuthModule` and
the tests in the parent package continue to see the moved declarations after only an import change.
This satisfies FR-005 and Constitution I ("declarations default to `internal`") with no exception.

## R5 — Comment inventory

**Measured**: 893 comment lines in 116 of the repository's 170 Kotlin files (158 under `apps/`,
`services/`, `shared/`; 12 more in `convention-plugins`, all already comment-free) — 179 KDoc blocks
and 56 `//` line
comments plus their continuations. Per area: `feature-auth/impl` 399, `services/server/feature-auth`
203, `app-root` 71, `services/server/app` 66, `feature-auth/api` 65, `core-common` 23,
`shared/contract/auth` 18, `shared-app` 16, `android-app` 13, `core-config` 11, `core-design` 4,
`core-test` 4. Zero in `core-network`, `core-database`, `core-security`.

**Decision**: Delete all of them.

**Key finding**: A search for machine-read comment forms — `noinspection`, `ktlint-disable`, detekt
directives, `TODO`/`FIXME`, `region`/`endregion`, `@formatter`, generated-file markers, license or
SPDX headers — returned **zero hits** across every Kotlin source. FR-008's preservation list is
therefore empty in practice, and no judgement call is needed per file. Suppressions in this repository
are the `@Suppress` annotation, which is code, not a comment.

## R6 — Safe removal method

**Decision (as implemented)**: A whole-line strip driven by a small raw-string-aware script, gated on
three preconditions measured on this tree first:

1. no comment begins after code on any line (zero trailing comments across all 170 Kotlin files),
2. no line inside a raw string (`"""`) starts with `//`, `/*`, or `*` — checked in the six files that
   contain raw strings,
3. no nested block comments.

Under those three, a line-based pass cannot reach inside a literal: every comment occupies whole
lines, and the only construct that could hide a comment-looking line is tracked explicitly.

**Rationale**: The original decision here was per-file manual editing, because a naive
`s|//.*||` sweep would corrupt the comment-looking text inside string literals — four
`jdbc:postgresql://…` URLs in `core-config` and the server tests, plus `https://` URLs in the login
legal line and its test host. Measuring the preconditions removed that risk without the cost of 116
hand edits, and made the result provable rather than reviewed by eye.

**Verification (stronger than the original plan)**: the pre-change tree was snapshotted, and after the
strip every one of the 170 files was compared to its snapshot with comment, `package`, and `import`
lines filtered out of both sides. All 170 matched exactly — 0 files with a changed non-comment line
(FR-009). Line and file counts also reconcile per task group to the 893/116 baseline. `git diff` could
not serve here because the login feature is still uncommitted and most of its files are untracked.

**Alternatives considered**: a one-shot `sed`/`perl` regex sweep (rejected — corrupts literals); a
Kotlin PSI-based tool (rejected — disproportionate).

## R7 — Documentation reconciliation

**Decision**: Update `docs/mobile/001-feature-boundaries.md:65`, which reads "Keep child presentation
slices in focused packages such as `presentation/login`", to also state that a slice's Compose code
sits in a nested `ui` package beside its state code.

**Also checked**: `docs/mobile/presentation/002-ui-compose.md` describes the state/Compose boundary but
names no package path — no edit needed. `CLAUDE.md` and `README.md` name no login path. The
`specs/001-login-screen/*` artefacts reference old paths but are a historical record of a completed
feature and are left as written.

**Rationale**: Constitution V — when a rule and the code disagree, one of the two changes in the same
PR.

## R8 — Facts that only exist in a comment

**Decision**: Delete them; no fact is lost.

**Checked**: The two comments carrying non-obvious content are `LoginColors`' KDoc, which says the
palette was lifted from the design prototype `screen_login.dc.html` and points at
`specs/001-login-screen/research.md` R9 for the role→value table, and `ComposeUiTestCase`'s KDoc,
which explains why the JUnit runner comes from the platform half. The first fact is already committed
in `specs/001-login-screen/research.md`; the second is re-derivable from the `expect`/`actual` pair
itself. Both are removed under FR-007.

## R9 — Detekt impact

**Decision**: No Detekt configuration change; no `@Suppress` is added or removed.

**Rationale**: `config/detekt/detekt.yml` is 28 lines and configures only `complexity`, `naming`, and
`style` entries — no `comments` ruleset. Detekt's documentation rules (`UndocumentedPublicClass` and
siblings) are inactive by default, so removing KDoc cannot raise a finding. Removing lines can only
reduce `LongMethod` counts. The YAML file's own comment is not a Kotlin source and stays (FR-010).

## R10 — Verification set

**Decision**: `./gradlew build` for the repository-wide sweep, plus
`./gradlew :apps:mobile:shared-app:compileKotlinIosSimulatorArm64` because the moved files are
`commonMain` and the KMP boundary is touched. Detail in [quickstart.md](quickstart.md).

**Rationale**: Constitution's workflow gates require the full build for repository-wide changes and the
iOS compile for KMP boundary changes; a change is reported complete only after they pass.
