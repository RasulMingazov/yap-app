# Unit Testing

These documents define unit-test rules for `apps/mobile/*` and `services/server/*`. Read only the document relevant to the test code being changed.

| Document | Read when changing |
| --- | --- |
| [Test structure](001-test-structure.md) | scenarios, naming, assertions, or test complexity |
| [Test environment](002-test-environment.md) | repeated setup or system-under-test construction |
| [Test stubs](003-test-stubs.md) | domain/data builders, `stubcall`, or behavioral collaborators |
| [Presentation stubs](004-presentation-stubs.md) | `DataState`, `UiState`, nested UI values, or resource expectations |
| [Test fixtures](005-test-fixtures.md) | sharing feature-owned stubs between modules |

New and changed tests follow these rules. Existing tests migrate when their behavior or setup changes.
