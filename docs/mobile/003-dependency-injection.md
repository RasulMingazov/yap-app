# Dependency Injection

Koin declares each graph node once and resolves it by contract type. The module function is public
in **`impl`**; everything it declares is `internal` in `impl`. It cannot live in `api`: a function
there could not bind declarations that are `internal` to `impl`, which is why `app-root` depends on
both modules — `api` to compose destinations, `impl` to load the module.

```kotlin
fun featureProfileModule(): Module = module {
    single<ProfileRepository> { DefaultProfileRepository(api = get<NetworkClient>().createProfileApi()) }

    factory<ObserveProfileUseCase> { DefaultObserveProfileUseCase(get()) }

    viewModel { ProfileViewModel(observeProfileUseCase = get()) }

    navigation<ProfileNavKey.Overview> { ProfileScreen() }
}
```

- Name a feature module `feature<Name>Module()` and a core module `core<Name>Module()`.
- Bind by contract type, never by implementation type.
- Use `single` for shared instances, `factory` for stateless collaborators, `viewModel` for screens, `navigation<Key>` for destinations.
- Pass configuration as module-function parameters, not through global state.
- Release owned resources with `onClose`; a definition that opens a client, database, or socket must close it.
- Resolve with `get()` only inside a definition. Application code receives constructor parameters and never sees `Koin`, `Scope`, or `KoinComponent`.
- Pass runtime arguments with `parametersOf`, not through a mutable singleton.

## Composition root

`app-root` owns the whole common composition root: `appModules(...)` lists every module,
`initKoin(...)` starts Koin, the root back stack is derived there from application state, and
`App()` composes destinations with `koinEntryProvider()`. A feature never receives a back stack as
a parameter. Both functions take the application's configuration as parameters rather than reading
it from global state, so their signatures grow as features are added. Navigation mechanics follow
the `navigation-3` skill.

`shared-app` holds only the platform entry points and the iOS framework, and deliberately has **no
`commonMain` sources**: Kotlin/Native exports a framework module's own public declarations into the
generated Objective-C header, so common code there would widen the Swift surface silently. Platform
entry points call `initKoin` and add platform-only modules through its `appDeclaration` parameter.
No other module starts Koin.

## Scopes

- Application-wide collaborators are root singletons and live for the process.
- Session-bound state that must be dropped on logout belongs to a dedicated module loaded on login and unloaded on logout; do not keep it in a root singleton.
- An inactive destination must not create view models, subscriptions, or data loads.

## Verification

Cover module wiring with a Koin `verify()` test in the owning module so a missing or mistyped binding fails at build time rather than at first navigation. Tests override a binding by loading a module with the stub declaration instead of rebuilding the graph by hand.
