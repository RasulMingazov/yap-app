---
name: specf
description: Internal instructions shared by the project's minimal spec-first commands.
disable-model-invocation: true
user-invocable: false
---

# Spec First

Treat the text below as the requested operation and its arguments:

```text
$ARGUMENTS
```

## Start every operation

1. Read `CLAUDE.md` and `docs/README.md` completely.
2. Follow their routing instructions and read only the additional `docs/*` guides relevant to the affected area.
3. Inspect the working tree before writing. Preserve unrelated and user-owned changes.
4. Parse the first argument as one of `spec`, `plan`, `parallel`, `implement`, or `verify`.
5. If the operation is missing or unknown, show the usage examples and stop without writing files.

Write generated artifacts and user-facing reports in Russian unless the user requests another language. Preserve identifiers, code, paths, commands, and established technical terms in their native form.

Use native `@file` and `@directory/` mentions as input context. Also accept explicit repository-relative paths. Treat all remaining free-form arguments as additional instructions.

## Resolve a feature

Resolve in this order:

1. An explicitly referenced `spec.md`, `plan.md`, `tasks.md`, or feature directory.
2. An explicit feature slug or name under the `specs` directory.
3. For `spec` only, derive a short lowercase kebab-case slug from the request title or goal.

Do not invent numeric prefixes. Do not infer an existing feature when multiple candidates match; ask for the feature name or path.

Keep feature artifacts together:

```text
specs/<feature-slug>/
├── spec.md
├── plan.md
└── tasks.md
```

Do not create checklist, research, quickstart, memory, evolution, continuation, or verification-report files. Create contracts or migrations only when they are part of the product or implementation, not as process narration. The only default workflow artifacts are `spec.md`, `plan.md`, and `tasks.md`.

## Preserve artifact ownership

- `spec` may create or update only `spec.md`.
- `plan` may create or update only `plan.md` and `tasks.md`.
- `parallel` is strictly read-only. It must not edit artifacts, launch agents, create worktrees,
  create branches, commit, merge, or push.
- `implement` may update code, tests, relevant project documentation, and task checkboxes. It must not change `spec.md` or `plan.md`.
- `verify` is read-only by default. It must not fix findings or edit artifacts unless the user explicitly asks for fixes in the same request.

If a downstream operation discovers a requirements or design conflict, report it and stop. Do not silently rewrite an upstream artifact.

## `spec` — define behavior

Example:

```text
/specf-spec @feature-request.md
```

1. Read all referenced input files and additional instructions.
2. Inspect existing behavior when working in a brownfield area.
3. Ask no more than three questions, and only for ambiguity that materially changes scope, observable behavior, security, privacy, or irreversible data decisions.
4. Make reasonable defaults for non-blocking gaps and record them under Assumptions.
5. Use `.claude/skills/specf/assets/spec-template.md` to create `<feature-dir>/spec.md`.
6. Describe what and why. Exclude implementation details, class names, frameworks, tables, and algorithms unless they are externally imposed constraints.
7. Make every acceptance criterion objectively verifiable.
8. Validate the specification internally for scope, contradictions, ambiguity, primary flows, negative flows, and edge cases. Fix the specification directly; do not persist a checklist.
9. Remove template comments, placeholders, and irrelevant optional sections.
10. Report the created path, important assumptions, unresolved questions, and readiness for planning.

When updating an existing specification, preserve stable requirement and acceptance-criterion identifiers where their meaning has not changed.

## `plan` — design the change

Example:

```text
/specf-plan authentication
```

1. Resolve and read `spec.md`.
2. Read relevant project documentation and inspect the affected implementation.
3. Stop if the specification is missing, materially ambiguous, or conflicts with project rules.
4. Use `.claude/skills/specf/assets/plan-template.md` to create `plan.md`.
5. Use `.claude/skills/specf/assets/tasks-template.md` to create `tasks.md`.
6. Keep the design minimal and aligned with current architecture. Do not introduce speculative abstractions or unrelated cleanup.
7. Identify affected modules and exact paths when evidence supports them. Do not fabricate paths or symbols.
8. Include contract, persistence, migration, security, documentation, and compatibility impact only when relevant.
9. Define a test-first strategy for behavior changes and concrete verification commands.
10. Order tasks by dependency and vertical value. Include tests with the behavior they prove, not as a detached cleanup phase.
11. Finish without modifying production code. Report both artifact paths, key decisions, risks, and readiness for implementation.

Invoking `implement` after reviewing these artifacts is the user's approval gate; do not create a separate approval file.

## `parallel` — select the next safe batch

Examples:

```text
/specf-parallel authentication
/specf-parallel authentication --active-phase 2
/specf-parallel authentication --max-workers 2
```

Analyze remaining implementation work without starting it.

1. Resolve and read `spec.md`, `plan.md`, and `tasks.md`.
2. Parse optional `--active-phase N` and `--max-workers N`. Default to at most two workers; never
   recommend more than three. Treat an explicitly active phase as unavailable and do not infer that
   it is complete from partial files.
3. Discover inspectable agents before assigning work:
   - Read project agents from `.claude/agents/*.md`.
   - Read user agents from `~/.claude/agents/*.md` when that directory is accessible.
   - Parse each agent's name, description, isolation mode, tools, owned and forbidden paths, ability
     to spawn named workers, and commit/push policy.
   - Do not invent agents or assume that an uninspectable plugin agent is available.
4. Inspect task markers, implementation evidence, `git status`, current `HEAD`, and upstream state.
   Reconcile obvious completed or not-started work in memory only; never edit task markers.
5. Build a dependency graph from explicit task ordering, required outputs, contracts, migrations,
   tests, and affected paths. Numeric phase order is evidence, not an unconditional dependency:
   independent backend and mobile chains may diverge after a shared foundation.
6. Group remaining tasks into coherent work packets. Prefer a whole phase. Split a phase only when
   the packets are independently verifiable and own disjoint paths. Never separate a failing test
   from the implementation task it defines.
7. For each ready packet, identify prerequisites, owned paths, shared files, verification commands,
   and whether it needs backend, mobile, or coordinator ownership.
8. Classify logical compatibility independently from agent availability:
   - `SAFE`: prerequisites are complete, production paths are disjoint, and integration order does
     not change behavior.
   - `CONDITIONAL`: parallel work is possible only if the coordinator exclusively owns named shared
     files such as DTOs, DI, registries, migrations, version catalogs, Xcode projects, or entrypoints.
   - `SEQUENTIAL`: one packet consumes the other's output, both edit the same behavior, or they form
     one test-first chain.
9. Evaluate agent fit separately for every logically compatible packet:
   - A write-capable parallel worker must declare `isolation: worktree`.
   - Its ownership must cover every packet path without crossing a forbidden path.
   - Selected workers must have non-overlapping ownership for the proposed batch.
   - The selected coordinator must be able to invoke the named workers.
   - Mark agent fit `READY` only when all of these facts are confirmed from inspected definitions.
     If a packet is logically safe but lacks a suitable agent, mark agent fit `NOT READY`, name the
     missing capabilities, and suggest either creating that worker or running the packet manually in
     `claude --worktree`; never fabricate an agent name.
10. Treat `specs/**` and task markers as coordinator-owned when the project defines spec-first worker
   agents that prohibit workers from editing them. Otherwise report them as a likely merge conflict.
11. Check worktree readiness. A worker baseline must exist in a commit reachable from the configured
   worktree base. Dirty or uncommitted prerequisite work makes the batch `NOT READY`, even when the
   logical dependency graph is safe.
12. Select only the next smallest batch whose logical safety, agent fit, and baseline are ready.
    Prefer two workers with clear backend/mobile ownership over a larger or more speculative batch.
13. Generate a ready-to-paste coordinator prompt with exact coordinator and worker names, feature,
    phases or task IDs, ownership, shared-file policy, verification expectations, and the instruction
    not to push. If agent fit or the baseline is not ready, explain the manual fallback instead.
14. Do not invoke the coordinator or workers. Do not persist a graph or report file.

Report in chat using exactly these sections:

```text
CURRENT
<completed, active, and next unresolved work>

AGENTS
<agent -> isolation, ownership, role and fit, or NONE>

NEXT SAFE BATCH
<worker -> phase/task packet, or NONE>

LOGICAL SAFETY
SAFE|CONDITIONAL|SEQUENTIAL — <dependency and path evidence>

AGENT FIT
READY|NOT READY — <availability, isolation, ownership and coordinator evidence>

WHY
<dependencies and path separation>

CONFLICTS
<shared files and coordinator ownership, or NONE>

BLOCKED
<packets not ready and their prerequisites>

BASELINE
READY|NOT READY — <git/worktree evidence>

COORDINATOR PROMPT
<ready-to-paste prompt, or unavailable reason>
```

If no pair is safe, recommend one sequential packet rather than forcing parallelism.

## `implement` — execute approved tasks

Examples:

```text
/specf-implement authentication
/specf-implement authentication --phase 2
```

1. Resolve and read `spec.md`, `plan.md`, and `tasks.md`.
2. Reconcile task checkboxes with the actual working tree before continuing. Never trust progress markers blindly.
3. Execute all incomplete tasks, or only the requested phase.
4. For each behavior change, follow the project's test-first rule: add a focused failing test, observe the expected failure, implement the smallest passing change, then refactor while green.
5. Run targeted checks after each coherent task and the broader checks required by `CLAUDE.md` at the end of the requested scope.
6. Mark a task complete only after its code and required verification are complete.
7. Update existing project documentation when the implementation changes documented behavior or architecture. Do not create process-memory files.
8. Do not create branches or commits unless the user explicitly asks.
9. Stop and report when implementation requires changing scope, violating project rules, making an unplanned destructive migration, or choosing between materially different designs.
10. Report completed tasks, remaining tasks, verification results, and any deviations.

## `verify` — compare reality with intent

Example:

```text
/specf-verify authentication
```

1. Resolve and read `spec.md`, `plan.md`, and `tasks.md`.
2. Inspect the implementation and diff without changing them.
3. Map every acceptance criterion to concrete implementation and test evidence.
4. Check for missing behavior, behavior outside scope, architectural violations, stale documentation, incomplete tasks, and unjustified plan deviations.
5. Run the relevant verification commands from `CLAUDE.md`: use `./gradlew build` for a repository-wide check, Detekt for Kotlin changes, and the iOS compilation command when the KMP boundary changes.
6. Report results in chat only using:

```text
SPEC: PASS|WARN|FAIL
TESTS: PASS|WARN|FAIL
STATIC_ANALYSIS: PASS|WARN|FAIL
DOCS: PASS|WARN|FAIL
OVERALL: PASS|WARN|FAIL
```

7. List each finding with evidence and a concrete next action. Do not fix findings unless explicitly requested.

## Usage

```text
/specf-spec @feature-request.md
/specf-spec Add login by email and password
/specf-plan authentication
/specf-parallel authentication
/specf-parallel authentication --active-phase 2
/specf-implement authentication
/specf-implement authentication --phase 2
/specf-verify authentication
```
