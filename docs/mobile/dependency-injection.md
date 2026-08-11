# Dependency Injection

Mobile uses manual constructor injection so graphs and lifetimes remain explicit without a DI framework or service locator.

```kotlin
interface ProfileContainer {

    val profileComponentFactory: ProfileComponent.Factory
}

internal class DefaultProfileContainer(
    networkClient: NetworkClient,
    session: AuthenticatedSession,
) : ProfileContainer {

    private val repository: ProfileRepository by lazy {
        DefaultProfileRepository(
            api = networkClient.createProfileApi(),
            userId = session.userId,
        )
    }

    private val viewModelFactory: ProfileViewModel.Factory by lazy {
        ProfileViewModel.Factory(
            observeProfileUseCase = DefaultObserveProfileUseCase(repository),
        )
    }

    override val profileComponentFactory: ProfileComponent.Factory =
        DefaultProfileComponent.Factory(
            viewModelFactory = viewModelFactory,
        )
}
```

## Containers

- Name the boundary `FeatureContainer`, implementation `DefaultFeatureContainer`, and entry point `createFeatureContainer(...)`.
- Make the boundary public only when it crosses a Gradle-module boundary.
- Accept only wider-scope infrastructure, session inputs, platform bridges, configuration, or narrow integration ports.
- Construct sources, repositories, use cases, mappers, view model factories, and component factories inside the container.
- Keep graph nodes private and expose the root component factory by default.
- Use `by lazy` for cold branches; eager construction is valid for cheap unconditional dependencies.
- Never pass a container into application code or add keyed lookup methods.

## View models and components

- A view model factory owns all view-model dependencies and never caches the view model.
- A default component receives the view model factory and calls `instanceKeeper.getOrCreate { viewModelFactory.invoke(output) }`.
- `InstanceKeeper`, not a factory or container, owns view model reuse.
- Component and view model factories are invoked as `factory(...)`; both declare `operator fun invoke`.
- Parent navigators receive child factories or provider lambdas.
- A cold component factory keeps dependency providers and resolves them only when invoked.
- Cold branches may create lightweight factories but not containers, repositories, view models, or subscriptions.

## Scopes

- Application containers live for the application instance and close resources they create.
- Authenticated containers are recreated on login, logout, or account replacement.
- Navigation containers exist only while their branch exists.
- Platform facades create platform adapters and delegate the common graph.
- Component and view model factories may outlive their products but must not retain mutable screen state.

Tests replace the same narrow contracts or factories as production. Cross-module stubs follow [Fixtures](../testing/005-fixtures.md).
