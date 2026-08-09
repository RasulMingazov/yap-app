---
name: specf
description: Run the repository's minimal spec-first workflow. Use when the user invokes /specf to turn free-form text or referenced files into a feature specification, create an implementation plan and task list, implement an approved plan, or verify code against the specification. Supports spec, plan, implement, and verify operations.
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
4. Parse the first argument as one of `spec`, `plan`, `implement`, or `verify`.
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
- `implement` may update code, tests, relevant project documentation, and task checkboxes. It must not change `spec.md` or `plan.md`.
- `verify` is read-only by default. It must not fix findings or edit artifacts unless the user explicitly asks for fixes in the same request.

If a downstream operation discovers a requirements or design conflict, report it and stop. Do not silently rewrite an upstream artifact.

## `spec` — define behavior

Example:

```text
/specf spec @feature-request.md
```

1. Read all referenced input files and additional instructions.
2. Inspect existing behavior when working in a brownfield area.
3. Ask no more than three questions, and only for ambiguity that materially changes scope, observable behavior, security, privacy, or irreversible data decisions.
4. Make reasonable defaults for non-blocking gaps and record them under Assumptions.
5. Use `${CLAUDE_SKILL_DIR}/assets/spec-template.md` to create `<feature-dir>/spec.md`.
6. Describe what and why. Exclude implementation details, class names, frameworks, tables, and algorithms unless they are externally imposed constraints.
7. Make every acceptance criterion objectively verifiable.
8. Validate the specification internally for scope, contradictions, ambiguity, primary flows, negative flows, and edge cases. Fix the specification directly; do not persist a checklist.
9. Remove template comments, placeholders, and irrelevant optional sections.
10. Report the created path, important assumptions, unresolved questions, and readiness for planning.

When updating an existing specification, preserve stable requirement and acceptance-criterion identifiers where their meaning has not changed.

## `plan` — design the change

Example:

```text
/specf plan authentication
```

1. Resolve and read `spec.md`.
2. Read relevant project documentation and inspect the affected implementation.
3. Stop if the specification is missing, materially ambiguous, or conflicts with project rules.
4. Use `${CLAUDE_SKILL_DIR}/assets/plan-template.md` to create `plan.md`.
5. Use `${CLAUDE_SKILL_DIR}/assets/tasks-template.md` to create `tasks.md`.
6. Keep the design minimal and aligned with current architecture. Do not introduce speculative abstractions or unrelated cleanup.
7. Identify affected modules and exact paths when evidence supports them. Do not fabricate paths or symbols.
8. Include contract, persistence, migration, security, documentation, and compatibility impact only when relevant.
9. Define a test-first strategy for behavior changes and concrete verification commands.
10. Order tasks by dependency and vertical value. Include tests with the behavior they prove, not as a detached cleanup phase.
11. Finish without modifying production code. Report both artifact paths, key decisions, risks, and readiness for implementation.

Invoking `implement` after reviewing these artifacts is the user's approval gate; do not create a separate approval file.

## `implement` — execute approved tasks

Examples:

```text
/specf implement authentication
/specf implement authentication --phase 2
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
/specf verify authentication
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
/specf spec @feature-request.md
/specf spec Add login by email and password
/specf plan authentication
/specf implement authentication
/specf implement authentication --phase 2
/specf verify authentication
```
