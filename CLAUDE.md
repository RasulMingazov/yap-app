# Yap App

Single Gradle monorepo: Kotlin Multiplatform mobile client (`apps/mobile/*`), modular
Kotlin/JVM server (`services/server/*`), and platform-neutral wire contracts
(`shared/contract/*`). See `README.md` for the module layout. Only the authentication
slice is scaffolded — most modules currently have build configuration and boundaries
but no application behavior yet.

## Rules

Project-wide governance and focused rules apply to this repo. Read only what is
relevant to the current change.

- **`.specify/memory/constitution.md`** — project-wide governance: module boundary
  isolation, convention-plugin-governed build, the Detekt gate, Simplicity/YAGNI,
  branch naming. Applies to every change.
- **[`docs/adr/README.md`](docs/adr/README.md)** — routing index for small, focused
  architecture decisions. Use its "Read when changing" column to select only the
  ADRs relevant to the current boundary, then read each selected ADR in full.
- **[`docs/testing/README.md`](docs/testing/README.md)** — routing index for unit-test
  structure, environments, and stubs. Read the relevant document before changing
  unit tests or shared test support.

An intentional exception to an ADR must be justified in the PR description (per the
constitution's Governance section) or superseded by a later ADR — not silently
worked around.

## Verification

- `./gradlew build` — compilation, tests, and Detekt across all modules; required for
  any repository-wide change.
- For KMP boundary changes, also compile the iOS target:
  `./gradlew :apps:mobile:shared-app:compileKotlinIosSimulatorArm64`.
- `./gradlew detekt` (or `:module:detekt` for one module) for Kotlin-only changes.
  Shared rule tuning lives in `config/detekt/detekt.yml`; prefer a narrow
  `@Suppress` at the declaration over widening a rule for a one-off false positive.

## Working tree and Git

Branch names MUST be prefixed by kind, per the constitution: `feature/...` for new
functionality, `tech/...` for technical/build/tooling changes with no behavior
change, and `test/...` for test-only changes.

Run `git config core.hooksPath .githooks` once per clone — enables the `pre-push`
hook that blocks pushing Detekt violations.

`stubcall` (test stubs, see [`docs/testing/003-test-stubs.md`](docs/testing/003-test-stubs.md))
is resolved from the file-based Maven repository committed in `libs/`. Do not
`includeBuild` a sibling `stub-call` checkout: the committed repository keeps builds
independent from files outside this project.
