# Yap App

Single Gradle monorepo: Kotlin Multiplatform mobile client (`apps/mobile/*`), modular
Kotlin/JVM server (`services/server/*`), and platform-neutral wire contracts
(`shared/contract/*`). See `README.md` for the module layout. Only the authentication
slice is scaffolded — most modules currently have build configuration and boundaries
but no application behavior yet.

## Rules

Two layers of rules govern this repo. Read the one relevant to what you're touching;
you don't need to read everything up front.

- **`.specify/memory/constitution.md`** — project-wide governance: module boundary
  isolation, convention-plugin-governed build, the Detekt gate, Simplicity/YAGNI,
  branch naming. Applies to every change.
- **`docs/adr/`** — architecture decision records with concrete conventions. Each ADR
  has an "Agent essentials" section at the top — read that first; read the full ADR
  only when the change actually restructures that boundary.

| ADR | Scope | Read when touching |
| --- | --- | --- |
| [0001](docs/adr/0001-unit-test-conventions.md) | `apps/mobile/*` + `services/server/*` | any unit test |
| [0002](docs/adr/0002-feature-module-clean-architecture.md) | `apps/mobile/*` | feature module structure, layering, DI |
| [0003](docs/adr/0003-data-layer-and-repository-conventions.md) | `apps/mobile/*` | mobile repositories, data sources, caching |
| [0004](docs/adr/0004-presentation-architecture.md) | `apps/mobile/*` | Compose components, models, mappers, navigation |
| [0005](docs/adr/0005-feature-first-backend-architecture.md) | `services/server/*` | server routes, services, persistence, module boundaries |

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

`stubcall` (test doubles, see ADR-0001) is resolved from the file-based Maven repo
committed in `libs/`. Do not `includeBuild` a sibling `stub-call` checkout from Gradle
configuration — the committed `libs/` repo is what makes builds work without it
present (e.g. in CI or another clone).
