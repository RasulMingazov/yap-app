# Yap App Workspace

Yap App is one Gradle monorepo. Authentication is currently the only product scope.

## Boundaries

- Keep mobile, server, and shared-contract code inside their existing module trees.
- Mobile product features must not depend on other mobile feature implementations.
- Server product features must not depend on other server feature implementations.
- `shared:contract:auth` contains only platform-neutral wire types and must not
  depend on mobile or server modules.
- Server feature modules own their routes, domain behavior, and persistence
  mappings. Server core database code owns only bootstrap and transactions.
- Entry-point modules are composition roots and contain no product behavior.
- Keep shared Gradle configuration in `convention-plugins`; module build files
  should contain only role-specific plugins, dependencies, and exceptional setup.
- Do not add another product feature unless explicitly requested.

## Verification

Run `./gradlew build` for repository-wide changes. For KMP boundary changes also
run `./gradlew :apps:mobile:shared-app:compileKotlinIosSimulatorArm64`.

Run `./gradlew detekt` (or `:module:detekt` for a single module) for Kotlin changes.
Shared rule tuning lives in `config/detekt/detekt.yml`; prefer a narrow `@Suppress` at
the declaration for a one-off false positive over widening a rule.

Before updating `stub-call` or its bundled Maven artifacts, read
[Updating StubCall](docs/development/updating-stub-call.md). Mobile feature tests
depend on it transitively through `apps:mobile:core-test`; server modules add
`testImplementation(libs.stubcall)` directly, module by module, as their tests need it.

## Working tree and Git

Run `git config core.hooksPath .githooks` once per clone to enable the `pre-push`
hook that blocks pushing detekt violations.
