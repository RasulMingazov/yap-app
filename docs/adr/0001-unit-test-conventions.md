# ADR-0001: Unit Test Conventions

- Status: Accepted
- Date: 2026-08-08
- Scope: Unit tests across `yap-app` — `apps/mobile/*` (Kotlin Multiplatform,
  targeting Android and Kotlin/Native `iosArm64`/`iosSimulatorArm64`) and
  `services/server/*` (Kotlin/JVM)

## Agent essentials

For ordinary test work, read this section and only the detailed sections relevant to the test boundary being changed. Read the full ADR when changing shared test conventions, fixtures, or test infrastructure.

- One test covers one `GIVEN ... WHEN ... THEN ...` behavior with one primary action and one reason to fail; name the reusable fixture variable `env`.
- Keep scenario-specific values visible, expected values independent and literal, and test bodies free of branching or production algorithms.
- Use `stubcall` for configurable responses, failures, call counts, and exact arguments. Keep every thin stub adapter in its own file.
- Build repeated value objects through nearby `internal object StubX` factories with named, overridable fields; add named scenario helpers only for recurring scenarios.
- Prefer observable state or result assertions. Verify interactions only when the call itself is part of the contract.
- Do not add tests for construction, data-class accessors, or thin delegation already covered at the owning boundary.

## Context

Unit tests are executable behavior documentation. When one test checks several outcomes, has a long
name, or contains substantial orchestration, a failure does not clearly identify the broken behavior.
Repeated hand-written fake boilerplate also obscures the scenario and makes tests harder to maintain.

`yap-app` is a single Gradle monorepo spanning a Kotlin Multiplatform mobile client
(`apps/mobile/*`, applying `KmpLibraryPlugin`) and a modular Kotlin/JVM server
(`services/server/*`, applying `JvmLibraryPlugin`). Both sides share the same test
framework — `kotlin.test` (the `kotlin-test` catalog entry) — and both are wired to
resolve the `stubcall` library (`io.github.rasulmingazov:stubcall`, catalog entry
`stubcall`) from the local `libs/` Maven repository declared in
`settings.gradle.kts`.

This ADR defines a small, consistent test shape that stays readable across both sides
of the monorepo and survives compiling to a Kotlin/Native target.

## Decision

Each unit test describes one behavior through arrange, act, and assert:

```text
GIVEN precondition WHEN action THEN outcome
```

Keep these parts visually separated in the test body. A test has one reason to fail: one action under
one relevant condition produces one observable outcome.

## Naming

- Name test functions with Kotlin backticks using `GIVEN ... WHEN ... THEN ...`.
- Do not use punctuation in test function names because Kotlin/Native rejects some punctuation that
  JVM tests accept. This applies to every module under `apps/mobile/*`, since `KmpLibraryPlugin`
  compiles test sources for the `iosArm64`/`iosSimulatorArm64` Kotlin/Native targets in addition to
  Android. `services/server/*` modules only ever run on the JVM (`JvmLibraryPlugin`, JUnit Platform),
  so the constraint has no compiler consequence there, but tests still follow it for one naming
  convention across the whole repo.
- Keep the name concise. State only the precondition that changes behavior, the action, and the
  expected outcome.
- Prefer domain language over implementation details, method names, setup mechanics, and repeated
  type names.
- Name a test class after the concrete implementation under test: use `JwtTokenServiceTest`
  for `JwtTokenService` (`services/server/core-security`).
- Name the local variable that holds a test's environment/fixture object `env`.

For example:

```kotlin
fun `GIVEN malformed refresh token WHEN parsing THEN throws invalid token exception`()
```

## Verification per test

- Group verification statements by GIVEN scenario: a test may contain several verification
  statements when they all describe the outcome of the same GIVEN precondition and WHEN action.
- A verification statement is an `assert*` call or a `stubcall` verification such as `called()`,
  `calledWith(...)`, or `notCalled()`.
- Do not combine verifications from different GIVEN preconditions or different WHEN actions in the
  same test. Split those into separate tests with focused names.
- When several values form one outcome, compare one value object or collection instead of asserting
  each field separately — for example, assert one `IssuedTokens` instance rather than its
  `accessToken`, `refreshToken`, and `accessTokenExpiresAtEpochSeconds` fields individually.
- `assertFailsWith` is the single verification for an expected failure and contains the action it
  verifies — for example `assertFailsWith<InvalidTokenException> { service.parseRefreshToken(value) }`.
- Write an assertion's expected value as independent literals. Do not derive it by passing a stub's
  input through the same mapper or function the code under test also calls; if that mapper has a bug,
  a derived expectation goes through the same buggy path and cannot catch it. Literal duplication
  between what a fake dependency returns and what the test expects as the outcome is intentional, not
  something to remove.

## Test complexity

- Keep setup minimal and include only values relevant to the named `GIVEN` condition.
- Keep one primary action in `WHEN`. Multiple invocations are allowed only when repetition,
  concurrency, or idempotency is the behavior under test.
- Do not put branching, loops, `try`/`catch`, production algorithms, or unrelated object construction
  in a test body.
- Move reusable setup into a small fixture or focused factory, but keep behavior-specific values
  visible in the test.
- Split a test when its name needs more than one `AND`, its arrangement needs several independent
  conditions, or its failure would require inspecting multiple outcomes to find the cause.

## Test doubles

- Use `stubcall` for configurable return values, errors, call counts, and exact-argument
  verification.
- Put every stub implementation in its own file. Name the file after the stub, for example
  `StubTokenService.kt`, and do not nest stub implementations inside a test class.
- Place a stub in the nearest test source set and package where all of its intended consumers can
  reuse it:
  - On the mobile side, `apps/mobile/core-test` already exists as the shared test-utility module —
    reuse it for stubs consumed across more than one mobile module, but do not move feature-specific
    stubs there solely for speculative reuse.
  - `services/server/*` has no equivalent shared test-utility module yet. Per the constitution's
    Simplicity & YAGNI principle, do not create one speculatively; keep a stub local to the nearest
    consuming module's test source set until real reuse across two or more server modules exists,
    then introduce a dedicated module registered in `settings.gradle.kts` and governed by a
    convention plugin, matching the constitution's Module Boundary Isolation and
    Convention-Plugin-Governed Build principles.
- Keep hand-written stubs as thin interface adapters whose methods delegate directly to a
  `StubCallN` property.
- Do not duplicate return-value fields, error fields, counters, recorded arguments, or assertion
  helpers in hand-written fakes.
- Give fixture/stub builder functions for value objects (`internal object Stub* { fun stub*(...) }`)
  a base function that requires every field explicitly, for example `stubIssuedTokens(...)`. Add
  named scenario convenience functions on top of it, for example `stubExpiredIssuedTokens(...)`,
  whose defaults bake in that scenario; keep every field a named, overridable parameter so a test can
  adjust exactly the field its `GIVEN` depends on. Reserve unnamed, non-scenario defaults for true
  don't-care placeholders, such as the empty-string/`0` defaults already used for unexercised fields
  in `StubCallN`-based fakes.
- Prefer state assertions for observable behavior. Verify a dependency interaction only when that
  interaction is itself the contract under test.

## Consequences

Benefits:

- A failing test points to one behavior and one expected outcome.
- Short names remain scannable in IDE and CI output.
- Simple test bodies make production behavior easier to review.
- Shared stubs remove repetitive test-double bookkeeping.
- One naming convention holds across the Kotlin/Native mobile targets and the JVM-only server, so
  contributors do not need to track a per-module exception.

Costs:

- One GIVEN scenario maps to one test, which may contain several verification statements.
- Fixtures need deliberate boundaries so they reduce setup without hiding the relevant condition.
- The server side currently has no shared `core-test`-equivalent module, so some fixture duplication
  across `services/server/*` modules is expected until reuse justifies introducing one.

These costs are accepted in exchange for precise failures and maintainable tests.

## Compliance and migration

New and substantially changed unit tests follow this ADR immediately. `yap-app` only has the
authentication slice scaffolded so far, so there is no legacy test suite to retroactively migrate;
existing tests are migrated when their covered behavior changes, and unrelated tests are not
rewritten solely for formatting. Test-only changes follow the `test/...` branch prefix from the
constitution's Development Workflow section.

Any intentional exception must be justified in the pull request description, per the constitution's
Governance section, or superseded by a later ADR.
