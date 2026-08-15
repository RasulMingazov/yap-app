# Data Model: Login Screen

**Branch**: `feature/001-login-screen` | **Feature ID**: `001-login-screen` | **Refreshed**: 2026-08-15

Four views of the same slice: what the server persists, what the client keeps, what the feature's
domain exposes, and what presentation holds. Wire shapes are in
[contracts/auth-api.md](contracts/auth-api.md); the public Kotlin surface is in
[contracts/feature-auth-api.md](contracts/feature-auth-api.md).

## Server persistence

Owned by `services/server/feature-auth`, created by a forward-only Flyway migration at
`src/main/resources/db/migration/V1__auth.sql`. PostgreSQL 17.

### `users`

| Column | Type | Notes |
| --- | --- | --- |
| `id` | `uuid` | primary key |
| `created_at` | `timestamptz` | not null, default `now()` |

The account itself. It holds no profile: nothing on the login screen collects one. Learning
progress produced by later features hangs off this row.

### `provider_identities`

| Column | Type | Notes |
| --- | --- | --- |
| `id` | `uuid` | primary key |
| `user_id` | `uuid` | not null, references `users(id)` on delete cascade |
| `provider` | `text` | not null; `google` is the only value written in this feature |
| `provider_user_id` | `text` | not null; Google's `sub` claim — required, never nullable |
| `email` | `text` | nullable; refreshed on every successful login (FR-021) |
| `display_name` | `text` | nullable; refreshed on every successful login |
| `avatar_url` | `text` | nullable; refreshed on every successful login |
| `created_at` | `timestamptz` | not null, default `now()` |

- **`unique (provider, provider_user_id)`** is what makes FR-019 hold. Two concurrent first logins
  race on the insert; the loser retries the lookup and finds the winner's row. Proven by a
  Testcontainers test, not by application logic.
- **`email`, `display_name`, `avatar_url` carry no unique constraint and no index.** Nothing ever
  queries by them, which is the mechanism behind FR-020 — an index would invite exactly the lookup
  that must not exist. All three are nullable, because Google's ID token may omit `name` or
  `picture` and FR-021 requires an omitted field to be stored as absent rather than block login.
- `provider_user_id` is the one field that is not optional. A verified token carrying no `sub` is
  rejected before this table is reached; the `not null` constraint is the backstop.
- Index on `user_id` for the reverse lookup.

### `sessions`

| Column | Type | Notes |
| --- | --- | --- |
| `id` | `uuid` | primary key; matches `RefreshToken.sessionId` and the JWT `sid` claim |
| `user_id` | `uuid` | not null, references `users(id)` on delete cascade |
| `refresh_token_hash` | `text` | not null; SHA-256 via `TokenService.hash`, never the raw token |
| `expires_at` | `timestamptz` | not null; last renewal plus `REFRESH_TOKEN_TTL_SECONDS` (90 days) |
| `created_at` | `timestamptz` | not null, default `now()` |
| `rotated_at` | `timestamptz` | nullable; last successful rotation |

- Rotation validates the presented hash and writes the new one **inside one transaction**, so two
  concurrent rotations cannot both commit.
- **Rotation also pushes `expires_at` forward by another 90 days**, which is what makes FR-026's
  window sliding. This row is the only place `expires_at` is written — the mechanism behind "only a
  server exchange renews a session".
- A session past `expires_at` is invalid; refresh fails and the client signs out (FR-025).
- A successful login invalidates every earlier session for the account, inside the login
  transaction (FR-027). Index on `user_id`.

## Client persistence

One record, written by `SessionStorage` (Android Keystore-encrypted DataStore; iOS Keychain) and
owned by `SessionStore`.

`SessionLocal`

| Field | Notes |
| --- | --- |
| `accessToken` | bearer token attached by `core-network`'s `authenticated()` modifier |
| `refreshToken` | credential; the reason the store is encrypted |
| `accessTokenExpiresAtEpochSeconds` | read by the launch refresh's five-minute margin (FR-028) |
| `refreshTokenExpiresAtEpochSeconds` | the session's own 90-day expiry, checked at launch (FR-024) |

`SessionStore` is the single owner of that record and of the published state:

| Member | Rule |
| --- | --- |
| `sessionState: StateFlow<AuthSessionState>` | starts at `Unknown`; the only publisher |
| `resolveOnce()` | reads storage once behind a mutex; a record past its refresh-token expiry is cleared and resolves to `LoggedOut` |
| `read()` | the stored record, for the token provider and the refresh margin |
| `write(SessionDto)` | maps to `SessionLocal`, persists, publishes `LoggedIn` |
| `forget()` | clears storage, publishes `LoggedOut` |

Two rules govern writes, both from FR-025:

- **Both expiry fields are copied from the server's `SessionDto`, never computed on the device.**
  The client has no TTL of its own, which keeps FR-026 true by construction.
- **The record is deleted only on an explicit rejection** — `ApiError.Rejected` or
  `ApiError.Unauthorized` from `/v1/auth/refresh`, or the local expiry having passed. Anything that
  is not an answer about the session, including a `429` or a `5xx`, arrives as
  `ApiError.Unavailable` and leaves it exactly as it is (SC-014).

Logged out means either no record or a record past `refreshTokenExpiresAtEpochSeconds`. FR-024
requires that second check locally, before any request. It is a courtesy only — the server remains
the authority, and a wrong device clock can cost a needless login but never grant access.

## Client domain

Declared in `feature-auth/api` because these types appear in use-case signatures; everything else
stays `internal` in `impl`.

### `AuthSessionState`

```text
AuthSessionState
├── Unknown                        — storage not yet read; splash still up (FR-002)
├── LoggedOut
└── LoggedIn(userId: UserId)
```

Published by `ObserveAuthSessionStateUseCase`. `RootBackStack` collects it and chooses the back
stack's base, which is how FR-022, FR-024, and FR-025 are all served by one observation. `Unknown`
is the initial value and exists solely for FR-002: starting at `LoggedOut` would push the login
screen and then replace it, which is the flash that requirement forbids.

### `AuthProvider` and `AuthProviderType`

```kotlin
enum class AuthProviderType { APPLE, GOOGLE, T_ID }

data class AuthProvider(
    val type: AuthProviderType,
    val isEnabled: Boolean,
    val isVisible: Boolean,
)
```

| Field | Rule |
| --- | --- |
| `type` | identity only — no label, icon, order, or platform |
| `isVisible` | whether this device offers the provider at all |
| `isEnabled` | whether it can be chosen — false while a provider is announced but not shipped |

Both facts are per-instance runtime values, so the roster sets them per device today and a backend
can set them later (FR-006). Invariants: only the roster constructs instances; a provider with
`isEnabled = true` must resolve a `ProviderLogin`, checked by a wiring test, and no two handlers may
claim one type; a provider with `isEnabled = false` is never signed in with, even if a handler
exists — `DefaultLoginUseCase` reads the flag and returns `Unavailable` (FR-012).

### `LoginOutcome`

| Case | Meaning |
| --- | --- |
| `Success` | session established |
| `Cancelled` | the person backed out, or the 60-second attempt bound elapsed (FR-029) |
| `Failed` | the attempt ran and did not succeed; emits exactly one message (FR-030) |
| `Unavailable` | no usable login path for the requested provider (FR-012) |

A typed result rather than an exception, because the branches change caller behaviour — the only
condition under which `docs/mobile/002-domain.md` permits one.

### `LegalLinks`

`LegalLinks(privacyUrl: String?, termsUrl: String?)`, served by `GetLegalLinksUseCase` from values
passed into `featureAuthModule(...)`. A null destination renders its link but does not navigate
(FR-051), so the documents arriving is a configuration change rather than a code change.

### Repositories and the login port (`impl`)

| Type | Members | Rule |
| --- | --- | --- |
| `AuthSessionRepository` | `observe(): Flow<AuthSessionState>`, `refresh()` | the session lifecycle; `refresh()` applies the five-minute margin and delegates rotation to `AccessTokenProvider` |
| `GoogleAuthRepository` | `login(): LoginOutcome` | one provider's login path; a second provider adds its own repository, never a method here (FR-063) |
| `ProviderLogin` | `type: AuthProviderType`, `login(): LoginOutcome` | the domain port each provider registers; never returns `Unavailable` |

`GoogleProviderLogin` is the only implementation of `ProviderLogin` and delegates to
`GoogleAuthRepository`. The 60-second attempt bound lives in `DefaultLoginUseCase`, so every
provider inherits it.

### The roster — `DefaultObserveAuthProvidersUseCase`

The single source of which providers exist and how they may be used (FR-007).

| Aspect | Rule |
| --- | --- |
| Emission | `Flow<List<AuthProvider>>`, cold, no network or storage work |
| Order | `Google`, `Apple`, `T-ID` |
| `isVisible` | Apple on iOS only; Google and T-ID on both |
| `isEnabled` | Google only, until another provider ships a `ProviderLogin` |
| Input | `Platform` from `core-common` — a platform-neutral value, so this stays in `domain` |

There is deliberately **no repository behind it yet**: the rule is a pure function of `Platform`.
The `Flow` return type is the seam a remote roster slots into.

## Presentation state

### `LoginViewModel`

| Part | Content |
| --- | --- |
| dependencies | `getLegalLinksUseCase`, `loginUseCase`, `motionPreferences`, `navigator`, `newsMapper`, `uiStateMapper` |
| `DataState` | `isLoggingIn`, `legalLinks` |
| `UiState` | `isLoggingIn`, `isMotionReduced`, `privacyUrl`, `termsUrl`, `topics` |
| `Event` | `PrimaryActionClicked`, `ProviderChosen(provider)` |
| `News` | `ShowMessage(message, argument)` — the argument names the provider |

`PrimaryActionClicked` navigates to `AuthNavKey.SelectAuthProvider` through `Navigator`.
`ProviderChosen` arrives from the composable, which reads it off the navigation result bus, and
calls `loginUseCase(provider)`; `LoginNewsMapper` turns the outcome into news — `Failed` raises
`login_failed`, `Unavailable` raises `login_provider_soon` with the provider's name, `Success` and
`Cancelled` are silent. Nothing consumable appears in `UiState`, and `News` never carries
navigation.

Required to stay absent by FR-040 and SC-020: a sheet visibility flag, a provider list, a provider
declaration set, `platform`, and any map from provider to login.

### `SelectAuthProviderViewModel`

| Part | Content |
| --- | --- |
| `DataState` | `providers: List<AuthProvider>` |
| `UiState` | `providers: List<Provider>` where `Provider(provider, ui: AuthProviderUi)` |
| `Event` | `ProviderChosen` — navigate back after the composable has sent the result |
| `News` | none |

`SelectAuthProviderUiStateMapper` builds rows through `AuthProviderUiMapper`, dropping providers
whose `isVisible` is false. The row keeps the provider itself, so its `isEnabled` needs no second
copy. The mapper applies no platform rule and no ordering — the roster already did — and touches no
theme value.

### `AuthProviderUi` and `AuthProviderUiMapper` (`presentation/common`)

The one table where a provider identity becomes display data (FR-038):
`AuthProviderUi(iconRes, isMonochrome, labelRes, testTag)`, produced by a `when` over
`AuthProviderType`. Shared by both slices so neither owns a second copy; `LoginViewModel` reads it
for one purpose only — the provider's name inside the coming-soon message. `isMonochrome` is a fact
about the asset, not a theme value: the Apple mark is drawn in the row's content colour, while the
Google and T-ID marks carry brand colours. The composable branches on that flag, never on the
provider.

## Design system

### `YapColors` (`core-design`)

Role-named colours, immutable, provided by `LocalYapColors` and exposed as `YapTheme.colors`; the
Material `ColorScheme` for each theme is derived from the same values. Values and their design keys
are in [research.md](research.md) R23. Rules: a role name states purpose, never a screen or product
name (FR-042); `highlight`, `onHighlight`, `notice`, and `onNotice` hold the same value in both
themes (FR-044); brand colours belong to provider drawables, never here (FR-043).

### Provider drawables (`feature-auth/impl`)

`ic_provider_google.xml`, `ic_provider_apple.xml`, `ic_provider_t_id.xml`, converted from the
design's markup — see [research.md](research.md) R25.

## Navigation

| Key | Owner | Presentation |
| --- | --- | --- |
| `AuthNavKey.Login` | `feature-auth/api` | full screen |
| `AuthNavKey.SelectAuthProvider` | `feature-auth/api` | bottom-sheet overlay via entry metadata |
| `RootNavKey.Main` | `app-root` | full screen, placeholder until the home feature exists |

`Navigator` (`core-common`): `navigate(key: NavKey)` and `back()`, implemented by `RootBackStack`,
which keeps its session-derived base and a mutable tail. The chosen provider travels on Navigation
3's own result bus under the shared key `AuthResultKeys.PROVIDER_SELECTION` — see
[research.md](research.md) R20.

## Source layout rule for presentation files

A declaration belongs to `…presentation.<slice>.ui` when it **draws or styles the screen, or names
a drawn element**. It stays in `…presentation.<slice>` when it **produces or describes state**
(FR-053, FR-054).

| Slice | Rendering package (`…/ui/`) | State package |
| --- | --- | --- |
| `login` | `LoginScreen`, `LoginSnackbarHost`, `LegalLine`, `LoginTestTags` | `LoginViewModel`, `LoginUiStateMapper`, `LoginNewsMapper` |
| `selectprovider` | `SelectAuthProviderScreen`, `SelectAuthProviderTestTags` | `SelectAuthProviderViewModel`, `SelectAuthProviderUiStateMapper` |
| `common` | — | `AuthProviderUi`, `AuthProviderUiMapper`, `AuthResultKeys` |

Tests mirror the split. The one exception is `ComposeUiTestCase`, an `internal expect abstract
class` in `app.yap.feature.auth.presentation` whose `actual`s must declare the same package, so the
trio does not move.
