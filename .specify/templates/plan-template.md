# Implementation Plan: [FEATURE]

**Branch**: `feature/[###-feature-name]` | **Date**: [DATE] | **Spec**: [link]

**Input**: Feature specification from `/specs/[###-feature-name]/spec.md`

## Summary

[Primary requirement and the smallest technical approach that satisfies it]

## Applicable Guides

Always read `.specify/memory/constitution.md`, then list only the guides relevant
to this feature:

- [e.g. `docs/mobile/presentation/components.md`]
- [e.g. `docs/mobile/data/repositories.md`]
- [e.g. `docs/testing/README.md`]

Generated feature artifacts refine behavior but do not override project guides.
Resolve a conflict before implementation.

## Technical Context

**Area**: [mobile / server / shared contract / cross-cutting]

**Language/Platform**: [Kotlin Multiplatform, Kotlin/JVM, Android, iOS, server]

**Affected Modules**: [exact existing Gradle paths; identify any justified new module]

**Primary Dependencies**: [existing dependencies or a justified addition]

**Storage**: [Room zone, PostgreSQL, secure storage, or N/A]

**Testing**: [first observable behavior to test and its exact test target]

**Constraints**: [lifecycle, compatibility, performance, security, or N/A]

## Constitution Check

*GATE: Pass before research and re-check after design.*

- **Boundaries**: [module and layer dependencies remain valid]
- **Build policy**: [convention plugin and version-catalog impact]
- **Test-First**: [first failing test, or why this change owns no behavior]
- **Static analysis**: [Detekt impact]
- **YAGNI**: [no unused module, layer, abstraction, or infrastructure]
- **Branch**: [uses `feature/...`]

## Project Structure

### Feature artifacts

```text
specs/[###-feature-name]/
├── spec.md
├── plan.md
├── research.md       # only when research is required
├── data-model.md     # only when the feature owns data
├── contracts/        # only when it owns an external contract
├── quickstart.md     # only when manual verification needs instructions
└── tasks.md
```

### Source code

Use real paths from this repository. Keep only affected entries and do not create
empty layers:

```text
shared/contract/<capability>/
apps/mobile/feature-<capability>/
apps/mobile/app-root/
apps/mobile/core-<capability>/
services/server/feature-<capability>/
services/server/app/
services/server/core-<capability>/
```

**Structure Decision**: [exact files/modules changed and why ownership belongs there]

## Test-First Sequence

1. Add one focused behavior or regression test.
2. Run it and record that it fails for the expected reason.
3. Implement the smallest passing change.
4. Refactor while keeping the targeted suite green.
5. Run module checks and the repository verification required by `CLAUDE.md`.

## Complexity Tracking

Fill only for a constitution or project-guide exception.

| Exception | Why needed now | Simpler alternative rejected because |
| --- | --- | --- |
| [rule or boundary] | [concrete requirement] | [specific reason] |
