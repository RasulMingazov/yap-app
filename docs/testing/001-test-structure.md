# Test Structure

## Context

Tests document system behavior. One test should describe one scenario and fail for one reason.

## Decision

### What to test

Write a test only when the code owns observable behavior, such as a branch, mapping, state transition, orchestration rule, fallback, or error translation.

- Do not create a test class only to increase coverage.
- Do not test pass-through use cases, constructors, data-class accessors, or trivial delegation.
- Do not test behavior owned by Preferences, an SDK, a framework, or another library.
- Test a wrapper only when it adds project-owned behavior, such as serialization, key selection, validation, fallback, or error mapping.
- Test behavior once at the boundary that owns it; do not repeat the same scenario at every layer.

### Test shape

Describe each test as:

```text
GIVEN precondition WHEN action THEN outcome
```

- Name test functions with Kotlin backticks and use product language.
- Name the test class after the implementation under test.
- Keep setup minimal and scenario-specific values visible.
- Separate arrange, act, and assert visually.
- Allow multiple assertions only when they verify one outcome. Prefer comparing one object or collection over checking each field.
- Write expected values independently from production logic. Do not calculate them with the mapper or algorithm under test.
- Use `assertFailsWith` as the assertion for an expected failure.
- Keep branching, loops, `try`/`catch`, and production algorithms out of tests.
- Use multiple invocations only when testing repetition, concurrency, or idempotency.
- Prefer observable state or results. Verify calls only when the interaction is part of the contract.
