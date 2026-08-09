# Shared Contracts

Wire contracts under `shared/contract/*` are shared by mobile and server.
All Kotlin in this area also follows [Code conventions](../code-conventions.md).

## Naming

- Name every serialized wire type with the `Dto` suffix.
- Name each DTO by its payload meaning.

```text
CredentialsDto
SessionDto
UserDto
```

## Boundary

- DTOs contain wire data only; they are not domain entities, database models, or feature results.
- Mobile maps DTOs to domain at the repository boundary.
- Server maps DTOs to feature models at the API boundary.
