# Architecture Decisions

Each document owns one architectural decision. Use the "Read when changing" column to select the relevant document and read it in full.

## Mobile

| Decision | Read when changing |
| --- | --- |
| [Feature module boundaries](mobile-feature-boundaries.md) | module ownership, layers, public feature API, package shape |
| [Repository contracts and cache](repository-contracts-and-cache.md) | repository granularity, naming, fetch, observation, mutation, concurrency |
| [Components and models](presentation-components-and-models.md) | component contracts, default components, model state and events |
| [Manual DI containers](manual-dependency-injection.md) | dependency graph, factories, scopes, platform containers |
| [Data-source boundaries](mobile-data-sources.md) | DTOs, DB models, mapping, protocol errors, authenticated networking |
| [Persistence composition](mobile-persistence.md) | Room, database modules, DAO and schema ownership |
| [UI state and Compose](ui-state-and-compose.md) | mappers, immutable UI state, news/effects, composables |
| [Navigation and retention](navigation-and-retention.md) | Decompose navigation, child slices, `InstanceKeeper`, restoration |
| [Domain boundaries](mobile-domain-boundaries.md) | entities, repository ports, typed results, use cases |

## Backend

| Decision | Read when changing |
| --- | --- |
| [Feature-first boundaries](backend-feature-boundaries.md) | server modules, app composition, routes, services, adapters |
| [Persistence verification](backend-persistence-and-testing.md) | feature repositories, migrations, transactions, Testcontainers |

## Adding a decision

- Record one decision with its context, consequences, and compliance rule.
- Use a short descriptive filename.
- Prefer links over copying rules between documents.
- Update this index and `CLAUDE.md` routing when a new trigger category is introduced.
- Supersede an old ADR explicitly instead of silently contradicting it.
