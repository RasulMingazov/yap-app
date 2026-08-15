# Feature Specification: Login Screen

**Feature Branch**: `feature/001-login-screen` | **Feature ID**: `001-login-screen`

**Created**: 2026-08-13 | **Refreshed**: 2026-08-15 against the implemented code

**Status**: Delivered. Release to users still waits on two configuration facts — both legal
destinations set (FR-051) — and release to iOS users on the Sign in with Apple follow-up.

**Input**:

- Notion product page "001. Авторизация"
  (https://app.notion.com/p/001-3abdc7d20da48100bafeec7b2ace8de2)
- The Claude Design project screen `screen_login.dc.html`
  (https://claude.ai/design/p/0c49e08b-d7ab-4cd3-88be-8483024790e5)

This document is the single specification for the authentication slice. Two follow-ups that were
once specified separately — the rendering-package split with the repository-wide comment cleanup,
and the provider architecture with the selection destination, shared theme, snackbar, and data
layer — are part of it and are delivered.

## User Scenarios & Testing *(mandatory)*

Two audiences are served. A person signing in must reach the main screen in one tap with no
password, on a screen whose colours, motion, and messages behave. A contributor adding the next
provider must be able to do so without touching the login screen or its state holder.

### User Story 1 - First login without registration (Priority: P1)

A person opens Yap for the first time, taps the single primary action, chooses Google from the
provider selection screen, confirms the account with Google, and arrives on the main screen. They
are never asked to invent a password, fill a registration form, or supply a profile.

**Independent Test**: Install fresh, complete the Google confirmation, observe arrival on the main
screen with an account that exists.

**Acceptance Scenarios**:

1. **Given** a fresh install with no session, **When** the app launches, **Then** the login screen
   is shown.
2. **Given** the login screen, **When** the primary login action is activated, **Then** the
   provider selection screen opens.
3. **Given** the selection screen on Android, **When** the user reads it, **Then** Google and T-ID
   are listed and Apple is not present; on iOS all three are listed.
4. **Given** a Google account never used with Yap, **When** it is confirmed, **Then** a Yap account
   is created with no further registration step and the user reaches the main screen.
5. **Given** the selection screen, **When** Apple or T-ID is chosen, **Then** the screen closes, a
   message states that logging in through that provider is coming soon, no login is attempted, and
   Google remains choosable.
6. **Given** a device with no Google services, **When** Google is chosen, **Then** confirmation
   opens in the system browser without the user asking for it, and success reaches the main screen
   exactly as on any other device.

---

### User Story 2 - Returning with the same provider account (Priority: P2)

Someone who logged in before — after reinstalling, or after their session ended — logs in again
with the same Google account and finds their existing Yap account, streak, and lesson history.

**Independent Test**: Log in, generate progress, clear the session, log in again with the same
Google account, confirm the progress is still there.

**Acceptance Scenarios**:

1. **Given** a Google account already mapped to a Yap account, **When** it logs in again, **Then**
   the same Yap account opens with its learning progress preserved.
2. **Given** two different Google accounts, **When** each logs in, **Then** each opens its own Yap
   account with its own progress, and the two are never merged.

---

### User Story 3 - Session restored on relaunch (Priority: P2)

A returning user opens the app and goes straight to the main screen, without the login screen and
without confirming anything with a provider again.

**Independent Test**: Log in, close the app fully, reopen it, confirm the main screen appears.

**Acceptance Scenarios**:

1. **Given** a stored session still inside its window, **When** the app launches, **Then** the main
   screen is reached without the login screen and without provider confirmation, with or without a
   network connection.
2. **Given** the session has become invalid, **When** the app determines this, **Then** the user is
   returned to the login screen and can log in again.
3. **Given** a stored session, **When** the app is launched repeatedly, **Then** the login screen
   never appears, even briefly, between the splash screen and the main screen.
4. **Given** a stored session past its expiry, **When** the app launches, **Then** the login screen
   is shown directly, without the main screen appearing first and without any network request.

---

### User Story 4 - Cancelling or failing, then retrying (Priority: P3)

A user starts a login, changes their mind at Google's screen or hits a failure, and lands back on
the login screen able to try again without restarting the app.

**Independent Test**: Cancel at Google's confirmation screen, then start a second attempt and
complete it.

**Acceptance Scenarios**:

1. **Given** an attempt in progress, **When** the user cancels at the provider, **Then** the login
   screen returns to idle with no error shown and login remains available.
2. **Given** an attempt in progress, **When** it fails, **Then** a plain-language message is shown
   and login remains available.
3. **Given** the selection screen, **When** it is dismissed without a choice, **Then** nothing is
   treated as an error and the login screen stays idle.
4. **Given** an attempt in progress, **When** the primary action or a provider is activated again,
   **Then** no second concurrent attempt starts.
5. **Given** an attempt in progress, **When** the user leaves the app and returns much later
   without it having concluded, **Then** the login screen is idle, no error is shown, and login is
   available again.

---

### User Story 5 - One way in, whichever provider (Priority: P1)

The login screen's state holder asks a single login entry point for the chosen provider. It never
holds a collection of per-provider logins and never learns which providers exist in code.

**Independent Test**: Sign in with Google end to end, then register a second handler and confirm it
runs without a single edit to the login state holder.

**Acceptance Scenarios**:

1. **Given** a provider with a registered handler, **When** it is chosen, **Then** the login runs
   and its outcome — success, cancellation, failure — is reported.
2. **Given** the login state holder, **When** its dependencies are read, **Then** there is no
   collection keyed by provider and no reference to a provider catalogue.
3. **Given** a provider whose handler is not registered, **When** it is chosen, **Then** the person
   is told it is not yet available and no login is attempted.
4. **Given** a new provider with a handler, **When** it is registered, **Then** it is selectable
   with no change to the login state holder or the login screen.
5. **Given** a login already running, **When** the primary action or a provider is chosen again,
   **Then** no second login starts.

---

### User Story 6 - Providers are decided in one place (Priority: P1)

Which providers appear, and which of them can be picked, is decided by one observable source in the
domain layer. Presentation subscribes to it. Nothing in the UI hardcodes the roster, and the source
can later be fed by the backend with no presentation change.

**Independent Test**: Change the source so a provider becomes hidden or unselectable and confirm
the selection surface reflects it without any UI edit; verify no UI resource is reachable from the
domain or data layer.

**Acceptance Scenarios**:

1. **Given** the running app, **When** the roster is requested, **Then** it arrives as an
   observable stream and the surface re-renders when it changes.
2. **Given** a provider hidden for the current platform, **When** the roster is observed, **Then**
   that provider is absent from what the person sees.
3. **Given** a provider shown but not selectable, **When** it is chosen, **Then** no provider login
   runs and the person is told it is not yet available.
4. **Given** the domain and data layers, **When** their sources are read, **Then** no icon, label,
   or other UI resource appears in them.

---

### User Story 7 - Choosing a provider is its own screen (Priority: P2)

The provider list is a destination of its own — a bottom sheet with its own state, mapping, and
state holder. It shows each provider's name and mark, reflects availability, and hands the chosen
provider back to the login screen. The login screen's state holder keeps none of this.

**Independent Test**: Open the selection destination, choose a provider, confirm the login screen
receives the choice and starts the login; confirm the login state holder exposes no provider list.

**Acceptance Scenarios**:

1. **Given** the login screen, **When** the primary action is used, **Then** the selection screen
   opens as its own destination.
2. **Given** the selection screen, **When** a provider is chosen, **Then** the screen closes and
   the chosen provider reaches the login screen, which starts the login.
3. **Given** the selection screen, **When** it is dismissed by back gesture, system back, swipe, or
   scrim, **Then** no provider is returned and no login starts.
4. **Given** a rendered provider row, **When** it is read, **Then** its name and mark come from
   presentation's own mapping of the provider identity, not from the domain layer, and both match
   the design.
5. **Given** the login screen's state, **When** it is inspected, **Then** it carries no provider
   list and no provider-row model.

---

### User Story 8 - One colour language, no literals on the screen (Priority: P2)

The colours the login screen uses are part of the app theme under names that say what they are for.
No colour literal is written in a UI component. The transient message carries the same colour in
dark theme as in light.

**Independent Test**: Render the login screen in both themes and confirm the only difference from
the previous light-theme appearance is the message colour in dark theme.

**Acceptance Scenarios**:

1. **Given** any login UI component, **When** it is read, **Then** it declares no colour literal
   and reads every colour from the theme.
2. **Given** the theme, **When** a colour is looked up, **Then** its name states its role —
   surface, accent, caption, message background — not a product or screen name.
3. **Given** the transient message in dark theme, **When** it is shown, **Then** its background and
   text colours equal the light-theme values.

---

### User Story 9 - The message leaves the way it should (Priority: P3)

A transient message appears, waits, and leaves by moving upward off the screen rather than only
fading. Several messages in a row are shown one after another without overlapping or being lost.

**Independent Test**: Trigger a failure twice in quick succession and observe both messages in
order, each auto-dismissing with upward motion.

**Acceptance Scenarios**:

1. **Given** a message on screen, **When** its display time elapses, **Then** it leaves by moving
   upward out of view, not by fading in place.
2. **Given** a message on screen, **When** a second is raised, **Then** the first finishes and the
   second follows; neither is silently dropped.
3. **Given** the platform's standard message mechanism, **When** the screen shows a message,
   **Then** that mechanism owns queueing, timing, and dismissal instead of hand-rolled timers.
4. **Given** reduced motion is requested, **When** a message is dismissed, **Then** it disappears
   without motion and stays readable for its full duration.

---

### Edge Cases

**Login and provider**

- The user leaves the app mid-confirmation and returns much later — the screen must not be stuck
  showing progress forever.
- The device is offline when Google is chosen — a message the user can act on, not a silent stall.
- The device has no Google services at all (a de-Googled build). The browser fallback is what makes
  it usable.
- The device has neither a Google credential provider nor a browser able to handle confirmation.
- The user dismisses the browser tab instead of cancelling inside Google's page.
- The provider returns success but withholds the stable account identifier.
- A login request reaches the server without passing through the app, carrying a fabricated
  confirmation.
- The user taps a provider twice, or two providers, in rapid succession.
- A provider is listed but has no registered handler — reported as not yet available at the moment
  of choice, never a crash.

**Session**

- The session becomes invalid while the user is in the foreground — nothing in this feature makes
  an authenticated request, so the app learns of it at its next launch refresh (FR-028).
- The server is unreachable for a long stretch — the user stays logged in throughout.
- The same account logs in on a second device — the first device's session is already dead and
  finds out at its next launch refresh.
- The user opens the app regularly but always offline, so nothing renews the session and it reaches
  its expiry despite the app being in use.
- The device clock is wrong, so a live session looks expired locally or a dead one looks live.
- Many devices share one outbound IP address and collectively approach the rate limit.

**Selection screen and presentation**

- The roster changes while the selection screen is open — the list updates in place; a provider
  already mid-login is unaffected.
- The selection screen is left by system back during an in-flight login — the login continues and
  its outcome is still reported on the login screen.
- Configuration change while the selection screen is open — it survives, and no login starts twice.
  Process death is deliberately different: the app returns to the login screen with the chooser
  closed.
- A message would be raised while the selection screen covers the login screen — unreachable by
  construction: the sheet closes before any login runs.
- Theme changes while a message is on screen — it stays legible and keeps the one shared value.
- 320 dp of width and 200% font scale at once — heading, rotating topic, and legal line compete for
  the same vertical space.
- The provider reports a different email, display name, or avatar for a known account, or omits
  them.
- The terms or privacy document is unreachable when its link is followed.

## Requirements *(mandatory)*

Numbers are stable across revisions. Gaps (FR-014, FR-057, FR-059 … FR-061, SC-022, SC-026) are
one-time migration requirements that have been met and retired; the rules that survive them are
FR-058 and FR-062.

### Functional Requirements

#### Access and login surface

- **FR-001**: The app MUST show the login screen whenever there is no stored session, and MUST NOT
  show it when there is one.
- **FR-002**: While the app is still determining whether a session is stored, it MUST show the
  platform's own splash screen and neither the login nor the main screen. A user with a stored
  session MUST never see the login screen appear and disappear.
- **FR-003**: The login screen MUST present exactly one primary login action, which opens the
  provider selection destination rather than starting a specific provider's login.
- **FR-004**: Provider selection MUST show Google and T-ID on both platforms, and Apple on iOS
  only. Apple MUST NOT be presented on Android in any state.
- **FR-005**: Google MUST be a working provider on both platforms. Apple and T-ID MUST be listed
  but not functional: choosing either MUST state plainly that logging in through that provider is
  coming soon, MUST NOT begin a login attempt, and MUST NOT be reported as a failure. The wording
  MUST name the chosen provider without any screen gaining a per-provider branch (FR-038).
- **FR-006**: FR-004 and FR-005 MUST be expressed as two facts carried by each provider — whether
  it is shown, and whether choosing it starts a real login — rather than as decisions rediscovered
  wherever providers are listed, drawn, or chosen. Both MUST be runtime values rather than
  constants, so a later remote source can set them. The two are independent: a provider MAY be
  usable while hidden, or shown while unusable. A provider that is not shown MUST NOT be reachable
  by any means. Turning a provider on later MUST be exactly a change to those two facts plus the
  login path it runs.
- **FR-007**: The provider roster MUST be exposed as an observable stream owned outside
  presentation, so a later remote source can replace the local one without presentation changes.
  Platform-specific availability MUST be applied by that source, never by the UI, and the roster
  MUST emit every known provider in display order with both facts already decided.
- **FR-008**: The login screen's state holder MUST depend on exactly one login entry point that
  takes the chosen provider as its argument.
- **FR-009**: The system MUST NOT expose any collection of per-provider logins to presentation; the
  mapping from provider to login path lives in the domain or data layer.
- **FR-010**: The system MUST offer a named registration point where a provider's login handler is
  declared, so adding a provider is a registration rather than an edit to existing branching.
- **FR-011**: Adding a provider MUST NOT require changes to the login screen or its state holder.
- **FR-012**: Choosing a provider with no registered handler, or one the roster marks not
  selectable, MUST report unavailability and start no login. Selectability is a login-time check,
  not only a wiring fact.
- **FR-013**: The domain and data layers MUST NOT reference UI resources — icons, labels, colours.
- **FR-015**: Choosing Google MUST hand the user to Google's own account confirmation and return
  them to Yap when it concludes, in either outcome.
- **FR-016**: Google login MUST NOT depend on the device having Google's services installed. The
  app MUST first attempt the device's native Google credential flow and, when that flow is
  unavailable, MUST fall back to confirming the account in the system browser without the user
  choosing between them or being told which ran. Both paths MUST produce the same account, session,
  and outcomes. Confirmation MUST NOT be presented inside an embedded web view. When neither path
  is available, the attempt MUST end as an ordinary recoverable failure (FR-030).

#### Account resolution

- **FR-017**: The system MUST establish for itself that a login confirmation genuinely comes from
  the provider and names the account it claims. That check MUST happen on the server, independently
  of the app. A confirmation that cannot be verified — including one that verifies but carries no
  stable account identifier (FR-021) — MUST NOT create or resolve an account, MUST NOT issue a
  session, and MUST end as an ordinary recoverable failure (FR-030). This requirement owns that
  rule; others refer to it.
- **FR-018**: On the first successful confirmation of a provider account, the system MUST create a
  Yap account with no additional registration step, password, or profile input.
- **FR-019**: A repeat login with the same provider account MUST resolve to the same Yap account,
  with its learning progress intact.
- **FR-020**: Accounts from different providers MUST NOT be linked automatically. Each provider
  account maps to its own Yap account with independent progress.
- **FR-021**: The system MUST store the provider's stable account identifier together with the
  email address, display name, and avatar reference the provider reports, refreshing all of them on
  each successful login. Every field except the identifier is descriptive: none MUST be used to
  find, match, merge, or link accounts, so two providers reporting the same address still produce
  two independent Yap accounts (FR-020). An omitted descriptive field MUST be stored as absent and
  MUST NOT block login. The identifier itself is required; a confirmation without it is refused
  under FR-017.
- **FR-022**: A successful login MUST take the user to the main screen.

#### Session

- **FR-023**: A successful login MUST establish a session that survives closing and reopening the
  app.
- **FR-024**: On launch with a stored session that has not passed its own expiry, the app MUST
  reach the main screen without showing the login screen and without re-confirming with a provider.
  The decision MUST be made from stored state alone — no request before deciding — so an offline
  launch still reaches the main screen. The device MUST store the session's expiry alongside the
  session and MUST treat a stored session already past it as no session at all. The stored expiry
  is a courtesy check, not the authority: a revoked or superseded session inside its window is
  discovered at the first exchange with the server (FR-028) and handled by FR-025.
- **FR-025**: When the session becomes invalid, the app MUST return the user to the login screen. A
  session becomes invalid only when the server explicitly rejects it or when the locally stored
  expiry has passed. A request that fails because the server could not be reached — no network, a
  timeout, an unavailable server, or a rate-limit rejection — MUST leave the session intact and be
  retried later. The login screen MUST look identical whatever made the session invalid, and the
  system MUST NOT report which of them happened. This requirement owns what does and does not end a
  session; FR-026, FR-028, and SC-014 refer to it.
- **FR-026**: A session MUST expire 90 days after its last renewal, and every renewal MUST reset
  that window. Only a successful exchange with the server renews a session: a launch that never
  reaches the server MUST NOT move the expiry, and the device MUST NOT extend the window on its
  own.
- **FR-027**: A Yap account MUST hold at most one valid session at a time. A successful login MUST
  invalidate every session previously established for that account, so a user who logs in on a
  second device is logged out on the first and returns to its login screen at its next exchange
  with the server (FR-025, FR-028).
- **FR-028**: The app MUST attempt to refresh a stored session with the server on launch, so a user
  who keeps opening the app keeps their session alive (FR-026). The attempt MUST happen only
  **after** the launch decision has been made from stored state, so it never delays or gates
  reaching the main screen. It MUST be made when the stored access token has expired or is **within
  five minutes** of expiring, and skipped otherwise, so an app opened many times a day refreshes at
  most once per token lifetime. Its three outcomes are the ones FR-025 fixes: a success moves the
  expiry, an explicit rejection returns the user to the login screen, and an unreachable server
  leaves the stored session as it is. This is the only thing in this feature that reaches the
  server after login.

#### Cancellation, failure, and retry

- **FR-029**: Cancellation at the provider MUST return the login screen to idle with no error
  message and MUST leave login available. An attempt that concludes with neither a success nor an
  explicit failure MUST be treated the same way, whether the user dismissed the browser tab, left
  the app mid-confirmation, or nothing ever came back. No attempt MUST be left showing progress
  indefinitely: **an attempt still unresolved 60 seconds after it started MUST be ended and treated
  as a cancellation**, for every provider.
- **FR-030**: A failed login MUST show a plain-language message that exposes no provider or
  transport internals, and MUST leave login available for another attempt.
- **FR-031**: While an attempt is in progress the primary action MUST indicate progress and MUST
  NOT start a second concurrent attempt. The same guard MUST cover provider choice.
- **FR-032**: Dismissing the provider selection destination without choosing MUST be neither an
  error nor an attempt, whether the dismissal came from system back, a gesture, a swipe, or the
  scrim.
- **FR-033**: Every unauthenticated login endpoint MUST rate limit requests per originating IP and
  reject requests over the threshold. **The threshold is 100 requests per minute per IP.** A
  rejected request MUST reach the user as an ordinary recoverable failure (FR-030) and MUST leave
  login available once the limit clears. The threshold is sized so an office or carrier NAT never
  reaches it: a coarse guard against automated abuse, not a per-user fairness control.

#### Presentation

- **FR-034**: The login screen MUST present, top to bottom: the scrolling promotional band ("ВХОД
  ЗА 1 ТАП ✦ БЕЗ ПАРОЛЕЙ"), the hero heading "БОЛТАЙ БЕЗ СТРАХА", a rotating topic word cycling
  through "SMALL TALK", "ОТКАЗЫ", "ЗНАКОМСТВА", the supporting body copy, the primary action
  labelled "ВОЙТИ", the caption "Аккаунт нужен для стрика и истории занятий.", and the legal line
  of FR-051.
- **FR-035**: Provider selection MUST be its own navigable destination presented as a bottom sheet,
  not a visibility flag inside the login screen's state.
- **FR-036**: That destination MUST own its state, its mapping to rendered rows, and its state
  holder.
- **FR-037**: The selection destination MUST be headed "Способы входа", MUST identify each provider
  by its own name and mark from the design, and MUST be dismissible by tapping outside it, by
  swipe, and by system back.
- **FR-038**: A single table MUST be the only place where a provider identity becomes a name, a
  mark, and other display data; every screen that needs any of it reads that table.
- **FR-039**: The destination MUST return the chosen provider to the login screen, which performs
  the login; dismissal without a choice MUST return nothing.
- **FR-040**: The login screen's state MUST NOT contain a provider list or provider-row model.
- **FR-041**: The screen MUST render correctly in both light and dark themes, following the colour
  roles defined in the design.
- **FR-042**: Every colour the login screen uses MUST be defined in the shared app theme under a
  role-based name stating purpose rather than a screen or product name.
- **FR-043**: No login UI component MAY declare a colour literal. Brand colours belong inside the
  provider marks, never in the theme.
- **FR-044**: The transient message's background and text colours MUST be identical in light and
  dark themes. Roles the screen did not previously express — the sheet's scrim, border, and drag
  indicator — are taken from the design.
- **FR-045**: When the operating system reports a reduced-motion preference, decorative animation
  MUST NOT play and all content MUST remain readable at rest. The preference governs the screen's
  decorative motion and the transient message; the selection destination keeps the platform's
  standard sheet animation.
- **FR-046**: The layout MUST adapt to narrow widths and system font scaling without clipping or
  overlap, and MUST respect system insets. The guaranteed range is **down to 320 dp of width and up
  to 200% font scale**, including both at once; content that no longer fits MUST scroll.
- **FR-047**: Every interactive element MUST be reachable and operable by assistive technology,
  with a spoken name identifying which provider it starts.
- **FR-048**: Transient messages — failures and the coming-soon notice — MUST be presented through
  the platform's standard message state holder, which owns queueing, one-at-a-time display, and
  dismissal. They MUST NOT block interaction with the screen beneath. Display duration and motion
  are applied by the screen's own host, because the standard renderer exposes neither.
- **FR-049**: A dismissing message MUST leave by upward motion rather than opacity alone, and MUST
  respect a reduced-motion preference.
- **FR-050**: Consecutive messages MUST be shown in order without overlap and without silent loss.
- **FR-051**: The login screen MUST show a legal line beneath the primary action stating that
  continuing accepts the terms of service and the privacy policy, each reachable as its own link.
  The line MUST be legible, MUST NOT require a checkbox or an extra tap before logging in, and MUST
  be present on both platforms. Each destination MUST be a configured value. While a destination is
  unset its link MUST still render, MUST NOT navigate, and MUST NOT report a failure; the app MUST
  NOT be released while either is unset. Following a configured link hands the address to the
  system browser; what the browser then shows is outside Yap's surface.

#### Source structure and hygiene

- **FR-052**: Each presentation slice MUST expose a dedicated rendering package nested under it.
- **FR-053**: Every declaration whose purpose is to draw or style a screen MUST live in that
  screen's rendering package, including the screen itself, its message host, its legal line, and
  the tags naming its rendered elements.
- **FR-054**: Declarations that produce or describe state without drawing — view models and state
  mappers — MUST stay outside the rendering package.
- **FR-055**: Tests follow their subjects: rendering tests into the matching test package, state
  tests beside the state code.
- **FR-056**: No declaration's visibility may widen beyond its module for the sake of placement.
- **FR-058**: Kotlin sources MUST carry no explanatory comment or documentation block — mobile,
  server, shared contracts, and build logic, tests included. Comments the toolchain acts on
  (suppression directives, generated-file markers, license headers) are preserved; none exists
  today. Comments in non-Kotlin files — documents, build scripts, configuration, resources — are
  out of scope.
- **FR-062**: A statement in a project guide that this feature's code contradicts MUST be
  reconciled in the same change, never worked around silently.

#### Data layer

- **FR-063**: The auth data layer MUST stay provider-neutral above the credential adapter: the
  session lifecycle (observe, refresh, store, forget) and a provider's login path MUST be separate
  repositories, so a second provider adds a repository rather than a method to a shared one.
- **FR-064**: HTTP outcomes MUST be typed once, in the shared network module, and consumed as
  values — refused, unauthorized, unavailable, malformed — so no feature re-derives them from
  status codes and the sign-out decision of FR-025 is made in one place.
- **FR-065**: Any data-layer change MUST preserve login, session refresh, and access-token
  behaviour, proven by the existing data and domain tests.

#### Verification

- **FR-066**: Behaviour changes MUST be introduced test-first, and the repository build —
  compilation, tests, and static analysis — MUST pass, with the iOS target compiled for any change
  crossing the multiplatform boundary.

### Key Entities

- **User Account**: A person's identity inside Yap and the owner of their learning progress.
  Created on first successful login with a provider account.
- **Provider Identity**: The reference to an account held at an external provider. Carries the
  provider's stable account identifier — required, and the only thing accounts are matched by —
  plus the email address, display name, and avatar reference; everything but the identifier is
  descriptive and never used to match. Belongs to exactly one User Account.
- **Session**: The state that lets a returning user reach the main screen without re-confirming.
  Belongs to one User Account, survives restart, and carries an expiry that moves forward on every
  renewal — and only a successful exchange with the server renews it (FR-026). The expiry is stored
  on the device beside the session so the app can tell a dead session from a live one without the
  network. A User Account holds at most one valid Session (FR-027).
- **Auth provider**: The identity of a sign-in method, carrying whether it is shown and whether it
  can be chosen. Both are set by the roster; neither is a constant. Carries no display data and no
  platform rules.
- **Roster**: The observable source producing every provider in display order with those two facts
  decided; the unit presentation subscribes to.
- **Login handler registration**: The declared association between a provider and the login path
  serving it, read by the single login entry point.
- **Login outcome**: Success, cancellation, failure, or unavailability of an attempt. The last is
  how a provider with no login path, or one the roster marks unselectable, is reported.
- **Provider row**: What the selection screen draws — name, mark, availability — built from the one
  provider-to-display-data table.
- **Transient message**: A short, self-dismissing notice raised by the login screen.

## Success Criteria *(mandatory)*

- **SC-001**: A new user reaches the main screen from the login screen in under 15 seconds of their
  own effort, excluding time inside Google's screens. Analytics is out of scope, so this is
  observed by hand during release verification.
- **SC-002**: 100% of returning logins with a previously used provider account open the same Yap
  account with streak and lesson history intact.
- **SC-003**: 100% of launches holding a stored session inside its window reach the main screen
  without the login screen appearing, including launches with no network.
- **SC-004**: On Android, Apple appears in zero provider lists; on iOS all three providers appear.
- **SC-005**: 100% of cancellations and failures leave the user able to start another login without
  restarting the app.
- **SC-006**: Choosing Apple or T-ID never leaves the screen in a progress state and never produces
  an error message — only the coming-soon notice, which names the provider chosen.
- **SC-007**: No attempt leaves the screen in a progress state indefinitely; every attempt resolves
  to success, an idle screen, or a failure message within 60 seconds. An attempt concluding without
  either a success or an explicit failure resolves to idle, never to a failure message.
- **SC-008**: The screen presents its full content without clipping or overlap in both themes, at
  320 dp of width, and at 200% font scale — including both extremes together.
- **SC-009**: A user who opens the app with a network at least once every 90 days is never asked to
  log in again on that device; a session whose last successful exchange was more than 90 days ago
  always requires a new login. Launches that never reach the server do not count.
- **SC-010**: Neither a person retrying by hand nor a full office or carrier NAT of ordinary users
  triggers the rate limit, while a client sending more than 100 requests a minute from one address
  is rejected in 100% of cases.
- **SC-011**: The legal line and both links are visible on every launch, on both platforms, in both
  themes, across the whole 320 dp / 200% range, without adding a step to logging in. With both
  destinations configured each link opens its document in 100% of taps; with one unset, its link
  never navigates and never shows an error, and no build in that state reaches users.
- **SC-012**: A device with no Google services completes Google login successfully, reaching the
  same account and session it would on a device that has them.
- **SC-013**: After a login on a second device, the first returns to the login screen at its next
  launch refresh in 100% of cases, and no account ever holds two valid sessions.
- **SC-014**: A device that cannot reach the server — offline, timing out, refused by the rate
  limit, or facing an unavailable server — is never returned to the login screen for that reason
  alone; only an explicit rejection or a passed local expiry does that.
- **SC-015**: A request carrying a forged or unverifiable provider confirmation creates no account
  and yields no session in 100% of cases, whether or not it came from the app.
- **SC-016**: Every provider behaves correctly under all four combinations of its two facts, and
  switching a provider between them, or adding one, requires zero edits to the login screen and its
  state holder.
- **SC-017**: Zero colour literals remain in login UI components and in the shared sheet chrome.
- **SC-018**: The transient message renders with identical colour values in light and dark themes.
- **SC-019**: Every message dismissal ends with upward motion; none ends by fade alone, except
  under reduced motion, where no motion is expected.
- **SC-020**: The login state holder references no provider collection, catalogue, or provider-row
  type; provider concerns live entirely in the selection destination and the domain source.
- **SC-021**: A person goes from the login screen to a completed Google sign-in in two taps.
- **SC-023**: Zero explanatory comment lines remain in any Kotlin source.
- **SC-024**: 100% of login declarations that draw or style a screen sit in that screen's rendering
  package, and 0% of state-producing declarations do.
- **SC-025**: No test is skipped, renamed in intent, or weakened by a structural change, and the
  repository build passes on the branch.
- **SC-027**: A contributor can find the code that draws a login screen from the package name
  alone, without opening a file.

## Assumptions

- Only Google login is delivered working. Apple and T-ID appear where visible, each marked not
  selectable (FR-006); making one work is a change to its two facts plus its login path. Apple's
  review rules require an app offering Google login to also offer Sign in with Apple, so the iOS
  build is not submitted to the App Store until that follow-up lands. iOS is built, run, and tested
  here; only release waits.
- The provider set stays Apple, Google, T-ID. The roster emits every known provider in display
  order; the selection screen's mapping drops the ones marked not shown.
- The browser fallback of FR-016 matters on Android, where the native flow requires Google's
  services. On iOS the platform's own Google login already runs in the system browser.
- Returning the chosen provider uses the navigation library's own result mechanism rather than a
  project-specific carrier, which is why the navigation dependency sits on a pre-release.
- A roster with no visible providers is out of scope: the local roster always offers Google. It
  becomes reachable only when the roster is fed remotely.
- Transient messages stay anchored at the top of the screen, which is what makes upward exit read
  as leaving.
- The dark-theme message colour adopts the light-theme value the design has always specified.
- Provider marks come from the design: the multicolour Google mark, the Apple mark drawn in the
  row's text colour, and a yellow rounded badge for T-ID. Because they are not uniform, the row
  records which kind it is and the screen renders on that rather than on the provider's identity.
- The design is the source for the sheet's chrome and the message's placement, shape, timing, and
  motion; values are lifted rather than invented.
- Recording login outcomes and rate-limit rejections is out of scope: the server keeps only what
  its framework logs by default. The consequence is accepted — the FR-033 threshold cannot be tuned
  from evidence until a later feature adds that record.
- Analytics is out of scope. The metrics on the Notion page remain a product goal for when the app
  has an analytics capability.
- All user-facing copy is Russian, matching the design. No other locale is in scope.
- The main screen reached after login is a placeholder destination; the home feature is not part of
  this work. Because no other feature exists yet, the launch refresh of FR-028 is the only thing
  contacting the server after login.
- Logging out, deleting an account, editing a profile, and linking two provider accounts are out of
  scope; the design offers no surface for any of them.
- The onboarding screen in the design project is a separate feature.
- The launch gap is covered by the platform's own splash screen (FR-002), not an in-app screen. The
  `splash` phase in the design prototype's state machine is not that screen and is not implemented.
- Google's consent screen, account picker, and error screens are owned and rendered by Google.
- The design carries no legal line, so FR-051 adds one below the primary action; the design is
  updated to match rather than the requirement dropped, because FR-021 stores personal data.
- The terms and privacy addresses arrive as configuration, not code (FR-051). Building and testing
  is therefore not blocked; releasing to users is, until both exist.
- The learning progress this feature preserves is produced by other features; this feature
  guarantees only that the account it hangs off is stable.
