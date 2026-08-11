# Feature: Login (001-authentication)

**Created**: 2026-08-09
**Source**: `specify_prompt.md`; visual source — Claude Design project
`https://claude.ai/design/p/0c49e08b-d7ab-4cd3-88be-8483024790e5?file=screen_login.dc.html`
(files `screen_login.dc.html` and `support.js`)

The design baseline is the contents of those two imported files reviewed for this feature.
Before planning, record their SHA-256 hashes in `plan.md`. A later hash change requires explicit
specification review; it must not silently change an accepted requirement.

## Goal

The user must be able to access Yap through an external identity provider with one tap,
without separate registration or a Yap password. The account associates learning progress
(streak and lesson history) with a persistent identity, and a returning user must enter the
product without logging in again.

This iteration delivers working login through Google and Apple with product-controlled provider
availability, session restoration on startup, and recovery after cancellation or login failure.
T-ID is present as a disabled, deferred scaffold: identifiers, configuration, a visible disabled
row, and adapter shells behind the common provider port, with no working T-ID login.

### Terminology

- **Login** is the canonical product and implementation name for the feature; use **log in**
  as a verb and **login** as a noun or modifier.
- The two required UI units are named `Login` and `SelectProvider`. They are sibling screens
  coordinated by an orchestration unit named `Auth`, which is itself one branch of the application
  root beside the minimal authenticated unit.
- Keep official protocol terms such as OAuth authorization code, PKCE, nonce, and `state`.

## Scope

### In scope

- The `Login` screen from `screen_login.dc.html`: animated marquee, rotating topics,
  supporting copy, primary `ВОЙТИ` button, and account-purpose caption.
- The `SelectProvider` bottom sheet with a provider list supplied by injected configuration.
- Production-capable login through Google and Apple, including Yap backend verification,
  account creation or resolution, and Yap session issuance.
- A disabled T-ID scaffold: provider-neutral identifiers and configuration, a visible disabled row,
  compile-safe adapter shells behind the common provider port, and unregistered server-side
  verification scaffolding.
- Provider availability controlled by injected visibility and enabled flags without changing
  UI, navigation, or domain contracts.
- Automatic restoration of a valid session on startup, session credential refresh and rotation,
  and secure on-device session storage.
- Recovery after user cancellation and login failure.
- A minimal authenticated screen containing only `Успешно авторизован`.
- A provider-neutral product analytics contract without personal data.

### Out of scope

- Email/password registration or login.
- Creating or recovering a Yap password.
- Automatic linking or merging of accounts from different providers.
- Completing the T-ID integration: real T-ID login, SDK linkage, PKCE ownership, token exchange,
  userinfo retrieval, and any T-ID physical verification.
- Redesigning `Login` or `SelectProvider` beyond the referenced Claude Design project.
- Selecting or integrating a third-party product analytics destination; the feature owns only
  the event contract.
- A language selector or product locales other than Russian.
- Any authenticated-screen content other than `Успешно авторизован`.

## User scenarios

### Log in with an external provider

- **Given** a user without a valid session opens Yap
- **When** they tap `ВОЙТИ`, select an enabled provider, and confirm with that provider
- **Then** Yap creates or opens the account bound to that identity and displays the
  authenticated screen containing `Успешно авторизован`

### Select a disabled provider

- **Given** `SelectProvider` is open and a provider is visible but disabled by configuration
- **When** the user selects its row
- **Then** `SelectProvider` closes, no SDK or backend call starts, and
  `Вход через X скоро появится` appears once using the injected provider display name

### Return from provider selection

- **Given** `SelectProvider` is open
- **When** the user taps outside the sheet or performs system back navigation
- **Then** the sheet closes and reveals the previous, unchanged `Login` state

### Restore a session automatically

- **Given** a valid session is stored on the device
- **When** the user opens Yap
- **Then** the session is restored without showing `Login` or contacting the provider, and
  the same account opens with its existing learning progress

### Handle an invalid stored session

- **Given** the server definitively rejects the stored session
- **When** the user opens Yap or performs an authenticated request
- **Then** the stored session is cleared and `Login` is shown ready for a new attempt

### Cancel login

- **Given** an enabled provider flow is running
- **When** the user cancels it
- **Then** no error is shown, loading ends, the user remains on `Login`, and they can
  immediately open `SelectProvider` again

### Recover from login failure

- **Given** an enabled provider flow is running
- **When** the provider, network, or backend returns an error
- **Then** a clear Russian error is shown, loading ends, no account is created, and the user
  can retry without restarting the app

## Requirements

### `Login` screen

- **R-001**: A user without a valid session sees `Login` when opening Yap.
- **R-002**: `Login` matches `screen_login.dc.html` in layout, typography, colors, spacing,
  animations, reduced-motion behavior, and light/dark themes. Deviations are allowed only where
  required by the target platform.
- **R-003**: `Login` displays the marquee `ВХОД ЗА 1 ТАП ✦ БЕЗ ПАРОЛЕЙ`, heading
  `БОЛТАЙ БЕЗ СТРАХА`, rotating topics `SMALL TALK`, `ОТКАЗЫ`, and `ЗНАКОМСТВА`,
  supporting copy `Короткие диалоги с ИИ-наставником. Заходишь, говоришь минуту, закрываешь.`,
  button `ВОЙТИ`, and caption `Аккаунт нужен для стрика и истории занятий.`
- **R-004**: The marquee loops seamlessly and infinitely. Its region always contains text;
  an empty frame, gap, or visible reset is not allowed.
- **R-005**: With reduced motion enabled, movement stops while the marquee text remains visible.
- **R-006**: `Login` exposes no direct provider actions; `ВОЙТИ` is its only primary action.
- **R-007**: Repeated taps while opening the sheet or while login is running do not launch the
  action more than once.

### `SelectProvider` screen

- **R-008**: Tapping `ВОЙТИ` results in `SelectProvider` being presented as a modal bottom sheet
  over `Login`; `Login` remains visible beneath the scrim. `Login` requests this outcome; it does
  not perform the presentation itself (see R-087).
- **R-009**: `SelectProvider` is a separate screen in the feature navigation model, and a sibling of
  `Login` rather than a part of it. Closing it by tapping outside or using system back restores the
  previous `Login` state without recreating it.
- **R-010**: `SelectProvider` displays the sheet handle and `СПОСОБЫ ВХОДА` title.
- **R-011**: Each provider appears as a full-width row with an icon and label. Configuration
  defines the order, which is preserved without sorting; the default is Google, Apple where
  supported, then T-ID.
- **R-012**: Provider membership, order, visibility, enabled state, and display names come from
  injected configuration. The UI does not hardcode a provider set or branch by platform/provider.
- **R-013**: If no provider is visible on the current platform, the sheet retains its handle and
  `СПОСОБЫ ВХОДА` title, shows no provider rows, displays
  `Нет доступных способов входа`, and remains dismissible.
- **R-014**: Selecting any provider closes `SelectProvider`.

### Provider availability

- **R-015**: Each provider has independent visibility (whether its row exists on the current
  platform) and enabled state (whether selection launches the real provider flow).
- **R-016**: The default configuration enables Google and disables Apple and T-ID.
- **R-017**: Apple is hidden on Android regardless of its enabled flag. Google and T-ID are
  visible on Android and iOS; Apple is visible on iOS.
- **R-018**: Selecting a visible disabled provider closes `SelectProvider`, does not initialize
  an SDK or call the backend, does not enter loading, and shows
  `Вход через X скоро появится` exactly once, where `X` is the injected display name.
- **R-019**: Selecting a visible enabled provider launches its real login flow.
- **R-020**: Google and Apple are fully implemented in this iteration; disabling either one leaves a
  real, working adapter behind the configuration flag. T-ID is deliberately scaffolded rather than
  implemented (see R-103…R-106) and stays disabled by default.
- **R-021**: Enabling, disabling, hiding, or reordering an implemented provider requires only
  changing injected configuration and no changes to `Login`, `SelectProvider`, navigation,
  domain or wire contracts, backend routes, or login orchestration.
- **R-022**: Adding a provider requires its own client adapter, server verification, and
  configuration registration, but no changes to `Login`, `SelectProvider`, navigation, or
  provider-neutral domain contracts.
- **R-023**: A disabled provider does not initialize its SDK, validate missing external
  credentials, call the backend, or prevent application startup.
- **R-024**: Enabling provider `X` without valid required configuration fails that login attempt
  with `Вход через X временно недоступен. Обновите приложение или попробуйте позже.` instead of
  a “coming soon” message, where `X` is the injected provider display name.
- **R-025**: Provider-specific SDK types, external credentials, and callbacks remain behind the
  provider adapter. Presentation and domain layers use only provider-neutral identifiers,
  availability, login results, cancellation, and error contracts.

### Login flow and loading state

- **R-026**: After an enabled provider is selected, `SelectProvider` is dismissed first; only then
  does the revealed `Login` enter loading and the provider flow start. `Auth` owns this ordering
  (see R-090).
- **R-027**: During loading, the `ВОЙТИ` label inside the existing button is replaced by the
  design's loading indicator. Button size and shape do not change, and repeated actions are blocked.
- **R-028**: Cancellation and failure always end loading and leave `ВОЙТИ` available.
- **R-029**: User cancellation is not an error: no message appears and no new login flow starts
  automatically.
- **R-030**: A recoverable enabled-provider failure appears once per attempt using the styling and
  position from the design. Connectivity failure shows
  `Нет соединения с интернетом. Проверьте сеть и попробуйте ещё раз.`; any other recoverable
  provider or backend failure shows `Не удалось войти через X. Попробуйте ещё раз.`, where `X`
  is the injected provider display name.
- **R-031**: A cancelled or failed attempt creates no partial or duplicate Yap account.

### Account and provider identity

- **R-032**: The first successful login through any enabled provider automatically creates a Yap
  account without a separate registration step or Yap password.
- **R-033**: Logging in again with the same identity from the same provider resolves the same
  stable Yap account ID. That ID is the ownership key for learning progress, so account continuity
  proves progress continuity without adding progress UI in this iteration.
- **R-034**: Account resolution uses only the unique pair “provider + stable subject”.
  Email-based lookup and account linking are prohibited.
- **R-035**: Identities from different providers are never linked automatically and resolve to
  different stable Yap account IDs, which are independent ownership keys for learning progress.
- **R-036**: A Yap Account stores product-owned fields only. Provider-owned fields — provider,
  stable subject, optional email, optional email-verification state, creation time, and last login
  time — live in the Provider Identity record.
- **R-037**: A previously stored verified email is preserved when a later provider response omits
  email. Display name and avatar are not stored in this iteration.
- **R-038**: Apple login requests and stores email when supplied, including a private relay
  address. Missing email never blocks login.
- **R-039**: Provider access/refresh tokens, authorization codes, and identity tokens are
  transient verification inputs and are not persisted after exchange completion.

### Login security

- **R-040**: Every enabled login attempt begins by requesting a fresh provider-bound challenge
  from the Yap backend. A challenge is never cached or reused across attempts or providers.
- **R-041**: The server stores each challenge with an opaque cryptographically random ID,
  expected provider, provider-specific proof data, creation time, and expiry. For nonce-bound
  providers, store only the nonce hash, never the raw value. For PKCE providers, the stored proof is
  the exact S256 code challenge supplied by the client in the challenge request (see R-096). The
  default TTL is 5 minutes.
- **R-042**: The login request sends the challenge ID and provider credential or proof, but never
  a client-echoed nonce that the server treats as evidence.
- **R-043**: Verify the provider result before opening a database transaction when verification
  requires a network call. Then, in one transaction, lock the challenge, re-check existence,
  expiry, provider, and proof, consume it, resolve or create the identity/account, and create the
  Yap session.
- **R-044**: Consuming a challenge deletes it. A successful challenge is usable exactly once;
  concurrent attempts with one challenge produce exactly one session.
- **R-045**: An expired, mismatched, or missing challenge returns one opaque
  `challenge_invalid` result and creates no account, identity, or session and discloses no
  credentials. A never-issued and already-consumed challenge are indistinguishable to the client.
- **R-046**: Login rejection does not delete an expired row inside a transaction that will roll
  back. A separate periodic cleanup removes expired rows in its own committed transaction.
- **R-047**: Secret material may be generated inside the successful login transaction so its hash
  can be persisted, but it is not disclosed, logged, or returned before commit succeeds. Discard
  it on rollback or commit failure.
- **R-048**: Challenge proof is provider-specific. Google and Apple use a verified nonce-bound
  identity token. The Android Google browser fallback additionally binds a single-use authorization
  code to the client-supplied S256 code challenge stored as the challenge proof. Nonce validation is
  never weakened to accommodate a provider that is not implemented in this iteration.
- **R-049**: Derive identity only from an active, correctly scoped, verified provider result:
  signature or official server verification, issuer, audience, expiry, subject, and challenge
  binding. Local token decoding without verification is not evidence.
- **R-050**: For redirect flows, PKCE, anti-forgery `state`, and challenge binding are three
  separate mandatory controls; none replaces another. A missing or mismatched `state` fails
  locally before the authorization code is sent to the Yap backend.

### Yap session

- **R-051**: Store only Yap session credentials on the device in secure platform storage.
  Credentials never enter screen state, one-shot notifications, logs, analytics, crash
  attributes, or test output.
- **R-052**: Access credentials expire after 15 minutes.
- **R-053**: Refresh credentials expire after 30 days of inactivity and rotate atomically on
  every successful refresh.
- **R-054**: A session expires absolutely 180 days after creation regardless of activity.
- **R-055**: If an authenticated request is rejected because access credentials need refreshing,
  perform at most one silent refresh and one retry of the original request.
- **R-056**: Server-side refresh rotation is atomic and replay-aware: store only hashes; in one
  transaction, lock the session, validate inactivity and absolute expiry, compare the presented
  hash with the current hash, mark the previous hash used, and replace it with the next hash.
- **R-057**: Presenting a previously used refresh credential is a replay signal and revokes the
  whole session. An unknown or malformed value differs from a previously issued value; do not
  infer replay only because a conditional update affected zero rows.
- **R-058**: The client coalesces concurrent refreshes into one in-flight operation, retries a
  rejected request at most once, and atomically persists rotated credentials before making them
  observable.
- **R-059**: Definitive server rejection — expired, revoked, replayed, malformed, or invalid
  refresh credentials — immediately clears the stored session and opens `Login`. Rejected
  credentials are not presented again.
- **R-060**: A transient network or server failure does not clear or invalidate the stored session.
- **R-061**: A valid stored session restores on startup without showing `Login` or contacting
  the provider. `Login` must not flash during restoration.
- **R-062**: On offline startup with a stored session, open the authenticated screen
  provisionally without showing `Login`. Retry silent refresh when connectivity returns or before
  the next authenticated network operation. Definitive rejection clears the session and opens
  `Login`.
- **R-063**: If restoration fails because the session is invalid, show `Login` ready for an
  immediate new attempt.

### Authenticated screen

- **R-064**: After successful login or session restoration, show the current theme background
  with only the text `Успешно авторизован`.
- **R-065**: This screen contains no navigation, controls, progress, loading indicators, or
  placeholders.
- **R-066**: The authenticated screen is not a third authentication-feature screen. After login
  or restoration, the app root replaces the authentication branch with the minimal authenticated
  destination.

### Locale and copy

- **R-067**: Russian is the only supported product locale in this iteration. All user-facing
  labels, loading states, errors, and recovery messages are in Russian.
- **R-068**: Preserve Russian copy from the design verbatim unless a requirement explicitly
  replaces it. Brand names `Google`, `Apple`, `T-ID`, and the design topic `SMALL TALK` remain
  unchanged.
- **R-069**: All repeatable `Login` and `SelectProvider` content — copy, provider labels, icons
  or neutral icon tokens, order, visibility, enabled state, and loading state — is part of screen
  state and is not selected through provider branching in the rendering layer.

### Analytics

- **R-070**: The feature emits events through an injected provider-neutral analytics contract:
  `Login` viewed, provider selected, provider flow started, cancellation, failure with a coarse
  non-secret reason, and success.
- **R-071**: Events are segmented by platform and provider and preserve ordering.
- **R-072**: Events never contain tokens, authorization codes, nonce values, email, provider
  stable subjects, or other personal data.
- **R-073**: The product binding for the analytics contract is a no-op in this iteration. Local
  logging is diagnostic and does not count as analytics delivery; metrics remain unmeasurable
  until a destination is selected.

### Provider implementation constraints

- **R-074**: Android Google login uses Credential Manager's explicit
  `GetSignInWithGoogleOption` button flow with the configured web/server client ID and the current
  backend challenge nonce. It must not use legacy `GoogleSignInOptions`, `DEFAULT_SIGN_IN`, or
  `com.google.android.gms:play-services-auth` as an application login fallback.
- **R-075**: Android may use a system-browser Google authorization-code fallback only when
  Credential Manager is genuinely unavailable or returns an unusable non-cancellation result.
  The fallback uses PKCE, a fresh `state`, the same attempt nonce, and backend code exchange and
  ID-token verification. Explicit cancellation never starts a fallback. Hosting `Activity`,
  redirect/result launcher, and pending callbacks remain inside a lifecycle-aware platform
  adapter and are cleared on cancellation or host destruction.
- **R-076**: Google login on iOS uses the official Google Sign-In SDK with both the iOS and
  backend/server client IDs, the reversed-client-ID URL scheme, and the current challenge nonce.
  The platform adapter returns only the ID token; the Yap backend verifies signature, issuer,
  audience, authorized party when present, expiry, subject, and nonce.
- **R-077**: Apple login on iOS uses native Authentication Services. The Yap backend generates the
  raw challenge nonce, Apple receives `sha256(rawNonce)`, and the verified nonce claim is compared
  with the stored SHA-256 value. The backend also verifies signature, issuer, bundle-ID audience,
  expiry, and subject. Apple remains absent from Android.
- **R-078**: T-ID is not implemented in this iteration. It ships as a disabled scaffold whose
  purpose is to leave the integration point clean and extensible. No T-ID SDK is linked, no T-ID
  network call is made, and no T-ID login can succeed. When the external prerequisites listed in
  Dependencies are met, the intended integration is the official mobile App-to-App SDK with the
  registered non-HTTP(S) `mobile_redirect_uri`, a unique PKCE verifier per attempt, no shipped
  `client_secret`, a backend authorization-code exchange, and a stable subject taken from the
  official userinfo response; the technical `id_token` is not an identity token. Until those
  prerequisites are confirmed, no implementation may invent nonce, JWKS, issuer, audience,
  introspection, PKCE-ownership, or raw-authorization-code contracts that the official public T-ID
  documentation does not define.
- **R-079**: On Android, store the Yap session as an AES-GCM encrypted blob in private app storage
  using a key held in Android Keystore; do not use `EncryptedSharedPreferences`. On iOS, store the
  Yap session in Keychain with a device-only accessibility class.

### Verification contract

- **R-080**: Mapper tests verify exact Russian resources, icons, provider order, stable keys,
  visibility, enabled/loading state, empty-provider state, and both platform configurations.
  `Auth` orchestration tests verify presentation and dismissal of `SelectProvider`,
  dismiss-before-loading ordering, duplicate-action guards across both screens, preservation of
  `Login` state while `SelectProvider` is presented, and that the two screens never communicate
  directly. Screen-level model tests verify disabled-provider news, enabled dispatch, cancellation
  without news, retry, and auth branch replacement.
- **R-081**: Focused Compose UI tests cover only behavior a mapper cannot prove: modal sheet
  presentation over the existing screen, overlay/back dismissal, provider action wiring, loading
  replacement, one-shot snackbar rendering, usable semantics, reduced motion where supported,
  and light/dark rendering.
- **R-082**: Every implemented platform adapter has its own tests — Android Google, iOS Google, and
  iOS Apple — covering attempt preparation, every provider outcome, configuration failure, explicit
  and lifecycle cancellation, redirect handling and `state`, PKCE, nonce/proof binding, attempt
  cleanup, and Android Google fallback triggers. Server verifier tests do not substitute for
  platform adapter tests. The T-ID scaffolds are tested only for scaffold behavior (R-105).
- **R-083**: PostgreSQL invariants — challenge locking/consumption, refresh rotation/replay,
  concurrent first login, unique provider/subject identity, rollback, cleanup, and migrations —
  are verified with real PostgreSQL through Testcontainers using the deployment major version.
  An in-memory fake is not acceptable evidence.
- **R-084**: Physical-device verification is required for the real Google SDK round trip and, when
  its configuration is available, the real Apple round trip. Compilation, simulator execution, or
  mocks do not count as physical end-to-end evidence. Google must pass because it is enabled by
  default. Apple may be reported as externally blocked when its configuration is unavailable, but
  its adapter and contract tests must still pass. T-ID is excluded from physical verification in
  this iteration, and no T-ID round trip may be claimed as passing.
- **R-085**: Acceptance tests prove account continuity through the stable Yap account ID exposed
  by the backend integration test fixture or service result. Same-provider login and restoration
  resolve the same ID; different unlinked providers resolve different IDs. No production progress
  endpoint or progress UI is added for this verification.
- **R-086**: Planning is blocked until `screen_login.dc.html` and `support.js` can be imported and
  their SHA-256 hashes recorded. All visual checks use that frozen baseline; a changed hash returns
  the feature to specification review.

### Authentication structure and orchestration

- **R-087**: The authentication units form this hierarchy: the application root owns `Auth` and the
  minimal authenticated unit; `Auth` owns `Login` and `SelectProvider` as sibling children.
- **R-088**: `Login` neither declares, constructs, presents, nor references `SelectProvider`, its
  configuration, its state, or its navigation. `SelectProvider` never addresses `Login` directly.
  Neither screen holds a reference to the other.
- **R-089**: The two screens communicate only through `Auth`. `Login` emits an "open provider
  selection" output. `SelectProvider` emits either "dismissed" or "provider selected" carrying the
  provider-neutral identifier. After handling the selection, `Auth` drives `Login` only through its
  public event API: `login.dispatch(LoginEvent.ProviderSelected(providerId))`.
- **R-090**: `Auth` owns navigation state, presentation and dismissal of `SelectProvider`,
  and duplicate-action protection while the two screens transition. On "provider selected" it
  dismisses `SelectProvider`, returns to `Login`, and only then dispatches `ProviderSelected` to
  `Login`. `Login` owns the provider attempt, loading state, failure/cancellation handling, and its
  own duplicate-attempt guard.
- **R-091**: `Login` owns only `Login` screen state and rendering. `SelectProvider` owns only the
  provider-list screen state and rendering. Neither owns the relationship between them.
- **R-092**: `Auth` preserves the existing `Login` state while `SelectProvider` is presented and
  after it is dismissed; returning from selection or dismissal never recreates `Login`.
- **R-093**: The modal bottom-sheet presentation over `Login`, including scrim and overlay
  composition, is owned by `Auth`. The navigation primitive holding `SelectProvider` belongs to
  `Auth` and must not be declared inside `Login`.
- **R-094**: The application root owns only the choice between the authentication branch and the
  authenticated branch. It has no knowledge of `Login`, `SelectProvider`, or the navigation between
  them.
- **R-095**: Loading, error, and cancellation feedback for an attempt started through `Auth` is
  presented on `Login`, because `SelectProvider` is already dismissed when the flow starts.

### Provider attempt lifecycle

- **R-096**: A login attempt begins with attempt preparation in the platform adapter, before the
  backend challenge is requested. For PKCE-capable flows, preparation generates a fresh
  `code_verifier` and its S256 `code_challenge`; only the public `code_challenge` leaves the device
  in the challenge request, and the backend stores it verbatim as the challenge proof.
- **R-097**: The `code_verifier` stays ephemeral in the prepared attempt on the device until the
  authorization result is submitted. It is never persisted, logged, or emitted in analytics.
- **R-098**: The login request carries the authorization code together with the `code_verifier`. The
  backend recomputes S256 of the received verifier, compares it with the persisted proof, and
  rejects a mismatch before performing any token exchange.
- **R-099**: A prepared attempt is single-use. Cancellation, failure, and completion all discard it,
  and a discarded or already-submitted attempt can never be reused for another login.
- **R-100**: No `code_verifier`, authorization code, or provider token remains on the device after
  the attempt ends, regardless of its outcome.
- **R-101**: The Android Google browser fallback uses the same prepared attempt model: its PKCE
  proof is bound to the backend challenge even when Credential Manager was attempted first, and it
  never silently starts a second attempt with different proof material.
- **R-102**: Attempt preparation and authentication are expressed by the provider-neutral adapter
  contract. Preparation exposes no SDK type, credential, or platform object to domain or
  presentation code. The PKCE-capable attempt abstraction is generic and is exercised in this
  iteration by the Android Google browser fallback.

### T-ID scaffold

- **R-103**: The T-ID scaffold consists of provider-neutral identifiers and configuration, a visible
  disabled row in `SelectProvider`, Android and iOS adapter shells behind the common provider port,
  callback and result placeholders that expose no SDK type, unregistered server-side verification
  scaffolding, and dependency wiring that never initializes a T-ID SDK.
- **R-104**: A T-ID adapter shell returns a typed "integration not configured" result rather than
  attempting a login. No T-ID SDK dependency is linked, no T-ID network request is issued, and no
  speculative production exchange code exists.
- **R-105**: Selecting the disabled T-ID row initializes no SDK, issues no backend request, enters
  no loading state, and shows `Вход через T-ID скоро появится` exactly once, consistent with R-018.
  Scaffold tests prove this and prove that the absence of T-ID credentials never blocks startup.
- **R-106**: The application builds, starts, and runs every implemented flow with no T-ID
  credentials, configuration, or SDK present. Each scaffold carries an explicit reference to the
  external prerequisites required to replace it.

## Acceptance criteria

### Story 1 — log in with an external provider

- **AC-001**: A user without a valid session sees `Login` when opening Yap.
- **AC-002**: `Login` shows one primary `ВОЙТИ` action and no direct provider actions.
- **AC-003**: Tapping `ВОЙТИ` opens the `SelectProvider` bottom sheet.
- **AC-004**: Tapping outside the sheet closes it and restores unchanged `Login`.
- **AC-005**: The default Android configuration shows Google and T-ID and does not show Apple.
- **AC-006**: The default iOS configuration shows Google, Apple, and T-ID.
- **AC-007**: Google is enabled by default and launches its real account confirmation/login flow.
- **AC-008**: Apple is disabled by default; selecting it closes `SelectProvider` and shows
  `Вход через Apple скоро появится`.
- **AC-009**: T-ID is disabled by default; selecting it closes `SelectProvider` and shows
  `Вход через T-ID скоро появится`.
- **AC-010**: Enabling an implemented, configured provider makes its existing row launch its real
  flow without any other code change. Enabling the scaffolded T-ID provider instead surfaces its
  typed "integration not configured" outcome, which is the message required by AC-030.
- **AC-011**: Disabling Google or another provider keeps its row visible on supported platforms
  and replaces its real flow with that provider's “coming soon” message.
- **AC-012**: The first successful login through an enabled provider creates a Yap account
  without separate registration or a Yap password.
- **AC-013**: Logging in again with the same identity from the same provider resolves the same
  stable Yap account ID in backend integration evidence.
- **AC-014**: Identities from different providers are not linked and resolve different stable Yap
  account IDs in backend integration evidence.
- **AC-015**: Successful login opens the authenticated screen containing only
  `Успешно авторизован` on the current theme background.
- **AC-016**: With no visible provider, the sheet shows its handle, `СПОСОБЫ ВХОДА`, and
  `Нет доступных способов входа`, contains no provider rows, and remains dismissible.

### Story 2 — session restoration

- **AC-017**: On startup, a valid stored session restores without showing the provider flow or
  flashing `Login`.
- **AC-018**: Restoration opens the authenticated screen containing only
  `Успешно авторизован`.
- **AC-019**: The restored session resolves the same stable Yap account ID as the original login;
  this proves continuity for progress owned by that account.
- **AC-020**: An invalid stored session is cleared and `Login` is shown ready for an immediate
  new attempt.
- **AC-021**: A transient network failure does not clear the stored session. Offline startup
  opens the authenticated screen provisionally and retries silent refresh later.
- **AC-022**: Rejection of an authenticated request due to expired access performs exactly one
  silent refresh and exactly one retry of the original request.

### Story 3 — cancellation and failure

- **AC-023**: Cancelling any enabled provider flow shows no error.
- **AC-024**: After cancellation, the user remains on `Login` and can immediately reopen
  `SelectProvider`.
- **AC-025**: An enabled-provider connectivity failure shows
  `Нет соединения с интернетом. Проверьте сеть и попробуйте ещё раз.`; another recoverable
  provider/backend failure shows `Не удалось войти через X. Попробуйте ещё раз.`. Each appears
  once using the design's styling and position.
- **AC-026**: After failure, the user can retry without restarting Yap.
- **AC-027**: Cancellation and failure always end loading.
- **AC-028**: A cancelled or failed attempt creates no partial or duplicate account.
- **AC-029**: Selecting a disabled provider shows `Вход через X скоро появится` using the
  injected display name and starts no SDK, backend request, or loading state.
- **AC-030**: An enabled provider `X` without valid required configuration shows
  `Вход через X временно недоступен. Обновите приложение или попробуйте позже.` rather than a
  “coming soon” message.

### Security and data

- **AC-031**: One challenge is consumed exactly once; two concurrent attempts using it create
  exactly one session.
- **AC-032**: An expired, provider-mismatched, or missing challenge returns one opaque
  `challenge_invalid` result and creates no account, identity, or session.
- **AC-033**: Re-presenting a previously used refresh credential revokes the whole session.
- **AC-034**: Successful refresh rotation is atomic: only hashes are stored, the previous value
  is marked used, and the new value becomes observable only after commit.
- **AC-035**: The pair “provider + stable subject” is unique; concurrent first login using one
  identity creates exactly one account.
- **AC-036**: Provider tokens, authorization codes, and identity tokens are absent from storage
  after exchange completion.
- **AC-037**: Session credentials are absent from screen state, notifications, logs, analytics,
  and test output.
- **AC-038**: A mismatched redirect `state` fails before the authorization code is sent to the
  Yap backend.

### Presentation, locale, and analytics

- **AC-039**: All user-facing `Login` and `SelectProvider` copy, loading states, errors, and
  recovery messages are Russian and match the design verbatim where specified.
- **AC-040**: Configuration order is preserved on screen without sorting by alphabet, enum order,
  or a hardcoded list.
- **AC-041**: Every `SelectProvider` row carries its stable key, provider-neutral identifier,
  display name, icon, order, and enabled state in screen state.
- **AC-042**: Disabled-provider and recoverable-failure messages are delivered as one-shot
  notifications and handled exactly once; cancellation emits no notification.
- **AC-043**: The marquee loops seamlessly with no empty frame or visible reset. With reduced
  motion, movement stops and the text remains visible.
- **AC-044**: During loading, the `ВОЙТИ` label is replaced with the design indicator, button
  size and shape remain unchanged, and repeated actions are blocked.
- **AC-045**: `Login` and `SelectProvider` render correctly in light and dark themes without
  losing content.
- **AC-046**: Analytics emits `Login` view, provider selection, start, cancellation, coarse
  failure, and success in the correct order, segmented by platform/provider and without personal
  data.

### Provider, storage, and verification constraints

- **AC-047**: Android Google tests prove the nonce-bound `GetSignInWithGoogleOption` flow, the
  PKCE/`state`/same-nonce browser fallback only for allowed triggers, no fallback on cancellation,
  and absence of legacy Google Sign-In application APIs.
- **AC-048**: iOS Google and Apple adapter tests prove their required official SDK flows and strict
  backend validation of issuer, audience, subject, expiry, and nonce; Google also verifies
  authorized party when present.
- **AC-049**: Android and iOS T-ID scaffold tests each prove that the shell returns the typed
  "integration not configured" result, links no SDK, initializes nothing, and issues no network
  request. The server-side T-ID verification scaffolding is unregistered, so a login request naming
  T-ID is refused as an unavailable provider. No test claims a working T-ID login.
- **AC-050**: Android persistence evidence shows only an AES-GCM blob outside Keystore and its key
  inside Keystore, with no `EncryptedSharedPreferences`; iOS evidence shows a device-only Keychain
  item.
- **AC-051**: Real PostgreSQL Testcontainers tests pass for every invariant listed in R-083;
  equivalent in-memory tests are not reported as database verification.
- **AC-052**: A physical-device Google round trip passes. The Apple physical round trip either
  passes or is explicitly reported as externally blocked with the missing credential named, while
  its adapter and contract tests still pass. T-ID has no physical round trip in this iteration and
  is reported as deferred, never as passing.
- **AC-053**: `plan.md` records SHA-256 hashes for the imported `screen_login.dc.html` and
  `support.js`, and visual verification uses files with those exact hashes.
- **AC-054**: Same-provider login and session restoration produce the same stable Yap account ID;
  unlinked identities from different providers produce different IDs.

### Component structure and attempt lifecycle

- **AC-055**: A `Login` "open provider selection" output results in `SelectProvider` being presented
  by `Auth`; `Login` performs no presentation itself.
- **AC-056**: A `SelectProvider` "dismissed" output returns to the same `Login` child with unchanged
  state, and `Login` is not recreated.
- **AC-057**: A `SelectProvider` "provider selected" output is delivered to `Auth`, never directly
  to `Login`; after dismissing `SelectProvider`, `Auth` calls
  `login.dispatch(LoginEvent.ProviderSelected(providerId))` exactly once.
- **AC-058**: `SelectProvider` is dismissed before loading starts and before the provider flow is
  launched.
- **AC-059**: Duplicate actions are blocked across the transition: repeated `ВОЙТИ` taps while
  `SelectProvider` is being presented, and repeated provider selections while it is being dismissed,
  produce at most one presentation and one login attempt.
- **AC-060**: Static evidence shows `Login` contains no reference to `SelectProvider` and
  `SelectProvider` contains no reference to `Login`, and that the navigation primitive holding
  `SelectProvider` is declared by `Auth`.
- **AC-061**: The application root exposes only the authentication and authenticated branches, with
  no knowledge of `Login` or `SelectProvider`.
- **AC-062**: For a PKCE flow, the challenge request carries only the S256 code challenge, the
  backend persists that exact value as the challenge proof, and the login request carries the
  authorization code with the `code_verifier`.
- **AC-063**: A `code_verifier` that does not hash to the persisted proof is rejected before any
  token exchange occurs.
- **AC-064**: A prepared attempt is discarded on cancellation, failure, and completion, and reusing
  it fails; no verifier, authorization code, or provider token remains on the device afterwards.

### T-ID scaffold

- **AC-065**: The application builds, starts, and passes the implemented authentication contract
  tests with no T-ID credentials, configuration, SDK dependency, or URL scheme present. The real
  Google round trip is verified separately by AC-052.
- **AC-066**: Selecting the disabled T-ID row shows `Вход через T-ID скоро появится` once and
  performs no SDK initialization, backend request, or loading state.
- **AC-067**: The Android and iOS T-ID adapter shells return the typed "integration not configured"
  result, expose no SDK type through the provider port, and carry an explicit reference to the
  external prerequisites required to replace them.

## Edge cases

- A provider is selected while `SelectProvider` is already closing: execute the action at most once.
- Repeated `ВОЙТИ` taps while opening the sheet or while login is running.
- No account exists for the selected provider on the device: allow adding one within the
  provider flow; this is not an error.
- The system account selector is unavailable or returns a non-cancellation unusable result:
  allow a fallback authorization flow that preserves every mandatory check. Never start fallback
  after explicit user cancellation.
- The provider returns after screen destruction or coroutine cancellation: clear pending handlers
  and do not leave loading stuck.
- The user cancels and immediately retries: use a fresh challenge.
- The challenge expires while the user confirms with the provider.
- Two concurrent login attempts use one challenge or one new identity.
- Apple supplies email only on first authorization and omits it later.
- Apple supplies a private relay address rather than a real address.
- A provider is enabled but required external configuration is missing or invalid.
- A provider is disabled and its external credentials are absent: application startup still works.
- No provider is visible on the current platform.
- Multiple requests refresh the session concurrently.
- Definitive refresh rejection versus refresh failure caused by a transient network error.
- Absolute session expiry occurs while the refresh credential is otherwise valid.
- Offline startup with a stored session followed by definitive refresh rejection.
- A previously rotated refresh credential is presented again.
- The app opens in system dark theme with reduced motion enabled.

## Assumptions

- The feature slug is `001-authentication` and artifacts live in
  `specs/001-authentication/`. No earlier pass exists in that directory; this specification
  derives only from `specify_prompt.md`, the design, and repository rules.
- Modules `apps/mobile/feature-auth`, `services/server/feature-auth`, and
  `shared/contract/auth` exist but are empty; the repository has no login behavior to preserve.
- Default provider display names are `Google`, `Apple`, and `T-ID`. The “coming soon” message
  uses the injected name rather than a fixed string list.
- The design does not define exact Russian error copy. This specification therefore defines the
  configuration, connectivity, and generic recoverable-failure messages that planning and tests
  must use verbatim.
- This iteration defines no numeric conversion or login-time targets. It establishes a
  measurement baseline, and missing targets do not block delivery.
- Product metrics — conversion from `Login` view to success, cancellation rate, failure rate,
  restored-session startup rate, and view-to-success time — derive from analytics-contract events
  and remain unavailable until a destination is selected.
- Physical verification of the real Apple SDK exchange may remain blocked by missing external
  configuration. In that case, record the exact blocker rather than claiming the round trip passed,
  while still implementing the adapter and its contract tests completely. Google is enabled by
  default and its physical round trip is not optional.
- T-ID is deliberately deferred. Its partner credentials, private documentation, exact SDK versions,
  callback APIs, PKCE ownership, token-exchange mechanics, and minimal userinfo scope are external
  prerequisites for a later iteration. They are not blockers for this one: Google, Apple, the
  authentication component hierarchy, backend sessions, secure storage, and the UI all proceed
  independently, and the scaffold is what makes that separation explicit.

## Dependencies

- Claude Design project `0c49e08b-d7ab-4cd3-88be-8483024790e5`
  (`screen_login.dc.html` and `support.js`) as the visual source of truth, accessed through the
  `claude_design` MCP. Planning requires an importable snapshot and recorded SHA-256 hashes; lack
  of access blocks planning rather than allowing visual requirements to drift.
- Google OAuth client configuration for Android, iOS, and the server client, plus access to
  Google's signature-verification keys.
- Sign in with Apple configuration and the iOS application bundle identifier.
- **Not required by this iteration**, and listed as the external prerequisites for replacing the
  T-ID scaffold later: the official T-ID mobile App-to-App SDK for Android and iOS with confirmed
  versions and callback APIs, partner application registration, a registered non-HTTP(S)
  `mobile_redirect_uri`, backend-held `client_id`/`client_secret` for the documented token endpoint,
  the minimal userinfo scope that returns `sub`, partner-only documentation stating whether the SDK
  accepts a caller-supplied `code_verifier`/`code_challenge` or exposes the raw authorization code,
  and partner test accounts for physical verification. `plan.md` records these as deferred
  integration questions; none of them blocks the work in this iteration.
- Yap backend PostgreSQL storage and migrations for challenges, accounts, provider identities,
  and sessions.
- Current official provider and SDK documentation as the primary source for flows, token shapes,
  and verification methods during planning.
