<!--
Sync Impact Report
==================
Version change: 2.0.0 → 2.1.0 (MINOR — workflow guidance expanded)
Modified principles: none this amendment
Added sections:
  - Development Workflow & Quality Gates: branch naming convention
    (`feature/...`, `tech/...`, `test/...`)
Removed sections: none this amendment
Deferred/TODO placeholders: none
Templates requiring follow-up: none checked in this run — plan/spec/tasks templates
  read this constitution at runtime and were not modified here (see Scope Guard).

Previous entry (2.0.0):
Version change: 1.0.0 → 2.0.0 (MAJOR — principle removed)
Modified principles:
  - III. Static Analysis Gate (was IV) — renumbered, wording unchanged
  - IV. Simplicity & YAGNI (was V) — renumbered, wording unchanged
Removed sections:
  - Core Principles: III. Test-First Development — removed at user request
-->

# Yap App Constitution

## Core Principles

### I. Module Boundary Isolation
Feature modules MUST NOT depend on other feature modules' implementations. Only
entry-point/composition modules (e.g. `apps/mobile/app-root`,
`services/server/app`) MAY depend on multiple feature modules to wire them
together. Cross-cutting capability shared by both client and server MUST live
in `shared/contract/*` as a platform-neutral contract, not be duplicated or
reached into directly across the client/server boundary.
**Rationale**: The repository is structured explicitly around this boundary
(see README.md module layout). Enforcing it keeps features independently
buildable and testable, and prevents hidden coupling as the number of
features grows beyond the current auth slice.

### II. Convention-Plugin-Governed Build
All shared Gradle policy (Kotlin/Compose/Ktor setup, Android SDK levels,
serialization, static analysis wiring) MUST be expressed once as a plugin in
`convention-plugins/` and applied by role. Individual modules MUST declare
only the convention plugin(s) matching their role plus module-specific
dependencies; they MUST NOT re-declare compiler options, SDK versions, or
tool configuration that a convention plugin already owns.
**Rationale**: A single source of truth for build configuration avoids drift
between the many KMP/JVM modules in this monorepo and keeps onboarding a new
module a matter of picking the right plugin, not copying build scripts.

### III. Static Analysis Gate (NON-NEGOTIABLE)
Every module that applies a JVM/KMP convention plugin MUST run Detekt against
the shared ruleset in `config/detekt/detekt.yml` with
`buildUponDefaultConfig` enabled. Code MUST NOT be merged with unresolved
Detekt findings, and the shared ruleset MUST NOT be weakened or suppressed
per-module to pass a build; rule changes belong in the shared config, decided
deliberately and reviewed as a config change.
**Rationale**: `DetektPlugin` is already wired into the shared convention
plugins and applied uniformly. Treating it as advisory rather than a gate
would let inconsistent code quality creep in across modules silently.

### IV. Simplicity & YAGNI
Implementation MUST match the current spec — no speculative abstractions,
unused configuration flags, or infrastructure for features not yet
specified. When a module currently has no application behavior (as noted for
several scaffolded modules in README.md), it MUST stay a minimal build
target until a spec calls for real behavior, rather than accumulating
placeholder code.
**Rationale**: The project is explicitly an early-stage scaffold ("only the
authentication slice is scaffolded"). Keeping unbuilt areas empty rather than
speculatively fleshed out keeps the true state of the system legible.

## Technology Stack Constraints

- Mobile client: Kotlin Multiplatform with Compose Multiplatform, targeting
  Android and iOS (`iosArm64`, `iosSimulatorArm64`); JVM target 17.
- Server: Kotlin/JVM with Ktor, modularized by feature and core capability
  (config, database, security).
- Shared wire contracts live under `shared/contract/*` and MUST remain
  platform-neutral (no Android/iOS/Ktor-specific types leaking into them).
- Dependency resolution is centralized (`dependencyResolutionManagement`,
  version catalog via `libs`); modules MUST source versions from the catalog
  rather than hardcoding them.
- New modules MUST be registered in `settings.gradle.kts` and apply an
  existing convention plugin where one fits their role; a new convention
  plugin is only added when an existing one genuinely does not fit.

## Development Workflow & Quality Gates

- `./gradlew build` (which runs compilation, tests, and Detekt across
  modules) MUST pass before a change is considered done.
- Changes touching `shared/contract/*` MUST be reviewed for impact on both
  `apps/mobile/*` and `services/server/*` consumers in the same change set —
  contracts and their consumers MUST NOT drift out of sync across separate,
  uncoordinated changes.
- Code review MUST verify compliance with Module Boundary Isolation and
  Convention-Plugin-Governed Build before functional review, since violations
  here are structural and costly to unwind later.
- Branch names MUST be prefixed by their kind: `feature/...` for new
  functionality, `tech/...` for technical/build/tooling changes with no
  behavior change, and `test/...` for test-only changes.

## Governance

This constitution supersedes ad-hoc conventions for anything it explicitly
addresses. Amendments are made via the `/speckit-constitution` workflow:
propose the change, update this document, increment the version per the
policy below, and record the rationale in the Sync Impact Report at the top
of this file.

Versioning policy (semantic versioning applied to governance):
- MAJOR: Backward-incompatible removal or redefinition of a principle.
- MINOR: A new principle or section is added, or existing guidance is
  materially expanded.
- PATCH: Wording, clarification, or typo fixes with no rule change.

All pull requests and code reviews MUST verify compliance with the Core
Principles above. Any deviation MUST be justified in the PR description and,
if it reveals a recurring need, MUST be proposed back as a constitution
amendment rather than repeated as an undocumented exception.

**Version**: 2.1.0 | **Ratified**: 2026-08-08 | **Last Amended**: 2026-08-08
