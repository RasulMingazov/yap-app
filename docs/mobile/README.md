# Mobile

These documents define current implementation rules under `apps/mobile/*`.
All Kotlin in this area also follows [Code conventions](../001-code-conventions.md).

| Document | Read when changing |
| --- | --- |
| [Feature boundaries](001-feature-boundaries.md) | `api`/`impl` split, layers, public surface, or implementation order |
| [Domain](002-domain.md) | entities, repository ports, typed results, or use cases |
| [Repositories](data/001-repositories.md) | repository shape, naming, cache, observation, concurrency, or mutations |
| [Data sources](data/002-data-sources.md) | DTOs, mapping, protocol errors, or authenticated networking |
| [View models](presentation/001-view-models.md) | screen contracts, `UiState`, `News`, events, or splitting a screen |
| [UI and Compose](presentation/002-ui-compose.md) | what belongs in state versus in a composable |
| [Dependency injection](003-dependency-injection.md) | Koin modules, bindings, destinations, or scopes |

Test rules live in [Testing](../testing/README.md).

Navigation 3 mechanics — `NavKey`, `NavDisplay`, back stack, scenes, deep links, conditional
navigation, and the Koin `navigation<Key> { }` DSL — are covered by the official `navigation-3`
skill. These documents only state where this project constrains it: destinations are declared in the
owning feature's Koin module (see [Dependency injection](003-dependency-injection.md)), and a view
model reports navigation intent instead of navigating itself (see
[View models](presentation/001-view-models.md)).
