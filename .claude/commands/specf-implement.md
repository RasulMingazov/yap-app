---
description: Implement an approved plan in full or only the selected phase.
argument-hint: "[feature] [--phase N]"
disable-model-invocation: true
---

Read `.claude/skills/specf/SKILL.md` completely and apply its shared rules to this operation.

Use `$ARGUMENTS` to resolve the feature and optional `--phase N`.

# Execute approved tasks

1. Run `.claude/skills/specf/scripts/status.sh <feature>` for incomplete task IDs per phase and
   current `HEAD`/dirty state, then read `spec.md`/`plan.md`/`tasks.md` and reconcile checkboxes
   with reality.
2. Implement all incomplete tasks or the requested phase in dependency order.
3. For behavior changes, add a focused failing test, observe the failure, implement the smallest
   passing change, then refactor while green.
4. Run focused checks during implementation and the repository-required checks at the end.
5. Mark tasks complete only after implementation and verification.
6. Update existing documentation when behavior or architecture changes.
7. Do not branch or commit unless explicitly requested.
8. Stop when progress requires changing scope, violating project rules, making an unplanned
   destructive migration, or choosing between materially different designs.
9. Report completed and remaining tasks, verification, and deviations. Recommend `batch` when at
   least two packets are independently executable, the next incomplete phase otherwise, or `verify`
   when implementation is complete.
