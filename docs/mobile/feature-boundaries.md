# Feature Boundaries

Mobile features use vertical KMP modules so product behavior remains independent from UI, protocols, persistence, and platform SDKs.

```text
                  di
             /    |    \
            v     v     v
presentation --> domain <-- data
      |
      v
   Compose
```

- `presentation` depends on `domain`, never on `data`.
- `data` implements contracts owned by `domain`.
- `domain` depends on neither `presentation` nor `data`.
- `di` constructs and connects all feature layers.
- Compose renders presentation state and emits intent.
- Common code stays platform-neutral; platform source sets provide narrow adapters.
- Do not create a layer or abstraction before it owns behavior or a required boundary.

## Public surface

Expose only what another Gradle module needs:

- the root `FeatureComponent` contract;
- presentation values required by that contract;
- public `FeatureContent`;
- a narrow container, component factory, or platform facade.
- Room entities and DAO contracts required by the zone database.

Keep default components, view models, mappers, use cases, repositories, sources, other
representations, and default containers `internal` or `private`. Public Room schema
types are persistence integration details, not a general feature API. Hosts pass
application or platform inputs into feature factories; they do not assemble feature
internals.

## Package shape

Use this as a menu, not an empty template:

```text
feature-auth/src/commonMain/kotlin/app/yap/feature/auth/
├── presentation/
│   ├── auth/         orchestrator: component, default, slot config
│   └── login/        one screen slice, see Components
├── domain/
│   ├── entity/
│   ├── repository/
│   └── usecase/
├── data/
│   ├── identity/
│   ├── local/entity/
│   ├── mapper/
│   ├── platform/entity/
│   ├── remote/entity/
│   └── repository/
└── di/
```

Keep child presentation slices in focused packages such as `presentation/signin`; the file set of a
slice is fixed by [Components](presentation/components.md). Presentation extends `BaseViewModel`
from `core-decompose`.

## Implementation order

1. Define behavior, state, outcomes, and ownership.
2. Create the minimal feature module.
3. Define required domain types and ports.
4. Implement representations, adapters, mapping, and repositories.
5. Add presentation contracts, view models, mapping, and rendering.
6. Wire the container and expose one narrow entry point.
7. Integrate the feature into its navigator.
8. Add focused tests and verify Android and iOS compilation.

Layer rules: [Domain](domain.md), [Repositories](data/repositories.md), [Data Sources](data/data-sources.md), and [Dependency Injection](dependency-injection.md).

Presentation rules: [Components](presentation/components.md), [View Models](presentation/view-models.md), [UI and Compose](presentation/ui-compose.md), [Child Components](presentation/child-components.md), [Navigation](presentation/navigation.md), and [Retention](presentation/retention.md).
