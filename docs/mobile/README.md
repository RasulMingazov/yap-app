# Mobile

These documents define current implementation rules under `apps/mobile/*`.
All Kotlin in this area also follows [Code conventions](../code-conventions.md).

| Document | Read when changing |
| --- | --- |
| [Feature boundaries](feature-boundaries.md) | modules, layers, public surface, packages, or implementation order |
| [Domain](domain.md) | entities, repository ports, typed results, or use cases |
| [Repositories](data/repositories.md) | repository shape, naming, cache, observation, concurrency, or mutations |
| [Data sources](data/data-sources.md) | DTOs, DB models, mapping, protocol errors, or authenticated networking |
| [Dependency injection](dependency-injection.md) | containers, model factories, graph construction, scopes, or platforms |
| [Persistence](data/persistence.md) | Room, database ownership, DAOs, or authenticated database lifetime |
| [Components](presentation/components.md) | `Value`, component contracts, default implementations, events, or factories |
| [Models](presentation/models.md) | model placement, factory, dependencies, state, cleanup, or tests |
| [UI and Compose](presentation/ui-compose.md) | mapping, recomposition, resources, or `core-design` composables |
| [Child components](presentation/child-components.md) | complex screens, sheets, dialogs, or host components |
| [Navigation](presentation/navigation.md) | Decompose, navigation ownership, levels, or cold destinations |
| [Retention](presentation/retention.md) | `InstanceKeeper`, `StateKeeper`, restoration, keys, or lifecycle tests |

Test rules live in [Testing](../testing/README.md).
