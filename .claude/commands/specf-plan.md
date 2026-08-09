---
description: Create a minimal implementation plan and task list from an approved specification.
argument-hint: "[feature | path/to/spec.md]"
disable-model-invocation: true
---

Read `.claude/skills/specf/SKILL.md` completely and apply its shared rules to this operation.

Use `$ARGUMENTS` to resolve the feature or `spec.md` path.

# Design the change

1. Read `spec.md`, relevant documentation, and affected implementation. Stop if the specification is
   missing, materially ambiguous, or conflicts with project rules.
2. Create `plan.md` and `tasks.md` from `.claude/skills/specf/assets/`.
3. Keep the design minimal and aligned with current architecture. Do not invent paths, symbols,
   abstractions, or unrelated cleanup.
4. Cover contracts, persistence, migrations, security, documentation, and compatibility only when
   relevant.
5. Order concrete, test-first tasks by dependency and vertical value. Keep each behavior test with
   its implementation and include verification commands.
6. Remove unused template sections and placeholders.
7. Report paths, decisions, risks, and readiness, then recommend the first incomplete phase.

Invoking `implement` after reviewing the artifacts is the approval gate.
