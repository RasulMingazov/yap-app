# Test Fixtures

## Context

A feature may own stubs that another module needs in tests. They must be reusable without becoming production dependencies or losing feature ownership.

## Decision

Keep a stub in the owning feature's test source set while only that feature uses it. Because this project has no KMP-native shared `testFixtures` source set, cross-module feature stubs live in a separate KMP subproject:

```text
apps/mobile/feature-auth/test-fixtures/
└── src/commonMain/kotlin/app/yap/feature/auth/test/
    ├── StubAuthComponent.kt
    └── StubAuthContainer.kt
```

- Create `:apps:mobile:feature-auth:test-fixtures` only when a real cross-module consumer exists.
- Keep local stubs `internal`. Make only exported fixture declarations public.
- Let the fixtures project depend on its owning feature; add `core-test` only for generic test infrastructure.
- Consumers depend on fixtures only from test source sets or configurations.
- Keep feature-owned stubs out of `core-test`; reserve it for generic utilities.
- Production source sets must not depend on fixtures or test utilities.
- Keep server stubs local until cross-module reuse justifies a server fixtures project.
