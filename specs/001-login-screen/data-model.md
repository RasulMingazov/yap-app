# Phase 1 Data Model: Login Screen

Three views of the same three concepts: what the server persists, what the client keeps, and what
the feature's domain exposes. Wire shapes are in [contracts/auth-api.md](contracts/auth-api.md).

## Server persistence

Owned by `services/server/feature-auth`, created by a forward-only Flyway migration at
`src/main/resources/db/migration/V1__auth.sql`. PostgreSQL 17.

### `users`

| Column | Type | Notes |
| --- | --- | --- |
| `id` | `uuid` | primary key |
| `created_at` | `timestamptz` | not null, default `now()` |

The account itself. It holds no profile: nothing on the login screen collects one, and the Notion
page states no registration step exists. Learning progress produced by later features hangs off
this row.

### `provider_identities`

| Column | Type | Notes |
| --- | --- | --- |
| `id` | `uuid` | primary key |
| `user_id` | `uuid` | not null, references `users(id)` on delete cascade |
| `provider` | `text` | not null; `google` is the only value written in this feature |
| `provider_user_id` | `text` | not null; Google's `sub` claim — required, never nullable |
| `email` | `text` | nullable; refreshed on every successful login (FR-026) |
| `display_name` | `text` | nullable; refreshed on every successful login |
| `avatar_url` | `text` | nullable; refreshed on every successful login |
| `created_at` | `timestamptz` | not null, default `now()` |

- **`unique (provider, provider_user_id)`** — this is what makes FR-007 hold. Two concurrent first
  logins for the same Google account race on the insert; the loser retries the lookup and finds
  the winner's row. Proven by a Testcontainers test, not by application logic.
- **`email`, `display_name`, and `avatar_url` carry no unique constraint and no index.** FR-026
  stores them as descriptive data; FR-008 forbids linking providers automatically. Nothing ever
  queries by them, which is the mechanism — an index would invite exactly the lookup that must
  not exist.
- All three are **nullable**: Google's ID token may omit `name` or `picture`, and FR-026 requires
  an omitted descriptive field to be stored as absent rather than to block login.
- `provider_user_id` is the one field that is not optional. A verified token carrying no `sub` is
  rejected before this table is reached (`contracts/auth-api.md`), so the `not null` constraint is
  never the thing that reports the problem — it is the backstop proving no row can exist without
  the only value accounts are matched by (FR-026, FR-007).
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
  concurrent rotations cannot both commit. Proven by a Testcontainers test.
- **Rotation also pushes `expires_at` forward by another 90 days.** That is what makes FR-025's
  window sliding rather than fixed: an active user's session never expires, an untouched one dies
  90 days after its last renewal. This row is the only place `expires_at` is ever written, which
  is the mechanism behind FR-025's rule that only a successful exchange with the server renews a
  session — a device that never reaches the server has no way to move the date.
- A session past `expires_at` is invalid; refresh fails and the client signs out (FR-012).
- Index on `user_id`.

## Client persistence

One record, written by `SessionStorage` (Android Keystore-encrypted DataStore; iOS Keychain).

`SessionLocal`

| Field | Notes |
| --- | --- |
| `accessToken` | bearer token attached by `core-network`'s `authenticated()` modifier |
| `refreshToken` | credential; the reason the store is encrypted |
| `accessTokenExpiresAtEpochSeconds` | used to refresh proactively rather than only on 401 |
| `refreshTokenExpiresAtEpochSeconds` | the session's own 90-day expiry, checked at launch (FR-011) |

`accessTokenExpiresAtEpochSeconds` is what the launch renewal reads: once auth state has resolved
to logged-in, an access token already expired or **within five minutes** of expiring triggers one
refresh (FR-032), which is the only thing in this feature that reaches the server after login and
therefore the only thing that ever moves the 90-day window. That comparison belongs to
`RenewSessionUseCase` inside `impl`, not to `app-root`: this record is `internal` and never leaves
the feature, so the caller supplies the trigger and the feature supplies the decision.

Logged out means either no record at all or a record already past
`refreshTokenExpiresAtEpochSeconds`: FR-011 requires that second check to happen locally, before
any request, so a long-dormant user reaches the login screen directly instead of the main screen
followed by an ejection. The check is a courtesy only — the server remains the authority, and a
wrong device clock can cost a needless login but can never grant access. This record is the only
thing that survives app restart, and reading it at launch is the whole of the launch decision.

Two rules govern writes to it, both from FR-012:

- **Both expiry fields are copied from the server's `SessionDto`, never computed or advanced on
  the device.** The client has no TTL of its own and never moves a date forward, which is what
  keeps FR-025's "only a server exchange renews a session" true by construction rather than by
  discipline.
- **The record is deleted only on an explicit rejection** — a `400` or `401` from
  `/v1/auth/refresh`, or the local expiry having passed. A failure to reach the server at all,
  including a `429` or a `5xx`, leaves it exactly as it is (SC-014).

## Client domain

Declared in `feature-auth/api` because these types appear in use-case signatures; everything else
in the feature stays `internal` in `impl`.

### `AuthState`

```text
AuthState
├── Unknown                        — storage not yet read; splash still up (FR-024)
├── LoggedOut
└── LoggedIn(userId: UserId)
```

Published as a `Flow<AuthState>` by `ObserveAuthStateUseCase`. `app-root` collects it and chooses
the back stack root, which is how FR-009 (success reaches the main screen), FR-011 (restored
session skips the login screen), and FR-012 (invalid session returns to login) are all served by
one observation.

`Unknown` is the initial value and exists solely to satisfy FR-024. Starting at `LoggedOut`
instead would push the login screen onto the back stack and then replace it, which is precisely
the flash that requirement forbids.

### `AuthProvider`

```text
AuthProvider = GOOGLE | APPLE | T_ID
```

Which providers exist is fixed; which are **shown** comes from `Platform` (Apple on iOS only,
FR-003) and which are **usable** is a per-provider fact — only `GOOGLE` is usable in this feature
(FR-004). Both decisions live in `LoginUiStateMapper`, so both are provable by a mapper test.

### `LoginOutcome`

```text
LoginOutcome
├── Success
├── Cancelled          — emits no message (FR-013); also the 60-second timeout
└── Failed             — emits exactly one message (FR-014)
```

A typed result rather than an exception, because the three branches change caller behavior — which
is the only condition under which `docs/mobile/002-domain.md` permits a typed result.
`NotAvailable` is deliberately **not** a member: choosing Apple or T-ID never starts an attempt
(FR-004), so the mapper reports it from `UiState` availability and the view model never calls the
use case.

## Presentation state

`LoginViewModel.UiState`, derived by `LoginUiStateMapper` from domain state, `Platform`, and
`MotionPreferences`.

| Field | Requirement it serves |
| --- | --- |
| `isLoggingIn` | FR-015 — progress shown, second activation ignored |
| `isProviderSheetVisible` | FR-002, FR-016 — repeatable state, not a consumable flag |
| `providers: List<Provider>` where `Provider(provider, labelRes, isAvailable)` | FR-003, FR-004 |
| `isMotionReduced` | FR-020 |
| `topicRes: List<StringResource>` | FR-017 — the rotating topic words |

`News.ShowMessage(StringResource)` carries the failure text and the not-yet-available notice
(FR-023). Nothing consumable appears in `UiState`, and `News` never carries navigation.
