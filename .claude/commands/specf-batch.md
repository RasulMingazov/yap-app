---
description: Analyze remaining tasks, offer a safe batch, and optionally run it after confirmation.
argument-hint: "[feature] [--active-phase N] [--max-workers N]"
disable-model-invocation: true
---

Read `.claude/skills/specf/SKILL.md` completely and apply its shared rules to this operation.

Use `$ARGUMENTS` to resolve the feature and optional `--active-phase N` and `--max-workers N`.

# Choose and optionally run a safe batch

## Analyze

1. Run `.claude/skills/specf/scripts/status.sh <feature>` for task/phase state, `HEAD`, and dirty
   status. Read `.claude/agents/specf-coordinator.md`, `.claude/agents/specf-worker.md`, and
   implementation evidence; read `spec.md`/`plan.md` sections only for the phases under
   consideration, not in full.
2. Parse optional `--active-phase N` and `--max-workers N`. Default to two workers and never exceed
   three. Treat an active phase as unavailable; do not infer completion from partial files.
3. Group remaining work into coherent packets from the status script's phase/task data. Prefer whole
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

## Report

Keep task descriptions, file-level paths, verification commands, dependency details, baseline, and
agent configuration internal. Pass them to workers only after approval.

Assign each worker a unique lowercase kebab-case role name based on its module and concern, such as
`server-auth`, `mobile-auth`, or `shared-contract`. Never expose generic names such as `worker-1` or
`worker-2`; the underlying agent type remains `specf-worker`.

Show only the shortest repository-relative module scope that covers the packet, suffixed with `/**`.
Use at most two module roots per worker. Do not show individual files, `src/main`, `src/test`, or
verification paths.

For `READY`, report exactly:

```text
Ready for parallel

Agents:
1. <role-name>
   - Phase <N> (<task IDs>)
   - <module/**>
```

Add `COORDINATOR` only when the batch requires concrete shared implementation files outside worker
scope:

```text
Coordinator:
- <exact files and change, one line>
```

Do not list routine integration, task markers, final verification, protected path categories, or
potential files. For `READY`, never print explanations, blockers, confirmation choices, file-level
paths, verification commands, task descriptions, or future-phase dependencies. Show only the compact
module scopes required by the plan template.

For `NOT READY`, report:

```text
Not ready for parallel

Fallback:
1. <role-name>
   - Phase <N> (<task IDs>)
   - <module/**>

Why:
<one complete sentence, at most 20 words>

Blocked:
- <phase/task IDs -> prerequisite phase/task IDs>
```

Omit `Fallback` when there is no executable fallback. Omit `Blocked` when the sentence in `Why` fully
explains why parallelism is unavailable. Never list blocked future phases when a ready batch exists.

Before showing the report, verify that every line is complete, correctly spaced, and matches one of
these templates. Each agent block must contain only role name, phase, task IDs, and compact module
scope. Number agents from 1 without gaps.

## Confirm

After the report, use the interactive question tool when available. Do not duplicate its choices in
the report or surrounding text. Offer only actions that are currently executable: `Run`, `Change`,
and `Cancel` for a ready batch; omit `Run` when no batch is ready. If the tool is unavailable, ask one
plain-text question instead. Recheck a changed batch and report it again. Only explicit approval
authorizes changes.

## Coordinate after approval

1. Keep coordination in the main session and apply `specf-coordinator` rules directly; subagents
   cannot spawn subagents.
2. Recheck `HEAD` and status against the approved baseline. Stop if they changed incompatibly.
3. Invoke approved `specf-worker` instances concurrently using the displayed role names and exact
   task IDs, full allowed paths, baseline, and verification commands.
4. Review and integrate acceptable commits, apply coordinator-owned changes, run combined checks,
   and mark only verified tasks complete.
5. Report commits, verification, blockers, and final branch state. Never push without a separate
   request. Recommend another batch, the next sequential phase, or verification.
