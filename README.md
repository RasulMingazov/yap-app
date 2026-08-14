# Yap App

Yap App is a single Gradle monorepo containing a Kotlin Multiplatform mobile client,
a modular Kotlin/JVM server, and shared wire contracts.

Shared Gradle policy is implemented by the included `convention-plugins` build.
Individual modules declare only their role-specific plugins and dependencies.

The authentication slice is implemented end to end: a Compose Multiplatform login
screen, Google login on Android and iOS, and a Ktor server that verifies the Google
confirmation, resolves or creates the account, and issues a session that survives
restart. Everything beyond it is still scaffolding.

## Modules

```text
shared/
└── contract/auth              # platform-neutral auth wire contract

apps/mobile/
├── android-app                # Android entry point: MainActivity, splash, OAuth redirect
├── ios-app                    # Xcode host: YapShared.framework, GoogleSignIn
├── shared-app                 # platform entry points and iOS framework
├── app-root                   # composition root: Koin graph, back stack, launch renewal
├── feature-auth/api           # what other modules may see of authentication
├── feature-auth/impl          # the screen, the adapters, and the session
├── core-common                # shared coroutine/presentation/platform primitives
├── core-design                # shared visual primitives
├── core-network               # shared HTTP client infrastructure
└── core-test                  # shared test utilities

services/server/
├── app                        # server process: plugins, error mapping, rate limit, graph
├── feature-auth               # auth routes and server business capability
├── core-config                # environment and runtime configuration
├── core-database              # database bootstrap and transactions
└── core-security              # token and authentication infrastructure
```

A mobile feature is exactly two modules: `api`, which other modules may depend on,
and `impl`, which nothing outside the feature may — except the composition root,
which loads its Koin module. Feature modules do not depend on other feature
implementations. Entry-point modules compose features, and both client and server
may depend on the shared auth contract.

## Running it

Server configuration is read from `.env` at the repository root; copy
[`.env.example`](.env.example) and fill it in. Then:

```shell
./gradlew :services:server:app:run          # server, on http://localhost:8080
./gradlew :apps:mobile:android-app:installDebug
```

For iOS, build `:apps:mobile:shared-app` first so `YapShared.framework` exists, then
open `apps/mobile/ios-app/YapApp.xcodeproj` — see
[`apps/mobile/ios-app/README.md`](apps/mobile/ios-app/README.md).

## Documentation

Start with [`docs/README.md`](docs/README.md), then read only the guides relevant
to the area being changed. Android platform mechanics come from the official
[`android/skills`](https://github.com/android/skills) plugin, enabled for this
repository in `.claude/settings.json`.

## Build

```shell
./gradlew build
```

This runs the PostgreSQL integration suite through Testcontainers, so Docker must be
running. Without it those tests are skipped, and the database behavior is not verified.

Compile the shared iOS framework sources explicitly with:

```shell
./gradlew :apps:mobile:shared-app:compileKotlinIosSimulatorArm64
```
