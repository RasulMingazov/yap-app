# Data Model: Auth Provider Selection and Login Theming Refactor

No persisted schema changes. The models below are in-memory: domain entities in `feature-auth/api`,
ports in `feature-auth/impl`, and presentation state owned by each view model.

## Domain

### AuthProvider (`api/entity`, changed)

A sealed hierarchy — `Apple`, `Google`, `TId` — whose members are `data class`es constructed at
runtime, so both flags are runtime values. The declaration itself is written once, in
[contracts/feature-auth-api.md](contracts/feature-auth-api.md); the reasoning behind the shape is
[research.md](research.md) R1.

| Field | Rule |
| --- | --- |
| `isVisible` | whether this device offers the provider at all |
| `isEnabled` | whether it can be chosen — false while a provider is announced but not shipped |

Invariants:

- Only the roster constructs instances; nothing else may invent flag combinations.
- A provider with `isEnabled = true` must resolve a `ProviderLogin`, checked by a wiring test.
- A provider with `isEnabled = false` is never signed in with, even if a handler is registered:
  `DefaultLoginUseCase` reads the flag and returns `Unavailable`. Selectability is a login-time
  check, not only a wiring fact — FR-005 covers both the unregistered and the disabled case.
- The type carries no label, icon, order, or platform. Identity is the subclass.

### LoginOutcome (`api/entity`, changed)

| Case | Meaning | Raised when |
| --- | --- | --- |
| `Success` | session established | unchanged |
| `Cancelled` | the person backed out, or the attempt bound elapsed | unchanged |
| `Failed` | the attempt ran and did not succeed | unchanged |
| `Unavailable` | no login path exists for the requested provider | **new** |

### ProviderLogin (`impl/domain/provider`, new)

| Member | Type | Rule |
| --- | --- | --- |
| `provider` | `KClass<out AuthProvider>` | the subclass this implementation serves; unique across bindings |
| `login()` | `suspend () -> LoginOutcome` | performs the attempt; never returns `Unavailable` |

`GoogleProviderLogin` is the only implementation and delegates to `AuthRepository`. The 60-second
attempt bound lives in the use case, so every provider inherits it.

### The roster — `DefaultObserveAuthProvidersUseCase` (`impl/domain/usecase`, new)

The single source of which providers exist and how they may be used.

| Aspect | Rule |
| --- | --- |
| Emission | `Flow<List<AuthProvider>>`, cold, no network or storage work |
| Order | `Google`, `Apple`, `TId` — the positions the deleted catalogue encoded |
| `isVisible` | `Apple` on iOS only; `Google` and `TId` on both |
| `isEnabled` | `Google` only, until another provider ships a `ProviderLogin` |
| Input | `Platform` from `core-common` — a platform-neutral value, so this stays in `domain` |

There is deliberately **no repository or data source behind it yet**: the rule is a pure function of
`Platform` and touches nothing. The `Flow` return type is the seam a remote roster slots into; a port
with a single in-memory implementation would own no behaviour, which the constitution forbids. When a
backend roster lands, the port is introduced then and only this class changes.

## Presentation

### LoginViewModel (simplified)

| Part | Before | After |
| --- | --- | --- |
| dependencies | `Map<AuthProvider, LoginUseCase>`, `platform`, `declarations` | `loginUseCase`, `navigator`, `motionPreferences`, urls |
| `DataState` | `isProviderSheetVisible`, `isLoggingIn` | `isLoggingIn` |
| `UiState` | + `providers`, `isProviderSheetVisible` | neither |
| `Event` | `PrimaryActionClicked`, `ProviderChosen`, `ProviderSheetDismissed` | `PrimaryActionClicked`, `ProviderChosen(provider)` |
| `News` | `ShowMessage(message)` | `ShowMessage(message, argument)` — the argument names the provider |

`PrimaryActionClicked` navigates to `AuthNavKey.SelectAuthProvider` through `Navigator`.
`ProviderChosen` arrives from the composable, which receives it from the navigation result bus, and
calls `loginUseCase(provider)`. `Failed` raises `login_failed`; `Unavailable` raises
`login_provider_soon` with the provider's name as its argument; `Success` and `Cancelled` are silent.
`login_provider_not_available` is replaced by `login_provider_soon`.

### SelectAuthProviderViewModel (new)

| Part | Content |
| --- | --- |
| `DataState` | `providers: List<AuthProvider>` |
| `UiState` | `providers: List<Provider>` |
| `Event` | `ProviderChosen` — navigate back after the composable has sent the result |
| `News` | none |

```kotlin
data class Provider(
    val iconRes: DrawableResource,
    val isEnabled: Boolean,
    val isMonochrome: Boolean,
    val labelRes: StringResource,
    val provider: AuthProvider,
)
```

`AuthProviderResources` is the one place an `AuthProvider` becomes display data: it maps the sealed
members to a label and a mark, branching with `is`. `SelectAuthProviderUiStateMapper` builds rows
from it, dropping providers whose `isVisible` is false and copying `isEnabled` from the instance; it
applies no platform rule and no ordering — the roster already did — and touches no theme value.
`LoginViewModel` reads the same table for one purpose only: the provider's name inside the
"not yet available" message.

`isMonochrome` is a fact about the asset, not a theme value: the Apple mark is drawn in the row's
content colour, while the Google and T-ID marks carry brand colours of their own. The composable
branches on this flag rather than on the provider.

## Design system

### YapColors (`core-design`, new)

Eighteen role-named colours, immutable, provided by `LocalYapColors` and exposed as
`YapTheme.colors`. Values and their design keys are tabulated in [research.md](research.md) R7. The
Material `ColorScheme` for each theme is derived from this set.

Rules:

- A role name states purpose, never a screen or product name.
- `highlight`, `onHighlight`, `notice`, and `onNotice` hold the same value in both themes; every
  other role differs by theme.
- Brand colours belong to provider drawables, never to `YapColors`.
- `LocalIsDarkTheme` is deleted — `LoginColors` was its only consumer.

### Provider drawables (`feature-auth/impl`, new)

`ic_provider_google.xml`, `ic_provider_apple.xml`, `ic_provider_t_id.xml`, converted from the design's
markup. Details in [research.md](research.md) R9.

## Navigation

| Key | Owner | Presentation |
| --- | --- | --- |
| `AuthNavKey.Login` | `feature-auth/api` | full screen, unchanged |
| `AuthNavKey.SelectAuthProvider` | `feature-auth/api`, new | bottom-sheet overlay via metadata |

`Navigator` (`core-common`, new): `navigate(key: NavKey)` and `back()`. Implemented by
`RootBackStack` in `app-root`, which keeps its auth-derived base and gains a mutable tail.

The chosen provider travels on Navigation 3's own result bus rather than through any feature-owned
carrier — see [research.md](research.md) R4.
