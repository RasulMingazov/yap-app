---
name: specf
description: Internal instructions shared by the project's minimal spec-first commands.
disable-model-invocation: true
user-invocable: false
---

# Spec First

## Start

1. Read `CLAUDE.md` and `docs/README.md` when present, then follow their routing instructions to the
   relevant guides.
2. Inspect the working tree before writing and preserve unrelated changes.

Write artifacts and reports in English unless the user requests another language. Preserve code,
identifiers, paths, commands, and quoted product copy. Accept native `@file` and `@directory/`
context, repository-relative paths, and trailing free-form instructions.

## Artifacts

Resolve an explicit artifact or feature-directory path first, otherwise an explicit slug or name
under `specs/`. Ask when multiple existing features match. Do not invent numeric prefixes.

Keep the only workflow artifacts together:

```text
specs/<feature>/
├── spec.md
├── plan.md
└── tasks.md
```

Do not create process artifacts such as checklists, research notes, quickstarts, memory, continuation,
or verification reports. Create contracts and migrations only when the product needs them.

## Ownership

- `specify` changes only `spec.md`.
- `plan` changes only `plan.md` and `tasks.md`.
- `implement` changes implementation, tests, relevant docs, and verified task markers; never
  `spec.md` or `plan.md`.
- `verify` is read-only unless the request also asks for fixes.
- `batch` is read-only until the user approves a batch.

Stop instead of silently rewriting an upstream artifact when a downstream operation finds a
requirements or design conflict.

## Scoped reads

`spec.md` and `plan.md` grow large on real features. Reading either in full is required only for
`specify`, `plan`, and `verify` — those operations need the whole picture and correctness there
matters more than cost.

Everywhere else (`implement` on a single phase, delegated `specf-worker` packets, `batch` readiness
checks), read only what the operation needs:

- Prefer a section by heading boundary over a fixed line window:
  `sed -n '/^### R-042/,/^### /p' plan.md` extracts exactly one requirement, never truncated or
  padded with unrelated text.
- Use `.claude/skills/specf/scripts/status.sh <feature>` for task/phase/HEAD bookkeeping (which docs
  exist, which task IDs are incomplete per phase, branch and dirty state) instead of reading
  `tasks.md` in full to reconcile markers.
- When delegating, inline the relevant task text and `R-`/`AC-` ID text directly in the
  instruction instead of telling the reader to open the full artifact (see
  `specf-coordinator.md`/`specf-worker.md`).

## Recommend the next step

End each successful operation with exactly one context-aware recommendation using the resolved
feature slug. Prefer the interactive question tool when available, offering the recommended
`/specf-*` command as an `Apply` choice alongside `Change` and, when a step is optional, `Skip` —
the same pattern `batch` already uses for its own confirmation. Fall back to plain text only when
the question tool is unavailable:

```text
NEXT
<copy-ready /specf-* command or DONE>
```

Replace placeholders with the actual feature and phase. Recommend only a step whose prerequisites
are satisfied; otherwise use `BLOCKED: <short reason>`.
For `batch`, this applies only to a `READY` batch: interactive confirmation (`Run`/`Change`/`Cancel`)
replaces `NEXT` before approval, and `NEXT` (or the equivalent interactive choice) is emitted only
after an approved batch runs. A `NOT READY` report has no batch to approve, so it ends with `NEXT`
immediately, pointing at the concrete action that unblocks progress.
