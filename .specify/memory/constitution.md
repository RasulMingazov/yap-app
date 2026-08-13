<!--
Sync Impact Report
- Version change: none (unfilled template) → 1.0.0
- Bump rationale: initial ratification; every placeholder replaced with concrete,
  project-specific governance derived from `CLAUDE.md`, `README.md`, and `docs/*`.
- Modified principles: none (no prior named principles existed)
- Added sections:
  - Core Principles I–V (Feature-First Boundaries, Layered Dependencies,
    Test-First for Behavior Change, Wire Contracts at the Boundary,
    Documented Rules Govern)
  - Technology and Structural Constraints (was [SECTION_2_NAME])
  - Development Workflow and Quality Gates (was [SECTION_3_NAME])
  - Governance
- Removed sections: none
- Follow-up TODOs: none
-->

# Yap App Constitution

## Core Principles

### I. Feature-First Module Boundaries

Every product slice is a module that owns its behavior end to end, and its public surface is
the smallest thing other modules need.

- A mobile feature is exactly two Gradle modules: `api` (what other features may see) and
  `impl` (everything else). `impl` depends on its own `api`; `api` depends on nothing but
  `core-*`.
- A feature depends on a sibling feature's `api` only, never on its `impl`. Two features that
  need each other's `impl` are one feature.
- On the server, `core-*` modules MUST NOT depend on features, and neither features nor
  `core-*` may depend on `app`. Feature code is promoted to `core-*` only for independent
  reuse, build, lifecycle, or ownership.
- Declarations default to `internal`. Visibility widens only for a real, named boundary.
- No layer or abstraction is created before it owns behavior or a required boundary.

**Rationale**: Boundaries that are enforced by the module graph cannot be eroded by
convenience imports, which keeps slices independently buildable and testable as the codebase
grows past the authentication scaffold.

### II. Layered Dependencies Within a Feature

Inside a feature's `impl`, dependencies flow inward toward domain and never outward.

- `presentation` depends on `domain`, never on `data`.
- `data` implements ports owned by `domain`.
- `domain` depends on neither `presentation` nor `data`.
- Common code stays platform-neutral; platform source sets provide narrow adapters only.
- A view model reports navigation intent rather than navigating itself; destinations are
  declared in the owning feature's Koin module.
- Framework and transport types — rows, JWT claims, provider exceptions, Ktor types — MUST NOT
  surface as domain or feature models. Adapter failures are translated at their own boundary,
  and coroutine cancellation is preserved across it.

**Rationale**: A domain that depends on nothing is the only part of a feature that can be
reasoned about and tested without a device, a server, or a network.

### III. Test-First for Behavior Change (NON-NEGOTIABLE)

For a behavior change, a focused test is written first and run to confirm it fails for the
expected reason, then the smallest passing change is implemented.

- Tests MUST fail for the intended reason before implementation begins; a test that passes on
  first run has not established the behavior it claims.
- Artificial tests MUST NOT be created for documentation, build-only changes, generated code,
  or behavior the project does not own.
- New and changed tests follow `docs/testing/*`. Existing tests migrate when their behavior or
  setup changes — not opportunistically.
- Server behavior that depends on PostgreSQL semantics — constraints, concurrency, migrations —
  is verified against a real database per `docs/testing/003-backend-integration.md`, not
  against a mock.

**Rationale**: Writing the test first is what forces the boundary to be stated before the
implementation makes it convenient to skip; running it red is what proves the test is wired to
the behavior at all.

### IV. Wire Contracts at the Boundary

Types crossing the mobile/server wire live in `shared/contract/*` and stay wire-only.

- Every serialized wire type carries the `Dto` suffix and is named by its payload meaning.
- DTOs are not domain entities, database models, or feature results.
- Mobile maps DTOs to domain at the repository boundary; the server maps DTOs to feature models
  at the API boundary. No DTO travels past those points.
- Server-only HTTP contracts stay beside their routes rather than entering the shared contract.
- Credential and profile responses stay separate.

**Rationale**: A single shared type used as both wire format and domain model couples client
releases to server schema changes; mapping at one known boundary keeps each side free to
evolve.

### V. Documented Rules Govern, Exceptions Are Explicit

The guides under `docs/` are the source of truth for how code in this repository is written,
and departures from them are recorded rather than absorbed.

- Work proceeds in the order: relevant docs → implementation → verification. Only the guides
  relevant to the change under way are read.
- An intentional exception to a documented architecture rule MUST be justified in the PR
  description or reflected by updating the owning guide. Silently working around a rule is
  prohibited.
- Android platform mechanics — Navigation 3, Compose adaptive layout and theming, test-harness
  setup — come from the official `android-skills` plugin. Project guides state only what this
  project decides on top of it and MUST NOT restate or fork upstream mechanics.
- When a rule and the code disagree, one of the two is changed in the same PR; neither is left
  standing as a contradiction.

**Rationale**: Undocumented exceptions accumulate into a second, unwritten architecture that
new contributors and agents cannot discover, which is precisely what the routing docs exist to
prevent.

## Technology and Structural Constraints

- The repository is a single Gradle monorepo: Kotlin Multiplatform mobile client under
  `apps/mobile/*`, modular Kotlin/JVM server under `services/server/*`, and platform-neutral
  wire contracts under `shared/contract/*`.
- Shared Gradle policy is implemented by the included `convention-plugins` build. Individual
  modules declare only their role-specific plugins and dependencies; build logic MUST NOT be
  duplicated into module scripts.
- Mobile dependency injection is Koin, with each feature declaring its own module, including
  its `navigation<Key> { }` entries. The server wires dependencies manually in `app`.
- Server ownership is fixed: `core-config` owns environment loading and validation;
  `core-database` owns connection, migration bootstrap, and transaction infrastructure but not
  feature schemas; `core-security` owns token and credential issuance and verification; `app`
  owns process lifecycle, shared Ktor plugins, error mapping, health endpoints, and graph
  construction.
- Prefer one cohesive service per feature over one use-case type per endpoint. A policy is
  extracted only for an independent rule, state, or lifecycle.
- All Kotlin follows the official Kotlin coding conventions plus
  `docs/001-code-conventions.md`: `is` on every branch of a subtype `when`, alphabetical
  ordering within a semantic group where no domain order applies, and guard clauses where they
  remove nesting.
- `stubcall` is resolved from the file-based Maven repository committed in `libs/`. A sibling
  `stub-call` checkout MUST NOT be introduced via `includeBuild`; builds stay independent of
  files outside this project.

## Development Workflow and Quality Gates

- Branch names MUST be prefixed by kind: `feature/...` for new functionality, `tech/...` for
  technical, build, or tooling changes with no behavior change, and `test/...` for test-only
  changes.
- `git config core.hooksPath .githooks` is run once per clone, enabling the `pre-push` hook
  that blocks pushing Detekt violations.
- `./gradlew build` — compilation, tests, and Detekt across all modules — is REQUIRED for any
  repository-wide change.
- KMP boundary changes additionally compile the iOS target:
  `./gradlew :apps:mobile:shared-app:compileKotlinIosSimulatorArm64`.
- Kotlin-only changes run `./gradlew detekt`, or `:module:detekt` for a single module. Shared
  rule tuning lives in `config/detekt/detekt.yml`; a narrow `@Suppress` at the declaration is
  preferred over widening a rule for a one-off false positive.
- A change is reported complete only after its required verification command has been run and
  passed. Failing or skipped verification is stated plainly, not omitted.

## Governance

This constitution supersedes other practices, conventions, and habits in this repository. Where
it conflicts with a guide under `docs/`, this document wins and the guide is corrected.

**Amendment procedure**: Amendments are made by updating this file in a PR that states what
changed and why. An amendment that invalidates existing code MUST include the migration plan or
the follow-up work required to bring the repository back into compliance.

**Versioning policy**: This document is versioned as MAJOR.MINOR.PATCH.

- MAJOR — a principle is removed or redefined in a backward-incompatible way, or governance
  itself changes.
- MINOR — a principle or section is added, or existing guidance is materially expanded.
- PATCH — clarification, wording, or typo fixes that do not change meaning.

**Compliance review**: Every PR verifies compliance with these principles. Complexity and any
departure from a documented rule MUST be justified in the PR description under Principle V.
Runtime development guidance for agents and contributors lives in `CLAUDE.md` and the routing
indexes it points to; those documents implement this constitution and are updated alongside it.

**Version**: 1.0.0 | **Ratified**: 2026-08-13 | **Last Amended**: 2026-08-13
