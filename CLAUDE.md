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
  isolation, Test-First, convention-plugin-governed build, the Detekt gate,
  Simplicity/YAGNI, and branch naming. Applies to every change.
- **[`docs/README.md`](docs/README.md)** — top-level documentation routing by area.
- **[`docs/mobile/README.md`](docs/mobile/README.md)** — routing index for mobile
  domain, data, repositories, and presentation rules.
- **[`docs/server/README.md`](docs/server/README.md)** — routing index for server
  module, service, adapter, and persistence rules.
- **[`docs/shared/README.md`](docs/shared/README.md)** — DTO naming and wire-boundary
  rules for `shared/contract/*` and its mobile/server consumers.
- **[`docs/testing/README.md`](docs/testing/README.md)** — routing index for test
  structure, unit-test support, and backend integration verification. Read each
  relevant document before changing tests or shared test support.

An intentional exception to a documented architecture rule must be justified in the
PR description or reflected by updating the owning guide. Do not silently work
around the rule.

When using Spec Kit, read the constitution and relevant `docs/*` guides before
finalizing `plan.md` or `tasks.md`. Generated feature artifacts refine behavior;
they do not override project rules. Work in this order:
`constitution → relevant docs → spec/plan/tasks → implementation → verification`.

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

`stubcall` (test stubs, see [`docs/testing/003-stubs.md`](docs/testing/003-stubs.md))
is resolved from the file-based Maven repository committed in `libs/`. Do not
`includeBuild` a sibling `stub-call` checkout: the committed repository keeps builds
independent from files outside this project.
