# Yap App

Yap App is a single Gradle monorepo containing a Kotlin Multiplatform mobile client,
a modular Kotlin/JVM server, and shared wire contracts.

Shared Gradle policy is implemented by the included `convention-plugins` build.
Individual modules declare only their role-specific plugins and dependencies.

Only the authentication slice is scaffolded. The repository currently contains
build configuration and module boundaries, but no authentication behavior or
application source code.

## Modules

```text
shared/
└── contract/auth              # platform-neutral auth wire contract

apps/mobile/
├── android-app                # Android entry point
├── ios-app                    # Xcode host placeholder
├── shared-app                 # shared UI entry point and iOS framework
├── app-root                   # bootstrap and composition root
├── feature-auth               # authentication presentation/orchestration
├── core-common                # shared coroutine/presentation primitives
├── core-design                # shared visual primitives
├── core-network               # shared HTTP client infrastructure
└── core-test                  # shared test utilities

services/server/
├── app                        # server composition module
├── feature-auth               # auth routes and server business capability
├── core-config                # environment and runtime configuration
├── core-database              # database bootstrap and transactions
└── core-security              # token and authentication infrastructure
```

Feature modules do not depend on other feature implementations. Entry-point
modules compose features, and both client and server may depend on the shared
auth contract.

## Build

```shell
./gradlew build
```

Compile the shared iOS framework sources explicitly with:

```shell
./gradlew :apps:mobile:shared-app:compileKotlinIosSimulatorArm64
```
