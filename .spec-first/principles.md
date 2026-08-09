# Yap App Principles

## Core Principles

### I. Module Boundary Isolation

Feature modules MUST NOT depend on other feature modules' implementations. Only
entry-point/composition modules (for example, `apps/mobile/app-root` and
`services/server/app`) MAY depend on multiple feature modules to wire them
together. Cross-cutting capability shared by both client and server MUST live
in `shared/contract/*` as a platform-neutral contract, not be duplicated or
reached into directly across the client/server boundary.

**Rationale**: The repository is structured explicitly around this boundary.
Enforcing it keeps features independently buildable and testable and prevents
hidden coupling as the number of features grows.

### II. Convention-Plugin-Governed Build

All shared Gradle policy (Kotlin, Compose, Ktor, Android SDK levels,
serialization, and static analysis) MUST be expressed once as a plugin in
`convention-plugins/` and applied by role. Individual modules MUST declare only
the convention plugins matching their role plus module-specific dependencies.
They MUST NOT repeat compiler options, SDK versions, or tool configuration that
a convention plugin already owns.

**Rationale**: A single source of truth prevents drift between KMP and JVM
modules and makes new modules a matter of selecting the right plugin instead of
copying build scripts.

### III. Test-First Development (NON-NEGOTIABLE)

Every behavior change MUST start with a focused test that fails for the expected
reason. A bug fix starts with a regression test. After observing the failure,
write the smallest implementation that makes the test pass, then refactor while
keeping the suite green.

Tests MUST verify project-owned observable behavior at the boundary that owns it.
Do not add tests for pass-through use cases, constructors, generated code, or
framework behavior merely to satisfy this principle. Documentation, build-only,
and empty-scaffold changes require verification but no artificial test. If a
behavior change cannot be tested first, record the concrete reason in its plan or
PR before implementation.

**Rationale**: A failing test proves the test can detect the missing or broken
behavior and gives later refactoring a reliable safety boundary.

### IV. Static Analysis Gate (NON-NEGOTIABLE)

Every module that applies a JVM or KMP convention plugin MUST run Detekt against
the shared ruleset in `config/detekt/detekt.yml` with
`buildUponDefaultConfig` enabled. Code MUST NOT be merged with unresolved Detekt
findings. The shared ruleset MUST NOT be weakened or suppressed per module to
pass a build; deliberate rule changes belong in the shared configuration.

**Rationale**: Detekt is already wired into the convention plugins. Treating it
as advisory would allow inconsistent code quality across modules.

### V. Simplicity and YAGNI

Implementation MUST match the current specification. Do not add speculative
abstractions, unused configuration flags, or infrastructure for features that
are not specified. Modules without application behavior MUST stay minimal until
a specification calls for real behavior.

**Rationale**: Keeping unbuilt areas empty rather than filling them with
placeholders keeps the true state of this early-stage project legible.

## Technology Constraints

- The mobile client uses Kotlin Multiplatform with Compose Multiplatform,
  targeting Android and iOS (`iosArm64` and `iosSimulatorArm64`) with JVM 17.
- The server uses Kotlin/JVM with Ktor and is modularized by feature and core
  capability.
- Shared wire contracts live under `shared/contract/*` and MUST remain
  platform-neutral; Android, iOS, and Ktor-specific types MUST NOT leak into
  them.
- Dependency resolution is centralized through dependency management and the
  version catalog. Modules MUST NOT hardcode dependency versions.
- New modules MUST be registered in `settings.gradle.kts` and apply an existing
  convention plugin when one fits. Add a new convention plugin only when the
  existing roles genuinely do not fit.

## Quality Gates

- `./gradlew build` MUST pass before a change is considered done.
- Changes to `shared/contract/*` MUST account for both mobile and server
  consumers in the same change set.
- Review structural compliance with module boundaries and convention plugins
  before functional details.
- Branch names MUST use `feature/...` for functionality, `tech/...` for
  technical or build work, and `test/...` for test-only work.

## Governance

These principles supersede ad-hoc conventions for the areas they address.
Update this file directly when a recurring exception reveals that a principle
needs refinement. Explain intentional deviations in the pull request or update
the owning project documentation; do not repeat undocumented exceptions.
