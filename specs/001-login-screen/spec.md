# Feature Specification: Login Screen

**Feature Branch**: `feature/001-login-screen` | **Feature ID**: `001-login-screen`

**Created**: 2026-08-13

**Status**: Ready for implementation

**Input**: User description: Notion product page "001. Авторизация"
(https://app.notion.com/p/001-3abdc7d20da48100bafeec7b2ace8de2) plus the Claude Design
project screen `screen_login.dc.html`
(https://claude.ai/design/p/0c49e08b-d7ab-4cd3-88be-8483024790e5).

## Clarifications

### Session 2026-08-13

- Q: On launch with no network but a stored session, where does the user land? → A: The main
  screen — a stored session is sufficient, with no start-up call to the server.
- Q: What is shown during the launch gap before the stored session has been read? → A: The
  platform's own splash screen, following each platform's guidelines, held until the answer is
  known.
- Q: How long does a session survive without activity? → A: 90 days, sliding — each renewal
  extends it by another 90 days.
- Q: Which personal data does Yap keep from the provider? → A: The stable provider identifier,
  the email address, the display name, and the avatar reference.
- Q: Should the login endpoints be rate limited? → A: Yes, per originating IP address, on
  both endpoints.
- Q: Should the login screen link to terms and a privacy policy? → A: Yes — a legal line under
  the primary action. The destination addresses are supplied later; the line itself is part of
  this feature.
- Q: What are the narrowest width and largest font scale the layout must survive? → A: 320 dp
  wide and 200% font scale.
- Q: What is the Google login path? → A: The device's native Google credential flow first,
  falling back automatically to the system browser when that flow is unavailable — so a device
  without Google's services can still log in.
- Q: Does logging in on a second device keep the first device logged in? → A: No — one session
  per account; a new login invalidates every earlier session.
- Q: Is the user told why they were logged out? → A: No — the login screen looks the same
  whatever made the session invalid, and the reason is not reported to the app either.
- Q: Does the server record login outcomes and rate-limit rejections? → A: No — nothing
  beyond the framework's default logging; this feature states no requirement.
- Q: What happens when a device has neither a Google credential provider nor a usable browser?
  → A: An ordinary recoverable failure (FR-014), with no message distinct from any other
  failure.
- Q: Must Sign in with Apple work on iOS before this feature is done? → A: No — it stays a
  "not available yet" notice, and the iOS App Store submission waits for the follow-up
  feature that makes it work.
- Q: What do the legal line's two links do before the terms and privacy addresses exist? → A:
  Each destination is a configured value; while one is unset its link renders but does not
  navigate, and the app is not released to users until both are set.
- Q: Must the rate limit accommodate many real users behind one shared IP address? → A: Yes —
  the threshold is sized so an office or carrier NAT never reaches it, making it a coarse
  guard against automated abuse rather than a per-user fairness control.
- Q: On launch with a stored session that has already passed its own expiry, does the user see
  the main screen or the login screen? → A: The login screen, straight away — the device stores
  the session's expiry and checks it locally, with no network request.
- Q: What does a user see on returning to an attempt that never concluded? → A: The idle login
  screen, silently — an attempt that ends with neither success nor an explicit failure is
  treated as a cancellation, with no message.
- Q: Does every app open renew the 90-day session window, or only one that reaches the server?
  → A: Only a successful exchange with the server renews it; the device never extends the
  window on its own, so opening the app offline does not keep the session alive.
- Q: Does a request that fails because the server is unreachable sign the user out? → A: No —
  only an explicit rejection invalidates a session; an unreachable server leaves it intact and
  the app retries later.
- Q: What is the rate-limit threshold in numbers? → A: 100 requests per minute per originating
  IP address, on each unauthenticated login endpoint.
- Q: What happens when the provider succeeds but withholds the stable account identifier? → A:
  No account is created or resolved and no session is issued; the attempt ends as an ordinary
  recoverable failure.
- Q: Who establishes that a login confirmation is genuine? → A: The server, independently of
  the app — what the app reports is never taken on trust.
- Q: Nothing else in this feature makes an authenticated request, so what ever renews a session
  before its 90 days run out? → A: The app renews it at launch. Once the launch decision has been
  made from stored state, a session whose access token is due for renewal is refreshed against the
  server in the background, without delaying the main screen (FR-032).
- Q: What ends a login attempt that never returns an answer at all? → A: A 60-second bound. An
  attempt still unresolved after 60 seconds is treated as a cancellation — silently idle, no
  message (FR-013).
- Q: How close to its expiry must the stored access token be for the launch renewal to fire? → A:
  Five minutes. A token already expired or within five minutes of expiring is refreshed; one
  further out is left alone, so an app opened repeatedly costs at most one renewal (FR-032).
- Q: What has to change to turn a second provider on later? → A: Only that provider's own two
  declared facts — whether it is shown and whether it is usable — plus the login path it runs.
  Nothing that builds the list, renders a row, or handles a choice may name a provider (FR-033).

## User Scenarios & Testing *(mandatory)*

### User Story 1 - First login without registration (Priority: P1)

A person opens Yap for the first time and lands on the login screen. They tap the single
primary action, choose Google from the list of authentication providers, confirm the account with
Google, and arrive on the main screen. They are never asked to invent a password, fill a
registration form, or supply a profile.

**Why this priority**: This is the whole point of the feature — without it nobody can use
the product at all. Everything else in this spec is a refinement of this path.

**Independent Test**: Install the app fresh, complete the Google confirmation, and observe
arrival on the main screen with an account that exists. Delivers a usable account on its
own.

**Acceptance Scenarios**:

1. **Given** a fresh install with no session, **When** the app launches, **Then** the
   login screen is shown.
2. **Given** the login screen is shown, **When** the user activates the primary login
   action, **Then** the list of authentication providers for that platform appears.
3. **Given** the provider list is open on Android, **When** the user reads it, **Then**
   Google and T-ID are listed and Apple is not present.
4. **Given** the provider list is open on iOS, **When** the user reads it, **Then** Google,
   Apple, and T-ID are all listed.
5. **Given** a Google account never used with Yap before, **When** the user confirms it,
   **Then** a new Yap account is created with no further registration step and the user
   arrives on the main screen.
6. **Given** the provider list is open, **When** the user chooses Apple or T-ID, **Then** a
   message states the provider is not available yet, no login is attempted, and the user
   can still choose Google.
7. **Given** a device with no Google services installed, **When** the user chooses Google,
   **Then** confirmation opens in the system browser without the user asking for it, and a
   successful confirmation reaches the main screen exactly as it would on any other device.

---

### User Story 2 - Returning with the same provider account (Priority: P2)

Someone who logged in before — perhaps after reinstalling, or after their session ended —
logs in again with the same Google account and finds their existing Yap account, streak,
and lesson history rather than a blank slate.

**Why this priority**: Progress that survives is what makes the account worth having; it
is the reason stated for asking the user to log in at all. It cannot be tested until
Story 1 exists, but it is the difference between an account and a formality.

**Independent Test**: Log in, generate some learning progress, clear the session, log in
again with the same Google account, and confirm the progress is still there.

**Acceptance Scenarios**:

1. **Given** a Google account already mapped to a Yap account, **When** the user logs in
   with it again, **Then** the same Yap account opens with its learning progress preserved.
2. **Given** two different Google accounts, **When** each is used to log in, **Then** each
   opens its own independent Yap account with its own progress, and the two are not merged.

---

### User Story 3 - Session restored on relaunch (Priority: P2)

A returning user opens the app and goes straight to the main screen. They do not see the
login screen and are not asked to confirm anything with a provider again.

**Why this priority**: Re-authorizing on every launch would undo the "one tap, no
passwords" promise the screen makes. Independently valuable and independently observable.

**Independent Test**: Log in, close the app fully, reopen it, and confirm the main screen
appears without the login screen showing.

**Acceptance Scenarios**:

1. **Given** a stored session from a previous run that is still inside its window, **When**
   the app launches, **Then** the main screen is reached without the login screen being shown
   and without provider confirmation, whether or not the device has a network connection.
2. **Given** the session has become invalid, **When** the app determines this, **Then** the
   user is returned to the login screen and can log in again.
3. **Given** a stored session, **When** the app is launched repeatedly, **Then** the login
   screen never appears even briefly between the splash screen and the main screen.
4. **Given** a stored session that has passed its expiry, **When** the app launches, **Then**
   the login screen is shown directly, without the main screen appearing first and without
   any network request.

---

### User Story 4 - Cancelling or failing, then retrying (Priority: P3)

A user starts a login, changes their mind at Google's screen or hits a failure, and lands
back on the login screen able to try again without restarting the app.

**Why this priority**: Abandonment and failure are ordinary, not exceptional. Getting this
wrong strands users outside the product entirely, but the feature is demonstrable without
it.

**Independent Test**: Cancel at Google's confirmation screen, then start a second attempt
and complete it.

**Acceptance Scenarios**:

1. **Given** a login attempt in progress, **When** the user cancels at the provider,
   **Then** the login screen returns to its idle state with no error shown and login
   remains available.
2. **Given** a login attempt in progress, **When** it fails, **Then** a plain-language
   message is shown and login remains available for another attempt.
3. **Given** the provider list is open, **When** the user dismisses it without choosing a
   provider, **Then** nothing is treated as an error and the login screen returns to idle.
4. **Given** a login attempt is in progress, **When** the user activates the primary
   action again, **Then** no second concurrent attempt is started.
5. **Given** a login attempt in progress, **When** the user leaves the app and returns much
   later without the attempt having concluded, **Then** the login screen is idle, no error is
   shown, and login is available again.

---

### Edge Cases

- The user leaves the app mid-provider-confirmation and returns much later — the screen
  must not be stuck showing progress forever.
- The device is offline when Google is chosen — the user gets a message they can act on,
  not a silent stall.
- The device has no Google services at all — a de-Googled build such as GrapheneOS without the
  sandboxed compatibility layer. The browser fallback is what makes this device usable.
- The device has neither a Google credential provider nor any browser able to handle the
  confirmation.
- The user dismisses the browser tab instead of cancelling inside Google's page.
- The device has no Google account logged in yet, so the confirmation starts by asking the user
  to add one.
- The provider returns success but withholds the stable account identifier Yap needs to
  match accounts across logins.
- A login request reaches the server directly, without passing through the app, carrying a
  fabricated provider confirmation.
- The session becomes invalid while the user is mid-session in the foreground — nothing in this
  feature makes an authenticated request, so the app cannot learn of it until its next launch
  renewal (FR-032), and until then the user carries on undisturbed.
- The server is unreachable for a long stretch — the user must stay logged in throughout,
  however many attempts fail.
- The same account logs in on a second device while the first is still open — the first
  device's session is already dead and only finds out on its next launch renewal.
- The user taps a provider twice, or taps two providers, in rapid succession.
- The provider reports an error whose text is long, empty, or not in the user's language.
- The device is at 320 dp of width and 200% font scale at the same time — the hero heading, the
  rotating topic, and the legal line all compete for the same vertical space.
- Many devices share one outbound IP address — an office or a mobile carrier's NAT — and
  collectively approach the rate limit.
- The provider reports a different email address, display name, or avatar for an account that
  has logged in before.
- The provider omits the display name or the avatar entirely.
- The terms or privacy document is unreachable when its link is followed.
- A session is renewed on the very last day of its 90-day window.
- The user opens the app regularly but always without a network, so nothing ever renews the
  session and it reaches its expiry despite the app being in use.
- The device clock is wrong or has been moved, so a live session looks expired locally or a
  dead one looks live.

## Requirements *(mandatory)*

### Functional Requirements

#### Access and login surface

- **FR-001**: The app MUST show the login screen whenever there is no stored session, and
  MUST NOT show it when there is one.
- **FR-024**: While the app is still determining whether a session is stored, it MUST show
  the platform's own splash screen — following that platform's guidelines rather than a
  screen of the app's own — and MUST show neither the login screen nor the main screen until
  the answer is known. A user with a stored session MUST never see the login screen appear
  and disappear.
- **FR-002**: The login screen MUST present exactly one primary login action, which
  reveals the list of available authentication providers rather than starting a specific one.
- **FR-003**: The provider list MUST show Google and T-ID on both Android and iOS, and MUST
  show Apple on iOS only. Apple MUST NOT be presented on Android in any state.
- **FR-004**: Google MUST be a working authentication provider on both platforms. Apple and T-ID
  MUST be listed but not yet functional: choosing either MUST state plainly that the provider
  is not available yet, MUST NOT begin a login attempt, and MUST NOT be reported as a
  failure.
- **FR-033**: FR-003 and FR-004 MUST be expressed as two facts declared once per provider —
  whether the provider is shown at all, and whether choosing it starts a real login — rather
  than as decisions rediscovered wherever providers are listed, drawn, or chosen. The list, the
  rows, and the handling of a choice MUST be driven entirely by those declarations: no provider
  may be named anywhere else. Consequently, turning a provider on or off later MUST be exactly
  a change to its own declarations plus supplying the login path it runs, with the shown and
  usable facts independent of each other — a provider MAY be usable while hidden, or shown while
  unusable. A provider that is not shown MUST NOT be reachable by any means, whatever it
  declares about being usable.
- **FR-005**: Choosing Google MUST hand the user to Google's own account confirmation and
  return them to Yap when it concludes, in either outcome.
- **FR-029**: Google login MUST NOT depend on the device having Google's services installed.
  The app MUST first attempt the device's native Google credential flow and, when that flow is
  unavailable on the device, MUST fall back to confirming the account in the system browser
  without the user choosing between them or being told which one ran. Both paths MUST produce
  the same account, the same session, and the same outcomes (success, cancellation, failure).
  Confirmation MUST NOT be presented inside an embedded web view. When neither path is
  available on the device, the attempt MUST end as an ordinary recoverable failure (FR-014),
  with no message distinct from any other failure and with login left available for another
  attempt.

#### Account resolution

- **FR-031**: The system MUST establish for itself that a login confirmation genuinely comes
  from the provider and genuinely names the account it claims. That check MUST happen on the
  server, independently of the app: what the app reports MUST NOT be taken on trust, since a
  request can reach the server without passing through the app at all. A confirmation that
  cannot be verified — including one that verifies but carries no stable account identifier
  (FR-026) — MUST NOT create or resolve an account and MUST NOT issue a session, and the attempt
  MUST end as an ordinary recoverable failure (FR-014) with login left available for another
  attempt. This requirement is the single owner of that rule; others refer to it rather than
  restating it.
- **FR-006**: On the first successful confirmation of a given provider account, the system
  MUST create a Yap account with no additional registration step, password, or profile
  input required from the user.
- **FR-007**: A repeat login with the same provider account MUST resolve to the same Yap
  account, with its learning progress intact.
- **FR-008**: Accounts from different providers MUST NOT be linked automatically. Each
  provider account maps to its own Yap account with independent progress.
- **FR-026**: The system MUST store the provider's stable account identifier together with the
  email address, display name, and avatar reference the provider reports, refreshing all of
  them on each successful login. Every field except the identifier is descriptive data: none
  of them MUST be used to find, match, merge, or link accounts, so two providers reporting the
  same address still produce two independent Yap accounts (FR-008). A descriptive field the
  provider omits MUST be stored as absent and MUST NOT block login. The stable identifier
  itself is required: a confirmation that concludes successfully but withholds it MUST be refused
  exactly as an unverifiable one is, under FR-031.
- **FR-009**: A successful login MUST take the user to the main screen.

#### Session

- **FR-010**: A successful login MUST establish a session that survives closing and
  reopening the app.
- **FR-011**: On launch with a stored session that has not passed its own expiry, the app MUST
  reach the main screen without showing the login screen and without re-confirming with a
  provider. The decision MUST be made from stored state alone — the app MUST NOT contact the
  server before deciding — so launching without a network connection still reaches the main
  screen. The device MUST store the session's expiry alongside the session itself and MUST
  treat a stored session already past that expiry as no session at all, showing the login
  screen immediately rather than the main screen. The stored expiry is a courtesy check, not
  the authority: a session still inside its window that has in fact been revoked or superseded
  is discovered on the first exchange with the server (FR-032) and handled by FR-012, and a live session
  that a wrong device clock makes look expired costs the user a new login but never grants
  access.
- **FR-012**: When the session becomes invalid, the app MUST return the user to the login
  screen. A session becomes invalid only when the server explicitly rejects it or when the
  locally stored expiry FR-011 defines has passed. A request that fails because the server could not
  be reached — no network, a timeout, or an unavailable server — MUST leave the session intact,
  MUST NOT return the user to the login screen, and MUST be retried later instead. The login
  screen MUST look identical whatever made the session invalid — expiry, revocation, or a
  login on another device (FR-030) — and the system MUST NOT report which of them happened,
  to the app or to the user. This requirement is the single owner of what does and does not end a
  session; FR-025, FR-032, and SC-014 refer to it rather than restating it.
- **FR-025**: A session MUST expire 90 days after its last renewal, and every renewal MUST
  reset that 90-day window. Only a successful exchange with the server renews a session: an
  app launch that never reaches the server MUST NOT move the expiry, and the device MUST NOT
  extend the window on its own. A user whose app reaches the server at least once every 90
  days therefore never has to log in again on that device — the launch renewal of FR-032 is what
  makes that reachable in practice — and a session whose last successful exchange was more than 90
  days ago MUST stop being accepted.
- **FR-030**: A Yap account MUST hold at most one valid session at a time. A successful
  login MUST invalidate every session previously established for that account, so a user
  who logs in on a second device is logged out on the first and returns to the login screen
  there on its next exchange with the server (FR-012, FR-032).
- **FR-032**: The app MUST attempt to renew a stored session with the server on launch, so that a
  user who keeps opening the app keeps their session alive (FR-025). The attempt MUST happen only
  **after** the launch decision has been made from stored state, so it never delays or gates
  reaching the main screen and never contradicts FR-011's "no request before deciding" rule. It
  MUST be made when the stored access token has expired or is **within five minutes** of expiring,
  and MUST be skipped otherwise, so an app opened many times a day does not renew many times a
  day — a token further out than that margin means no request at all. Its three
  outcomes are the ones FR-012 already fixes: a success moves the expiry (FR-025), an explicit
  rejection returns the user to the login screen, and an unreachable server leaves the stored
  session exactly as it is. This is the only thing in this feature that reaches the server after
  login; without it no session would ever be renewed and every user would be logged out after 90
  days (FR-025, SC-009), and a session superseded on another device would never be discovered
  (FR-030, SC-013).

#### Cancellation, failure, and retry

- **FR-013**: Cancellation at the provider MUST return the login screen to its idle state
  with no error message shown, and MUST leave login available for another attempt. An attempt
  that concludes with neither a success nor an explicit failure MUST be treated the same way —
  silently idle and available again — whether the user dismissed the browser tab, left the app
  mid-confirmation and returned much later, or nothing ever came back. No attempt MUST be left
  showing progress indefinitely: **an attempt still unresolved 60 seconds after it started MUST be
  ended and treated as a cancellation**, which is what puts a bound on SC-007 rather than leaving
  it to chance.
- **FR-014**: A failed login MUST show a plain-language message that does not expose
  provider or transport internals, and MUST leave login available for another attempt.
- **FR-015**: While a login attempt is in progress, the primary action MUST indicate
  progress and MUST NOT start a second concurrent attempt when activated again. The same guard
  MUST cover the provider list: choosing a provider twice, or choosing a second provider, while an
  attempt is in progress MUST NOT start another one.
- **FR-016**: Dismissing the provider list without choosing a provider MUST be treated as
  neither an error nor an attempt.
- **FR-027**: Every unauthenticated login endpoint MUST rate limit requests per originating
  IP address and reject requests over the threshold. **The threshold is 100 requests per minute
  per originating IP address**, applied to each such endpoint. A rejected request MUST be
  presented to the user as an ordinary recoverable failure (FR-014) and MUST leave login
  available for another attempt once the limit clears. That threshold is far above what a
  person retrying after cancellations or failures reaches, and above what many ordinary users
  sharing one outbound address — an office network or a mobile carrier's NAT — reach
  collectively, since each device logs in at most once every 90 days (FR-025). The limit is
  therefore a coarse guard against automated abuse, not a per-user fairness control.

#### Presentation

- **FR-017**: The login screen MUST present, top to bottom: the scrolling promotional band
  ("ВХОД ЗА 1 ТАП ✦ БЕЗ ПАРОЛЕЙ"), the hero heading "БОЛТАЙ БЕЗ СТРАХА", a rotating topic
  word cycling through "SMALL TALK", "ОТКАЗЫ", and "ЗНАКОМСТВА", the supporting body copy,
  the primary action labelled "ВОЙТИ", the caption "Аккаунт нужен для стрика и истории
  занятий.", and the legal line required by FR-028.
- **FR-018**: The provider list MUST be presented as a bottom sheet headed "Способы входа",
  each provider identified by its own name and mark, dismissible by tapping outside it.
- **FR-019**: The screen MUST render correctly in both light and dark themes, following the
  colour roles defined in the design.
- **FR-020**: When the operating system reports a reduced-motion preference, decorative
  animation MUST NOT play, and all content MUST remain fully readable in its resting state.
- **FR-021**: The layout MUST adapt to narrow widths and to system font scaling without
  clipping or overlapping content, and MUST respect system insets at the top and bottom of
  the screen. The guaranteed range is **down to 320 dp of width and up to 200% font scale**;
  every element — including the primary action, the provider sheet, and the legal line — MUST
  stay fully visible and operable throughout it. Content that no longer fits MUST scroll
  rather than be cut off.
- **FR-022**: Every interactive element MUST be reachable and operable by assistive
  technology, with a spoken name that identifies which authentication provider it starts.
- **FR-023**: Transient messages — failures and the not-yet-available notice — MUST appear
  as a banner that dismisses itself **after four seconds** and does not block interaction with the
  screen beneath it.
- **FR-028**: The login screen MUST show a legal line beneath the primary action stating that
  continuing accepts the terms of service and the privacy policy, with each document reachable
  as its own link. The line MUST be legible rather than hidden, MUST NOT require a separate
  checkbox or any extra tap before logging in, and MUST be present on both platforms. Each
  link's destination MUST be a configured value rather than a fixed one. While a destination
  is unset, its link MUST still render as part of the line, MUST NOT navigate, and MUST NOT
  report a failure; the app MUST NOT be released to users while either destination is unset.
  Following a configured link hands the destination to the system's own browser: what the browser
  then shows — including a document that turns out to be unreachable — is outside Yap's surface,
  and the app MUST NOT report a failure of its own for it.

### Key Entities

- **User Account**: A person's identity inside Yap and the owner of their learning progress
  (streak, lesson history). Created on first successful login with a provider account.
- **Provider Identity**: The reference to an account held at an external provider — Google
  in this feature, Apple and T-ID later. Carries the provider's stable account identifier —
  required, and the only thing accounts are matched by — plus the email address, display name,
  and avatar reference it reports; everything but the identifier is descriptive, optional, and
  never used to match accounts. Belongs to exactly one
  User Account; a User Account has exactly one Provider Identity.
- **Session**: The state that lets a returning user reach the main screen without
  re-confirming with a provider. Belongs to one User Account, survives app restart, and carries
  an expiry that moves forward on every renewal — and only a successful exchange with the server
  renews it (FR-025), which in this feature means the launch renewal of FR-032. The expiry is stored on the device
  beside the session so the app can tell a dead session from a live one without the network. A
  Session can also become invalid before its expiry, at which point the user returns to the
  login screen. A User Account holds at most one valid Session; a new login replaces it
  (FR-030).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A new user reaches the main screen from the login screen in under 15 seconds
  of their own effort, excluding time spent inside Google's confirmation screens. Analytics is out
  of scope for this feature, so this one is observed by hand during release verification rather
  than measured in production.
- **SC-002**: 100% of returning logins with a previously used provider account open the
  same Yap account with its streak and lesson history intact.
- **SC-003**: 100% of launches that hold a stored session still inside its window reach the
  main screen without the login screen appearing, including launches with no network
  connection.
- **SC-004**: On Android, Apple appears in zero authentication provider lists; on iOS, all three
  providers appear — verified on both platforms.
- **SC-005**: 100% of cancellations and failures leave the user able to start another
  login without restarting the app.
- **SC-006**: Choosing Apple or T-ID never leaves the screen in a progress state and never
  produces an error message — only the not-yet-available notice.
- **SC-007**: No login attempt leaves the screen in a progress state indefinitely; every
  attempt resolves to success, an idle screen, or a failure message within 60 seconds. An
  attempt that concludes without either a success or an explicit failure resolves to the idle
  screen, never to a failure message.
- **SC-008**: The screen presents its full content without clipping or overlap in both
  themes, at 320 dp of width, and at 200% font scale — including both extremes together.
- **SC-009**: A user who opens the app with a network at least once every 90 days is never asked
  to log in again on that device, because each such launch renews the session (FR-032); a session
  whose last successful server exchange was more than 90 days ago always requires a new login,
  and the login screen — not the main screen — is what that user sees on launch. Launches that
  never reach the server do not count towards keeping the session alive.
- **SC-010**: Neither a person retrying by hand after cancellations and failures nor a full
  office or carrier NAT of ordinary users sharing one address ever triggers the rate limit,
  while a client sending more than 100 requests a minute from one address is rejected in 100%
  of cases.
- **SC-011**: The legal line and both of its links are visible on every launch of the login
  screen, on both platforms, in both themes, and across the whole 320 dp / 200% range, without
  adding a step to logging in. With both destinations configured, each link opens its own
  document in 100% of taps; with a destination unset, its link never navigates and never shows
  an error, and no build in that state reaches users.
- **SC-012**: A device with no Google services installed completes Google login successfully,
  reaching the same account and session as the same Google account would on a device that has
  them.
- **SC-013**: After a login on a second device, the first device returns to the login screen
  on its next launch renewal (FR-032) in 100% of cases, and no account ever holds two valid
  sessions at once.
- **SC-014**: A device that cannot reach the server — offline, timing out, or facing an
  unavailable server — is never returned to the login screen for that reason alone, however
  many times it fails; only an explicit rejection or a passed local expiry does that.
- **SC-015**: A login request carrying a forged or unverifiable provider confirmation creates
  no account and yields no session in 100% of cases, whether or not it came from the app.
- **SC-016**: Every provider behaves correctly under all four combinations of its two declared
  facts — shown or hidden, usable or not — and switching any provider between them is achieved
  by changing that provider's declarations alone, with no edit to how the list is built, how a
  row is drawn, or how a choice is handled.

## Assumptions

- Only Google login is delivered working by this feature. Apple and T-ID are drawn in the
  sheet as the design specifies, but each declares itself not yet usable (FR-033) and reports
  as much when chosen; making one work is a change to its own declarations plus its login path,
  and is separate follow-up work. Apple's review rules require an app that offers Google
  login to also offer Sign in with Apple, so the iOS build is not submitted to the App
  Store until that follow-up lands. iOS is built, run, and tested by this feature; only
  release to iOS users waits.
- The fallback in FR-029 matters on Android, where the native flow requires Google's services.
  On iOS the platform's own Google login already runs in the system browser, so it has no
  equivalent gap and needs no second path.
- Recording login outcomes and rate-limit rejections is out of scope: the server keeps only
  whatever its framework logs by default, and this feature adds no logging, metric, or audit
  requirement. The consequence is accepted knowingly — the FR-027 threshold cannot be tuned
  from evidence after release until some later feature adds that record.
- Analytics is out of scope. The metrics listed on the Notion page — conversion, cancel
  share, failure share, restored-session share, time to success — remain a product goal to
  be delivered once the app has an analytics capability at all.
- All user-facing copy on this screen is Russian, matching the design. No other locale is
  in scope.
- The main screen reached after login may be a placeholder destination — the home feature
  is not part of this work, and this feature only needs to hand off to it. Because no other
  feature exists yet, the launch renewal of FR-032 is the only thing that contacts the server
  after login; once the home feature adds its own authenticated traffic, that traffic renews the
  session too and FR-032 becomes one path among several rather than the only one.
- Logging out, deleting an account, editing a profile, and linking two provider accounts are
  all out of scope; the design offers no surface for any of them.
- The onboarding screen present in the design project is a separate feature and is not part
  of this one.
- The launch gap is covered by the platform's own splash screen (FR-024), not by an in-app
  screen. The `splash` phase left in the design prototype's state machine is not that screen
  and is not implemented.
- The prototype's placeholder message on Google ("Что-то пошло не так") stands in for real
  behavior and is not the shipping copy for a working provider. The "скоро появится" messages
  on Apple and T-ID do reflect what those providers do in this feature.
- Google's consent screen, account picker, and error screens are owned and rendered by
  Google; Yap is responsible for what happens before and after them.
- The design carries no legal line, so FR-028 adds one below the primary action. The design is
  updated to match rather than the requirement dropped, because FR-026 stores personal data.
- The terms-of-service and privacy-policy addresses are supplied later. The line, its copy, and
  its two links are built in this feature and read their destinations from configuration, so
  the documents arriving is a configuration change rather than a code change (FR-028). Building
  and testing the feature is therefore not blocked; releasing it to users is, until both
  addresses exist.
- The learning progress this feature preserves (streak, lesson history) is produced by other
  features; this feature only guarantees the account it hangs off is stable.
