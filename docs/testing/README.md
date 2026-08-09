# Testing

Test code also follows [Code conventions](../code-conventions.md).

These documents define focused test rules for `apps/mobile/*` and `services/server/*`. Read each document relevant to the test code being changed.

For a behavior change, add a focused test first and run it to confirm that it fails
for the expected reason. Then implement the smallest passing change. Do not create
an artificial test for documentation, build-only changes, generated code, or
behavior the project does not own.

## All tests

| Document | Read when changing |
| --- | --- |
| [Test structure](001-structure.md) | scenarios, naming, assertions, or test complexity |

## Unit-test support

| Document | Read when changing |
| --- | --- |
| [Test environment](002-environment.md) | repeated setup or system-under-test construction |
| [Test stubs](003-stubs.md) | domain/data builders, `stubcall`, or behavioral collaborators |
| [Presentation stubs](004-presentation-stubs.md) | `Model.DataState`, `Component.UiState`, `Component.News`, nested presentation values, or resource expectations |
| [Test fixtures](005-fixtures.md) | sharing feature-owned stubs between modules |

## Backend integration

| Document | Read when changing |
| --- | --- |
| [Backend integration tests](006-backend-integration.md) | PostgreSQL behavior, Testcontainers, concurrency, constraints, or migrations |

New and changed tests follow these rules. Existing tests migrate when their behavior or setup changes.
