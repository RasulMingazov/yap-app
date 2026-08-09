---
description: Create or update a feature specification from text, a file, or a directory.
argument-hint: "[feature description | @file | @directory/]"
disable-model-invocation: true
---

Read `.claude/skills/specf/SKILL.md` completely and apply its shared rules to this operation.

Use `$ARGUMENTS` as the feature description, input paths, and additional instructions.

# Define behavior

1. Read all supplied input and inspect existing behavior for brownfield work.
2. Derive a short lowercase kebab-case slug when no existing feature is specified.
3. Ask at most three questions, only when ambiguity materially changes scope, observable behavior,
   security, privacy, or irreversible data decisions. Record reasonable defaults under Assumptions.
4. Create `spec.md` from `.claude/skills/specf/assets/spec-template.md`.
5. Describe what and why, not implementation, unless an external constraint requires it.
6. Cover primary, negative, and edge flows with objectively verifiable acceptance criteria.
7. Remove placeholders and irrelevant sections. Preserve stable requirement and acceptance IDs when
   updating an existing specification.
8. Report the path, assumptions, unresolved questions, and planning readiness.
9. Recommend `/specf-plan <feature>` when ready; otherwise report the blocker.
