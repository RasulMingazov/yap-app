# Feature Specification: Auth Provider Selection and Login Theming Refactor

**Feature Branch**: `feature/003-auth-ui-refactor`

**Created**: 2026-08-14

**Status**: Draft

**Input**: User description: `fix.md` — семь пунктов рефакторинга авторизации и UI: цвета в общую тему,
флаги провайдеров, единый LoginUseCase, отдельный Bottom Sheet выбора провайдера, удаление
AuthProviderCatalog, корректная анимация Snackbar, чистка data-слоя.

## Clarifications

### Session 2026-08-14

- Q: If the navigation pre-release turns out not to link with the current dependency injection
  library, what happens? → A: Stop and escalate; no automatic fallback is pre-authorised.
- Q: Should the selection screen handle having no visible providers? → A: Out of scope while the
  roster is local and cannot produce that state; it arrives with the remote roster.
- Q: Does the selection screen's own open and close animation follow the reduced-motion preference?
  → A: No — it keeps the platform's standard sheet animation; the preference governs the message only.

## User Scenarios & Testing *(mandatory)*

Two audiences are served. A person signing in must see the same screen behave better — a message
that leaves the way it arrived, one colour language across themes, a selection surface that is a
real screen. A contributor adding the next provider must be able to do so without touching the
login screen or its state holder.

### User Story 1 - One way in, whichever provider (Priority: P1)

A person taps the primary action, picks a provider, and signs in. The login screen's state holder
asks a single login entry point for that provider; it never holds a collection of per-provider
logins and never learns which providers exist in code.

**Why this priority**: This is the load-bearing change. Every later story assumes the state holder
no longer owns provider knowledge, and it is what makes "add a provider without editing the login
screen" true.

**Independent Test**: Sign in with Google end to end while the rest of the screen is untouched, then
register a second handler and confirm it runs without a single edit to the login state holder.

**Acceptance Scenarios**:

1. **Given** a provider with a registered handler, **When** it is chosen, **Then** the login runs and
   its outcome — success, cancellation, failure — is reported exactly as before.
2. **Given** the login state holder, **When** its dependencies are read, **Then** there is no
   collection keyed by provider and no reference to a provider catalogue.
3. **Given** a provider whose handler is not registered, **When** it is chosen, **Then** the person
   is told it is unavailable and no login is attempted.
4. **Given** a new provider with a handler, **When** it is registered, **Then** it is selectable with
   no change to the login state holder or the login screen.
5. **Given** a login already running, **When** the primary action or a provider is chosen again,
   **Then** no second login starts.

---

### User Story 2 - Providers are decided in one place (Priority: P1)

Which providers appear, and which of them can be picked, is decided by one observable source in the
domain layer. Presentation subscribes to it. Nothing in the UI hardcodes the roster, and the source
can later be fed by the backend without any presentation change.

**Why this priority**: The current roster lives in a presentation-side catalogue that mixes platform
rules with UI strings. Fixing the ownership first is what lets the selection screen be a thin
renderer and lets the catalogue be deleted.

**Independent Test**: Change the source so a provider becomes hidden or unselectable and confirm the
selection surface reflects it without any UI edit; verify no UI resource is reachable from the
domain or data layer.

**Acceptance Scenarios**:

1. **Given** the running app, **When** the provider roster is requested, **Then** it arrives as an
   observable stream and the surface re-renders when it changes.
2. **Given** a provider hidden for the current platform, **When** the roster is observed, **Then**
   that provider is absent from what the person sees.
3. **Given** a provider that is shown but not selectable, **When** it is chosen, **Then** no provider
   login runs and the person is told it is not yet available.
4. **Given** the domain and data layers, **When** their sources are read, **Then** no icon, label, or
   other UI resource appears in them.
5. **Given** the previous provider catalogue, **When** the refactor lands, **Then** it no longer
   exists and nothing references it.

---

### User Story 3 - Choosing a provider is its own screen (Priority: P2)

The provider list is a destination of its own — a bottom sheet screen with its own state, its own
mapping to what is drawn, and its own state holder. It shows each provider's name and mark, reflects
availability, and hands the chosen provider back to the login screen. The login screen's state
holder keeps none of this.

**Why this priority**: It is the largest structural move and depends on Stories 1 and 2 being in
place, but it is what actually shrinks the login state holder.

**Independent Test**: Open the selection destination directly, choose a provider, and confirm the
login screen receives that choice and starts the login; confirm the login state holder no longer
exposes a provider list.

**Acceptance Scenarios**:

1. **Given** the login screen, **When** the primary action is used, **Then** the selection screen
   opens as its own destination.
2. **Given** the selection screen, **When** a provider is chosen, **Then** the screen closes and the
   chosen provider reaches the login screen, which starts the login.
3. **Given** the selection screen, **When** it is dismissed by back gesture, system back, or swipe,
   **Then** no provider is returned and no login starts.
4. **Given** the selection screen, **When** it renders a provider, **Then** the name and mark come
   from its own mapping of the provider identity, not from the domain layer, and both match the
   design.
5. **Given** the login screen's state, **When** it is inspected, **Then** it carries no provider
   list and no provider-row model.

---

### User Story 4 - One colour language, no literals on the screen (Priority: P2)

The colours the login screen uses are part of the app theme under names that say what they are for,
not where they came from. No colour literal is written in a UI component. The transient message
carries the same colour in dark theme as in light.

**Why this priority**: Independent of the architecture stories and separately verifiable, but it
touches the shared theme, so it is safest once the screen's structure has stopped moving.

**Independent Test**: Render the login screen in both themes, compare against the current light-theme
appearance, and confirm the only intended difference is the message colour in dark theme.

**Acceptance Scenarios**:

1. **Given** any login UI component, **When** it is read, **Then** it declares no colour literal and
   reads every colour from the theme.
2. **Given** the theme, **When** a colour is looked up, **Then** its name states its role — surface,
   accent, caption, message background — and not a product or screen name.
3. **Given** the transient message, **When** it is shown in dark theme, **Then** its background and
   text colours equal the light-theme values.
4. **Given** both themes, **When** the login screen renders, **Then** no other colour already on the
   screen changes value, and any role the screen did not previously express comes from the design.

---

### User Story 5 - The message leaves the way it should (Priority: P3)

A transient message appears, waits, and leaves by moving upward off the screen rather than only
fading. Several messages in a row are shown one after another without overlapping or being lost.

**Why this priority**: A visible defect, but narrow and independent of every structural change.

**Independent Test**: Trigger a failure twice in quick succession and observe both messages appear in
order, each auto-dismissing with upward motion.

**Acceptance Scenarios**:

1. **Given** a message is shown, **When** its display time elapses, **Then** it leaves by moving
   upward out of view, not by fading in place.
2. **Given** a message is on screen, **When** a second message is raised, **Then** the first finishes
   and the second follows it; neither is silently dropped.
3. **Given** the platform's standard message mechanism, **When** the screen shows a message, **Then**
   that mechanism owns queueing, timing, and dismissal instead of hand-rolled timers.
4. **Given** reduced motion is requested, **When** a message is dismissed, **Then** it disappears
   without motion and the message is still readable for its full duration.

---

### User Story 6 - A leaner auth data layer, agreed before it changes (Priority: P3)

Before any data-layer code moves, a short written proposal is delivered: what is proposed to change,
why the current shape is excessive, what the simplified structure looks like, and what the risks and
trade-offs are. Work stops there until the proposal is approved. Only then is the simplification
implemented.

**Why this priority**: It is the only part of the request whose scope is not yet known; the gate is
what keeps it from expanding silently.

**Independent Test**: Confirm the proposal exists and is approved, and that no data-layer commit
precedes that approval.

**Acceptance Scenarios**:

1. **Given** the auth data layer, **When** the review completes, **Then** a proposal names every
   candidate — verbose constructions, duplicated code, needless wrappers and intermediate models,
   single-implementation interfaces without a stated reason, redundant conversions, and code tied to
   one provider — with a reason for each.
2. **Given** the proposal, **When** it is delivered, **Then** it states the resulting structure and
   the risks and trade-offs of getting there.
3. **Given** an unapproved proposal, **When** work continues, **Then** no data-layer file has been
   changed.
4. **Given** an approved proposal, **When** it is implemented, **Then** login, session renewal, and
   token access behave exactly as before and their tests pass unchanged in intent.

---

### Edge Cases

- The roster changes while the selection screen is open — a provider disappears or becomes
  unselectable. The list updates in place; a provider already chosen and mid-login is unaffected.
- A provider exists in the identity set but has no registered handler. It must not crash the app: it
  is reported unavailable at the moment of choice.
- The selection screen is left by system back during an in-flight login. The login continues and its
  outcome is still reported on the login screen.
- Configuration change while the selection screen is open. It survives, and no login is started
  twice. Process death is deliberately different: the app returns to the login screen with the
  chooser closed. Restoring a transient chooser is not worth persisting navigation state for, and
  stating the limit is preferable to implying a guarantee that does not hold.
- A message would be raised while the selection screen covers the login screen. The selection screen
  closes before any login runs, so a message is never raised behind it; nothing may reintroduce a
  path that reports an outcome while the sheet is still up.
- Theme changes while a message is on screen. The message stays legible and its colour follows the
  single shared value.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The login screen's state holder MUST depend on exactly one login entry point that takes
  the chosen provider as its argument.
- **FR-002**: The system MUST NOT expose any collection of per-provider logins to presentation; the
  mapping from provider to its login path lives in the domain or data layer.
- **FR-003**: The system MUST offer a named registration point where a provider's login handler is
  declared, so that adding a provider is a registration rather than an edit to existing branching.
- **FR-004**: Adding a provider MUST NOT require changes to the login screen or its state holder.
- **FR-005**: Choosing a provider with no registered or enabled handler MUST report unavailability to
  the person and start no login.
- **FR-006**: The system MUST expose the provider roster as an observable stream so a later remote
  source can replace the local one without presentation changes.
- **FR-007**: Every provider MUST carry whether it is shown and whether it can be chosen, and both
  MUST be decided by the roster outside presentation, reaching the UI without any UI-side rule.
- **FR-008**: Platform-specific availability MUST be applied by that same source, not by the UI.
- **FR-009**: The domain and data layers MUST NOT reference UI resources — icons, labels, colours.
- **FR-010**: The presentation-side provider catalogue MUST be removed once nothing needs it.
- **FR-011**: Provider selection MUST be its own navigable destination presented as a bottom sheet.
- **FR-012**: That destination MUST own its state, its mapping to rendered rows, and its state holder.
- **FR-013**: A single table MUST be the only place where a provider identity becomes a name, a mark,
  and other display data; every screen that needs any of it reads that table.
- **FR-014**: The destination MUST return the chosen provider to the login screen, which performs the
  login; dismissal without a choice MUST return nothing.
- **FR-015**: The login screen's state MUST NOT contain a provider list or provider-row model.
- **FR-016**: Every colour used by the login screen MUST be defined in the shared app theme under a
  role-based name.
- **FR-017**: No login UI component MAY declare a colour literal.
- **FR-018**: The transient message's background and text colours MUST be identical in light and dark
  themes. No other colour already on the screen changes value; roles the screen did not previously
  express — the sheet's scrim, border, and drag indicator — are taken from the design.
- **FR-019**: Transient messages MUST be presented through the platform's standard message state
  holder, which owns queueing, one-at-a-time display, and dismissal. Display duration and motion are
  applied by the screen's own host, because the standard renderer exposes neither.
- **FR-020**: A dismissing message MUST leave by upward motion out of view rather than by opacity
  alone, and MUST respect a reduced-motion preference. The reduced-motion preference governs the
  message only; the selection screen keeps the platform's standard open and close animation.
- **FR-021**: Consecutive messages MUST be shown in order without overlap and without silent loss.
- **FR-022**: A written data-layer proposal MUST be delivered and approved before any auth data-layer
  file is modified.
- **FR-023**: The proposal MUST cover, for each candidate, what changes, why the current form is
  excessive, the resulting structure, and the risks or trade-offs.
- **FR-024**: The implemented data-layer simplification MUST preserve login, session renewal, and
  access-token behaviour.
- **FR-025**: Behaviour changes in this feature MUST be introduced test-first, and the repository
  build — compilation, tests, and static analysis — MUST pass, with the iOS target compiled for any
  change that crosses the multiplatform boundary.

### Key Entities

- **Auth provider**: the identity of a sign-in method, carrying whether it is shown and whether it can
  be chosen. Both are set by the roster; neither is a constant. Carries no display data and no
  platform rules.
- **Roster**: the observable source that produces every provider in display order with those two
  flags already decided; the unit presentation subscribes to.
- **Login handler registration**: the declared association between a provider and the login path that
  serves it, read by the single login entry point.
- **Login outcome**: success, cancellation, failure, or unavailability of a login attempt. The last
  is new: it is how a provider with no login path is reported.
- **Provider row**: what the selection screen draws — name, mark, availability — built from the one
  provider-to-display-data table.
- **Transient message**: a short, self-dismissing notice raised by the login screen.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Adding a provider requires zero edits to the login screen and its state holder, and is
  demonstrated by registering one and signing in with it.
- **SC-002**: Zero colour literals remain in login UI components, counted across the login slice.
- **SC-003**: The transient message renders with identical colour values in light and dark themes.
- **SC-004**: Every message dismissal ends with upward motion; none ends by fade alone, except under
  reduced motion, where no motion is expected.
- **SC-005**: The login state holder no longer references any provider collection, catalogue, or
  provider-row type; provider concerns are entirely inside the selection destination and the domain
  source.
- **SC-006**: Every login test that describes unchanged behaviour still passes, and the repository
  build passes on the branch.
- **SC-007**: A person can go from the login screen to a completed Google sign-in in the same number
  of taps as today.
- **SC-008**: No auth data-layer change is committed before the proposal is approved, verifiable from
  the branch history.

## Assumptions

- The provider set stays Apple, Google, and T-ID. Only Google has a working login path today; the
  other two stay visible where they are visible but cannot be signed in with.
- A provider is a sealed type whose members are constructed at runtime rather than an enum, so both
  flags `fix.md` asked for sit on the provider itself and both are still runtime values — the roster
  sets them per device today and a backend can set them later. An enum could carry the flags only as
  constants, which is why the enum shape was rejected.
- The roster emits every known provider in display order, each carrying its own two flags. Nothing
  filters before the selection screen's mapping, which drops the ones marked not shown.
- The selection destination is a full navigable destination with its own key and state holder, and it
  returns its result to the login screen, which owns the login call. This keeps the main state
  holder's single dependency on the login entry point, as the readiness criteria require.
- Returning that result uses the navigation library's own result mechanism rather than a
  project-specific carrier. That mechanism ships only in a pre-release version, so upgrading the
  navigation dependency is in scope for this feature. The upgrade is verified before anything depends
  on it; if it does not build with the dependency-injection or lifecycle libraries, work stops and
  the choice comes back to the requester rather than falling back on its own.
- Transient messages stay anchored at the top of the screen, which is what makes upward exit read as
  leaving; the standard message mechanism is configured accordingly rather than replaced.
- The dark-theme message colour adopts the light-theme value the design has always specified; no
  colour already on the screen changes otherwise.
- Provider marks come from the design, which specifies all three: the multicolour Google mark, the
  Apple mark drawn in the row's text colour, and a yellow rounded badge for T-ID. The initial-letter
  circle currently on screen appears nowhere in the design and is replaced. Because the marks are not
  uniform — two carry brand colours, one follows the theme — the row records which kind it is, and
  the screen renders on that rather than on the provider's identity.
- The design is also the source for the sheet's chrome and the message's placement, shape, timing,
  and motion; values are lifted rather than invented.
- Choosing a provider always closes the selection screen, and the login attempt always runs on the
  login screen, as the design stages it. A provider that cannot be signed in with produces the
  design's "not yet available" wording, which names the provider; the name comes from the same
  provider-to-display-data table the selection screen uses, so no screen gains a per-provider branch.
- A roster with no visible providers is out of scope: the local roster always offers at least Google,
  so the state cannot occur. It becomes reachable only when the roster is fed remotely, and is
  handled then.
- The data-layer proposal is produced within this feature, work pauses for approval, and the approved
  simplification is implemented on this same branch.
- The existing login screen's copy, layout, motion, semantics, and test tags are unchanged except
  where a requirement above states otherwise.
