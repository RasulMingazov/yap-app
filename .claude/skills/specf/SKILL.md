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

- `spec` changes only `spec.md`.
- `plan` changes only `plan.md` and `tasks.md`.
- `implement` changes implementation, tests, relevant docs, and verified task markers; never
  `spec.md` or `plan.md`.
- `verify` is read-only unless the request also asks for fixes.
- `parallel` is read-only until the user approves a batch.

Stop instead of silently rewriting an upstream artifact when a downstream operation finds a
requirements or design conflict.

## Recommend the next step

End each successful operation with exactly one context-aware recommendation using the resolved
feature slug:

```text
NEXT
<copy-ready /specf-* command or DONE>
```

Replace placeholders with the actual feature and phase. Recommend only a step whose prerequisites
are satisfied; otherwise use `BLOCKED: <short reason>`.
For `parallel`, interactive confirmation replaces `NEXT` before approval; emit `NEXT` only after an
approved batch runs.
