---
description: Verify implementation against the specification, plan, and tasks without changing code.
argument-hint: "[feature | path/to/spec.md]"
disable-model-invocation: true
---

Read `.claude/skills/specf/SKILL.md` completely and apply its shared rules to this operation.

Use `$ARGUMENTS` to resolve the feature or `spec.md` path.

# Compare reality with intent

1. Read all feature artifacts and inspect implementation and diff without editing.
2. Map every acceptance criterion to concrete implementation and test evidence.
3. Check missing or extra behavior, architectural violations, stale docs, task drift, and unjustified
   plan deviations.
4. Run the focused and repository-wide checks required by the project instructions and plan.
5. Report the status block, then concise findings with evidence and next actions:

```text
SPEC: PASS|WARN|FAIL
TESTS: PASS|WARN|FAIL
STATIC_ANALYSIS: PASS|WARN|FAIL
DOCS: PASS|WARN|FAIL
OVERALL: PASS|WARN|FAIL
```

Do not fix findings unless explicitly requested.

Report `DONE` when `OVERALL` is `PASS`. Otherwise recommend an unambiguous implementation phase or
report the blocker.
