# Feature Boundaries

The server uses feature-first modules so product scenarios stay cohesive and shared infrastructure remains feature-independent.

## Ownership

- `core-config` owns environment loading and validation.
- `core-database` owns connection, migration bootstrap, and transaction infrastructure—not feature schemas.
- `core-security` owns token and credential issuance and verification.
- `feature-*` owns one product slice and may depend on `core-*`.
- `app` owns process lifecycle, shared Ktor plugins, error mapping, health endpoints, and manual graph construction.

Core modules never depend on features. Features and core modules never depend on `app`. Promote feature code to core only for independent reuse, build, lifecycle, or ownership.

## Feature shape

Use this as a menu, not an empty package template:

```text
feature-auth/
├── api/          routes and HTTP contracts
├── model/        framework-free values and failures
├── identity/     provider verification
├── persistence/  repository, tables, and migrations
└── AuthService   scenario orchestration
```

- Routes validate and translate HTTP only.
- Use [shared DTOs](../shared/README.md) for cross-client wire payloads. Keep server-only HTTP contracts beside their routes.
- Translate expected failures through shared status handling.
- Keep credential and profile responses separate.
- Do not expose provider exceptions, rows, JWT claims, or framework types as feature models.
- Keep provider-specific configuration in its feature.
- Prefer one cohesive service over one use-case type per endpoint.
- Extract a policy only for an independent rule, state, or lifecycle.
- Preserve coroutine cancellation and translate adapter failures at their boundary.
- Default declarations to `internal`; widen visibility for a real boundary only.
- Wire dependencies manually.

Persistence rules follow [Server Persistence](002-persistence.md).
