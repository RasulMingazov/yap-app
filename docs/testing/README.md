# Testing

Test code also follows [Code conventions](../001-code-conventions.md). Framework choice, harnesses, and screenshot/UI test setup follow the official `testing-setup` skill; these documents cover this project's conventions.

For a behavior change, add a focused test first and run it to confirm it fails for the expected reason. Then implement the smallest passing change. Do not create an artificial test for documentation, build-only changes, generated code, or behavior the project does not own.

| Document | Read when changing |
| --- | --- |
| [Test structure](001-structure.md) | scenarios, naming, assertions, or repeated setup |
| [Test stubs](002-stubs.md) | domain/data builders, `stubcall`, or behavioral collaborators |
| [Backend integration](003-backend-integration.md) | PostgreSQL behavior, Testcontainers, concurrency, constraints, or migrations |

New and changed tests follow these rules. Existing tests migrate when their behavior or setup changes.
