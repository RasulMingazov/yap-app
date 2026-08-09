---
description: Analyze remaining tasks, offer a safe batch, and optionally run it after confirmation.
argument-hint: "[feature] [--active-phase N] [--max-workers N]"
disable-model-invocation: true
---

Read `.claude/skills/specf/SKILL.md` completely and apply its shared rules to this operation.

Use `$ARGUMENTS` to resolve the feature and optional `--active-phase N` and `--max-workers N`.

# Choose and optionally run a safe batch

## Analyze

1. Read all feature artifacts, `.claude/agents/specf-coordinator.md`,
   `.claude/agents/specf-worker.md`, implementation evidence, status, `HEAD`, and upstream state.
2. Parse optional `--active-phase N` and `--max-workers N`. Default to two workers and never exceed
   three. Treat an active phase as unavailable; do not infer completion from partial files.
3. Reconcile task markers in memory, then group remaining work into coherent packets. Prefer whole
   phases. Split only independently verifiable work with disjoint paths; never split a test from the
   behavior it defines.
4. Derive dependencies from consumed outputs, contracts, migrations, behavior, and paths. Phase
   numbers are evidence, not automatic dependencies.
5. Consider a batch executable only when:
   - prerequisites are committed;
   - packets have disjoint paths and no packet consumes another packet's output;
   - every worker path is assigned explicitly and shared paths are coordinator-owned;
   - `specf-worker` is available with worktree isolation;
   - no uncommitted prerequisite or shared change is missing from the worker baseline.
6. Select the smallest executable batch. If parallel work is unsafe, recommend one sequential packet.

Report only:

```text
PARALLEL: READY|NOT READY

PLAN
- <worker -> phase/task packet, or manual/sequential fallback>

COORDINATOR
<shared files and integration work, or NONE>

ACTION
Run | Change | Cancel
```

Add `WHY` only for `NOT READY` or a non-obvious plan. Add `BLOCKED` only when an immediate packet is
blocked, as `- <packet> -> <prerequisite>`. For `NOT READY`, omit `Run`; also omit `Change` when no
custom executable batch is possible. Keep entries short.

## Confirm

Ask `Run`, `Change`, or `Cancel` with the interactive question tool when available. Recheck a changed
batch and show the report again. When nothing is executable, omit run choices. Only explicit
approval authorizes changes.

## Coordinate after approval

1. Keep coordination in the main session and apply `specf-coordinator` rules directly; subagents
   cannot spawn subagents.
2. Recheck `HEAD` and status against the approved baseline. Stop if they changed incompatibly.
3. Invoke approved `specf-worker` instances concurrently with exact task IDs, paths, baseline, and
   verification commands.
4. Review and integrate acceptable commits, apply coordinator-owned changes, run combined checks,
   and mark only verified tasks complete.
5. Report commits, verification, blockers, and final branch state. Never push without a separate
   request. Recommend another parallel batch, the next sequential phase, or verification.
