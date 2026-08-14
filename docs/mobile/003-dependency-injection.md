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
`initKoin(...)` starts Koin, the root back stack lives there, and `App()` composes destinations with
`koinEntryProvider()`. A feature never receives a back stack as a parameter. Both functions take the
application's configuration as parameters rather than reading it from global state, so their
signatures grow as features are added. Navigation mechanics follow the `navigation-3` skill.

`RootBackStack` is a **`single`**, not a factory: it keeps a base derived from application state
plus a mutable tail that `navigate` pushes and `back` pops, and a change of base drops the tail.
Because the tail is application state rather than composition state, it must survive recomposition
and configuration change. It survives neither process death nor a `factory` binding.

A state holder reports navigation intent through `Navigator` (`core-common`), which `app-root` binds
to that same instance:

```kotlin
single { RootBackStack(observeAuthStateUseCase = get()) }

single<Navigator> { get<RootBackStack>() }
```

`App()` passes the back stack's `back` as `NavDisplay`'s `onBack`, and lists both its entry
decorators and its scene strategies:

```kotlin
NavDisplay(
    backStack = keys,
    onBack = rootBackStack::back,
    entryDecorators = listOf(
        rememberSaveableStateHolderNavEntryDecorator(),
        rememberViewModelStoreNavEntryDecorator(),
        rememberResultEventBusNavEntryDecorator(),
    ),
    sceneStrategies = listOf(BottomSheetSceneStrategy(), SinglePaneSceneStrategy()),
    entryProvider = koinEntryProvider<NavKey>(),
)
```

- `entryDecorators` **replaces** the default list rather than extending it, so the saveable-state
  decorator has to be named alongside anything added. The view-model decorator scopes a
  destination's view models to its entry and clears them when it pops; the result bus decorator lets
  a destination hand a value back to the one below it.
- Both the decorator list and the scene-strategy list are `remember`ed. A fresh list on every
  composition re-keys the scene state, which tears down an open overlay and composes a second one.
- Overlay scene strategies come before the single-pane one; the first that returns a scene wins.
- A destination opts into a scene through its own metadata in its feature's Koin module, so
  `app-root` learns nothing about the feature:

  ```kotlin
  navigation<AuthNavKey.SelectAuthProvider>(metadata = bottomSheetScene()) { SelectAuthProviderScreen() }
  ```

  `bottomSheetScene()` lives in `core-design` beside the strategy that reads it: `app-root` composes
  `NavDisplay` with the strategy while the feature attaches the metadata, and a feature may not
  depend on `app-root`.

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
