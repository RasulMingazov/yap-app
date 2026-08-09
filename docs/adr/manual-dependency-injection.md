# Manual Dependency Injection Containers

- Status: Accepted
- Date: 2026-08-09
- Scope: Dependency wiring and lifetime ownership under `apps/mobile/*`

## Context

Features need explicit dependency wiring without exposing internal graphs or introducing a DI framework. Containers also define lifetime boundaries for application, authenticated-user, and navigation-owned state.

## Decision

Use constructor injection assembled by manual containers. A container is a composition boundary and lifetime owner, not a service locator passed through application code.

For a graph consumed across a Gradle-module boundary, use this shape:

```kotlin
interface ProfileContainer {

    val profileComponentFactory: ProfileComponent.Factory
}

internal class DefaultProfileContainer(
    networkClient: NetworkClient,
    session: AuthenticatedSession,
) : ProfileContainer {

    private val profileRepository: ProfileRepository by lazy {
        DefaultProfileRepository(
            api = networkClient.createProfileApi(),
            userId = session.userId,
        )
    }

    private val observeProfileUseCase: ObserveProfileUseCase by lazy {
        DefaultObserveProfileUseCase(profileRepository)
    }

    override val profileComponentFactory: ProfileComponent.Factory by lazy {
        DefaultProfileComponent.Factory(observeProfileUseCase)
    }
}

fun createProfileContainer(
    networkClient: NetworkClient,
    session: AuthenticatedSession,
): ProfileContainer = DefaultProfileContainer(
    networkClient = networkClient,
    session = session,
)
```

## Container rules

- Name the boundary `FeatureContainer`, implementation `DefaultFeatureContainer`, and creation function `createFeatureContainer(...)`.
- Make the interface public only when another Gradle module needs it. Otherwise keep it and its factory `internal`, or omit the interface if no substitution boundary exists.
- Keep `DefaultFeatureContainer` `internal`; production creates it only through the factory.
- Accept only dependencies owned by a wider scope: application infrastructure, authenticated-session inputs, platform bridges, configuration, or narrow integration ports.
- Construct feature data sources, repositories, use cases, models, mappers, and default components inside the container. The host must not assemble feature internals.
- Keep graph nodes `private` unless they are deliberate cross-module integration ports. Expose the root component factory by default.
- Type a graph node by its contract when a contract exists while constructing the default implementation in the container.
- Use `by lazy` when creation should wait for a branch or consumer. Eager construction is valid for cheap, unconditional dependencies.
- Inject collaborators through constructors or focused factories. Never pass the container into components, models, use cases, repositories, composables, or arbitrary application code.
- Do not add string- or key-based lookup methods.

## Hierarchy and platforms

- The application host creates application-scoped infrastructure such as `NetworkContainer` once and passes its narrow products to composition containers.
- Only entry-point or composition modules may combine multiple feature containers. A product feature must not import a sibling feature's container or implementation.
- Parent navigators receive child factories or provider lambdas. Cold navigation branches must not create child containers, repositories, models, or subscriptions.
- Platform entry points may expose `createAndroidFeatureContainer(...)` or `createIosFeatureContainer(...)`. These facades create platform adapters and delegate the common graph to `DefaultFeatureContainer`.
- When a platform container adds lifecycle hooks, its contract may extend the common contract and its default implementation should delegate the common portion with Kotlin `by delegate`.

## Scope and cleanup

- Application containers live for the application instance. The scope that creates an `AutoCloseable` resource closes it.
- Authenticated containers are created with the authenticated branch and released on logout or account replacement.
- Navigation containers are created when their branch activates and become unreachable when it is destroyed.
- A component factory may outlive a component only when every invocation creates fresh screen-scoped dependencies. It must not reuse mutable model or subscription state across component instances.

Tests replace the same narrow contracts or factories production uses. Cross-module stub ownership follows the [test-fixture rules](../testing/005-test-fixtures.md).

## Consequences

Construction and lifetimes remain explicit without framework-generated behavior. Manual wiring is more verbose, but the graph stays local and module boundaries remain visible.

## Compliance

Do not introduce a global container, mutable singleton graph, service locator, or DI framework without a separate ADR. Do not add a container to a module with no graph to assemble.
