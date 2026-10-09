# Feature Specification: Scenarios and Progress

**Feature Branch**: `feature/002-feature-scenario` | **Feature ID**: `002-feature-scenario`

**Created**: 2026-10-09

**Status**: Draft

**Input**:

- Notion product page "1. Сценарии и прогресс"
  (https://app.notion.com/p/0ccdc7d20da48275a6f801f78480d961) and its child
  "Сценарий 1. Small talk без неловкости"
- The Claude Design project screens `feature_main.dc.html` with `tab_home.dc.html` and
  `tab_catalog.dc.html`
  (https://claude.ai/design/p/0baa8de1-eb4c-4522-90e4-b1fee4009f4a?file=feature_main.dc.html)

`feature-scenario` owns scenarios, objective definitions, per-scenario progress, active slots,
the practice streak, and accumulated practice minutes. Screens: **Home** (Главная) and
**Scenarios** (Сценарии), plus the repeat-confirmation dialog. In the UI an objective is
shown to the user as a step («Шаг X из N», «k из n шагов»).

**Out of scope, integration points only**: the practice conversation itself (`feature-session`
runs the `PracticeSession` and reports objective results back), purchase and paywall
(`feature-subscription` owns the access status; this feature only reads it and navigates to the
paywall), the Profile tab (`feature-profile`, which reads the same progress figures).

## Clarifications

### Session 2026-10-09

- Q: Что доступно пользователю без сети — можно ли открыть Главную и Сценарии оффлайн, и что
  происходит при попытке начать или продолжить практику? → A: Экраны показывают закешированный
  прогресс оффлайн; старт/продолжение практики требует сети, с понятным сообщением.
- Q: По какому времени определяется «календарный день» для стрика? → A: По локальному времени
  устройства — день меняется в полночь там, где находится пользователь.
- Q: Как разрешается конфликт прогресса при занятиях с двух устройств? → A: Монотонное
  слияние: достигнутая цель не отзывается, при расхождении берётся наибольший прогресс по
  каждому сценарию, стрик и минуты объединяются.
- Q: Что видит пользователь, если прогресс не загрузился при наличии сети, а кеша ещё нет? →
  A: Полноэкранную ошибку с кнопкой «Повторить»; при наличии кеша показывается кеш.
- Q: Что показывается во время загрузки прогресса? → A: Простой индикатор загрузки
  (progress bar): лаймовый акцент в тёмной теме, чёрный в светлой.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Continue or start practice from Home (Priority: P1)

A person opens the app and lands on Home. One primary action takes them into practice: a new
user is offered step 1 of the free scenario ("Начать"); a user with an open scenario sees that
scenario's title, «Шаг X из N», its step progress, and continues exactly where they left off
("Продолжить").

**Why this priority**: daily return to practice is the core loop; everything else supports it.

**Independent Test**: with a fresh account, launch → start the free scenario → leave
mid-session → relaunch → continue lands on the same objective.

**Acceptance Scenarios**:

1. **Given** a user who has never practised, **When** Home opens, **Then** the hero offers the
   free scenario at step 1 with a start action, and no active-scenarios row is shown.
2. **Given** a user with an unfinished scenario, **When** Home opens, **Then** the hero names
   that scenario, shows «Шаг X из N» with per-step progress, and its action continues the
   session at the current objective.
3. **Given** the hero's continue action is activated, **When** the practice session opens,
   **Then** it resumes at the saved objective (`continue_click` is recorded).
4. **Given** a user exits a session before finishing, **When** they return to Home, **Then**
   the hero and the scenario's card both reflect the saved objective — nothing is lost.

---

### User Story 2 - Pick a scenario on the Scenarios screen (Priority: P2)

A person opens the Scenarios tab and sees every scenario grouped by status: Active (with slot
usage "n / 5"), Available, By subscription, Completed. A filter rail (Все / per-group) narrows
the list. Tapping an active scenario continues it; tapping an available one activates it —
occupying a free slot — and starts practice at its first objective.

**Why this priority**: the Scenarios screen is the only place to start anything beyond the
hero and makes the 20-scenario programme visible.

**Independent Test**: with a subscribed user, open Scenarios, apply each filter, activate a
new scenario, observe it occupy a slot and appear under Active on both Scenarios and Home.

**Acceptance Scenarios**:

1. **Given** the Scenarios screen opens (`scenarios_view`), **When** the user reads it,
   **Then** scenarios appear in the groups Active, Available, By subscription, Completed — a
   group is hidden when empty — and each row states its status: progress «k из n шагов» for active, "Бесплатно" for
   the free scenario, a lock for subscription-only, a done mark for completed.
2. **Given** the filter rail, **When** a filter is selected (`scenarios_filter_select`),
   **Then** only that group's rows remain; "Все" restores all groups with headers and counts.
3. **Given** a subscribed user with a free slot, **When** they tap an available scenario,
   **Then** it becomes active, the slot count increases, and practice opens at objective 1
   (`scenario_open`).
4. **Given** a free user, **When** they tap the free scenario from Available, **Then** practice
   opens without any purchase step.
5. **Given** an active scenario row, **When** tapped, **Then** its session continues at the
   current objective (`scenario_open`).

---

### User Story 3 - Completion updates progress, slot, streak, and minutes (Priority: P3)

Achieving an objective updates progress and unlocks the next one; achieving the last
objective completes the scenario, frees the slot, and moves the scenario to Completed. Home's
streak reflects the practice; minutes accumulate in the feature's state (surfaced by Profile
later).

**Why this priority**: progression is the product's promise; without it scenarios are a static
list.

**Independent Test**: finish the last objective of a scenario and verify — slot freed, scenario
listed under Completed, streak day marked, minutes increased, hero switched to the next
relevant state.

**Acceptance Scenarios**:

1. **Given** a session reports an objective achieved, **When** the user returns to this
   feature's screens, **Then** the scenario's current objective is the next one and its step
   progress reflects it.
2. **Given** the last objective is achieved, **When** the scenario completes, **Then** its
   slot is freed, it appears under Completed, Home's streak reflects the earned day, and the
   accumulated minutes grow in the feature's state.
3. **Given** all 20 scenarios are completed, **When** Home opens, **Then** the hero states the
   programme is done and leads to the Scenarios screen to repeat.
4. **Given** a user completed at least one objective today, **When** they view the streak
   block, **Then** today is marked, the consecutive-day count is shown, and the note confirms
   the streak is safe; a user who hasn't earned today yet still sees yesterday's run with a
   note that one achieved objective today keeps it.

---

### User Story 4 - Locked scenario leads to subscription (Priority: P4)

A free user who finished the free scenario sees Home switch to a promo state: the hero invites
them to unlock the remaining 19 scenarios and a "Откроется с подпиской" row previews locked
ones. Tapping any locked scenario — on Home or on the Scenarios screen — records the interest and
hands over to the paywall; after a successful purchase the chosen scenario opens without the user
navigating again.

**Why this priority**: this is the conversion path, but it depends on the free loop existing
first.

**Independent Test**: as a free user with the free scenario completed, tap a locked scenario,
complete a (simulated) purchase, observe the scenario open directly.

**Acceptance Scenarios**:

1. **Given** a free user with the free scenario completed, **When** Home opens, **Then** the
   hero promotes the subscription and the locked-preview row is shown.
2. **Given** any locked scenario is tapped, **Then** `locked_content_click` is recorded and the
   paywall opens.
3. **Given** the paywall was opened from a locked scenario, **When** the purchase succeeds,
   **Then** that scenario is activated and its practice opens immediately. *In this slice the
   paywall is a placeholder: verified at the seam — the paywall request carries the scenario
   to resume; the end-to-end check lands with `feature-subscription`.*
4. **Given** access lapses (period ended after cancellation), **When** the user returns,
   **Then** subscription-only scenarios are locked again, their progress is retained, and
   continuing them resumes where they stopped once access is restored.

---

### User Story 5 - Repeat a completed scenario (Priority: P5)

A user taps a completed scenario and is asked to confirm a restart: the scenario returns to its
first objective and its previous result and error review are discarded, while the streak and
practice minutes are kept. The free scenario can be repeated without a subscription.

**Why this priority**: keeps the product useful after completion; a free-user retention lever.

**Independent Test**: complete the free scenario, tap it under Completed, confirm the restart,
verify objective 1 is current and streak/minutes unchanged.

**Acceptance Scenarios**:

1. **Given** a completed scenario is tapped, **When** the confirmation appears, **Then** it
   states what is reset (that scenario's result and error review) and what is kept (streak,
   minutes), and offers restart or cancel.
2. **Given** the restart is confirmed, **Then** the scenario becomes active at objective 1 and
   occupies a slot; cancelling changes nothing.
3. **Given** a free user, **When** they repeat the free scenario, **Then** no purchase step
   appears.

---

### Edge Cases

- All 5 slots are occupied: available scenarios state "Нет свободного слота", tapping one
  explains the limit without activating (`slot_limit_reached`); Home shows the full-slots
  notice instead of the add-slot card. Repeating a completed scenario follows the same rule.
- A subscriber has no active scenario and the programme is not finished: the hero invites them
  to pick a scenario on the Scenarios screen.
- The streak is about to break (no objective achieved today, promo state): the streak note
  warns that the series ends tomorrow.
- Progress must survive app restart, reinstall with the same account, and switching devices.
- Access status changes mid-use (purchase, cancellation, lapse elsewhere in the app): Home and
  Scenarios reflect the new status on next appearance without restart.
- No network: both screens render the last known progress; only the start/continue actions
  explain that practice needs a connection (FR-004).
- Two devices practise the same account in parallel or sync late: the merged result keeps
  the furthest objective per scenario and every earned streak day and minute (FR-005).
- First launch with a server error: full-screen error with retry — the screens never invent
  an empty "practice not started" state from unloaded data (FR-006).
- Logging out and into another account: the previous account's offline copy is gone; the new
  account starts from Loading, never from someone else's progress (FR-004).

## Requirements *(mandatory)*

### Functional Requirements

**Domain**

- **FR-001**: A scenario describes one real-life situation and contains 5–6 sequential
  objectives; the launch content set holds 20 scenarios of which exactly one is free.
- **FR-002**: Each user's progress is stored per scenario and per objective; one pass of a
  scenario corresponds to one practice session run by `feature-session`, which reports
  objective achievement back to this feature. Every active scenario keeps its own unfinished
  session, and opening that scenario resumes exactly that session.
- **FR-003**: Progress, streak, and minutes are bound to the account and persist across
  restarts, reinstalls, and devices.
- **FR-004**: Without a network connection Home and Scenarios open and show the last known
  progress; starting or continuing practice requires a connection and, when offline, is
  declined with a clear message instead of a broken session. The offline copy is bound to
  the signed-in account and cleared on logout — another account's progress is never shown.
- **FR-005**: Progress only moves forward. An achieved objective is never revoked by sync;
  when devices diverge, each scenario keeps the furthest progress reached anywhere, and
  streak days and minutes are merged so nothing earned is lost. Conflicts resolve without
  user involvement.
- **FR-006**: While progress is loading with no cached copy yet, Home and Scenarios show a
  plain progress indicator — the lime accent in the dark theme, black in the light theme,
  matching the design palette. When loading fails and no cached copy exists (e.g. first
  launch), they show a full-screen error with a retry action; once any cached progress
  exists, the screens show it instead of the indicator or the error.

**Home**

- **FR-010**: Home presents exactly one state-dependent hero: start the free scenario (never
  practised), continue the last open scenario with «Шаг X из N» and step progress, subscribe
  promo (free scenario done, no access), pick-from-Scenarios (subscriber with no active
  scenario), or programme-done leading to the Scenarios screen.
- **FR-011**: Home lists active scenarios (newest-opened highlighted) as cards with name, next
  objective number, and per-objective progress; tapping a card continues that scenario.
- **FR-012**: Alongside active cards Home shows remaining slot capacity: an add card with the
  free-slot count opening the Scenarios screen, or — when all slots are taken — a notice that one
  scenario must be completed first.
- **FR-013**: For a free user who completed the free scenario, Home shows a locked-scenarios
  preview row; the row and the hero promo lead to the paywall.
- **FR-014**: Home shows the weekly streak: consecutive-day count, the current week with
  practised days marked and today highlighted, and a contextual note (series safe / one
  objective needed / series at risk). Until today's first objective the displayed count is
  the run ending yesterday — it reads 0 only when yesterday earned nothing either; the count
  includes today once an objective is achieved today.

**Scenarios screen**

- **FR-020**: The Scenarios screen lists every scenario grouped as Active (with "n / 5" slot usage),
  Available, By subscription, Completed; empty groups are hidden; each row carries the status
  meta described in User Story 2.
- **FR-021**: A filter rail offers "Все" plus one filter per non-empty group, each with a
  count; filtering shows only that group, "Все" shows all groups with headers.
- **FR-022**: Row actions: active → continue; available free scenario → open; available with
  access and a free slot → activate and open; locked → paywall with the scenario opening after
  successful purchase; available without a free slot → explain the limit; completed → repeat
  confirmation.

**Slots and progression**

- **FR-030**: At most 5 scenarios are active at once; activation occupies a slot, completion
  frees it; the limit can never be exceeded.
- **FR-031**: Achieving an objective updates the scenario's progress and makes the next
  objective current; achieving the last one completes the scenario and frees its slot.
- **FR-032**: Leaving a session early keeps the current objective; continuing resumes there.
- **FR-033**: A streak day is earned by achieving at least one objective in any scenario that
  calendar day — finishing the whole session is not required. Further objectives the same day
  do not increase the streak again; a day with no objective achieved resets the count to 0.
  The day boundary is midnight in the device's local time zone.
- **FR-034**: Repeating a completed scenario requires confirmation, resets only that scenario's
  objectives, result, and error review, and keeps streak and minutes.

**Access**

- **FR-040**: This feature reads a single access status owned by `feature-subscription` and
  never changes it; without access only the free scenario is playable, with access (active or
  cancelled-until-period-end) all scenarios are.
- **FR-041**: When access lapses, subscription-only scenarios lock again with progress
  retained; active ones stay in their slots, shown locked, and tapping them leads to the
  paywall. Restored access resumes them where they stopped — slots and progress unchanged.

**Analytics**

- **FR-050**: The feature records: `home_view` (Home shown), `scenarios_view` (Scenarios
  screen shown), `scenarios_filter_select` (filter chosen), `continue_click` (hero continue),
  `scenario_open`
  (a scenario's practice opened), `slot_limit_reached` (activation refused by the limit),
  `locked_content_click` (locked scenario tapped).

### Key Entities

- **Scenario**: one life situation; title, objective list, free/subscription flag.
- **ObjectiveDefinition**: one goal inside a scenario — ordered, meaning-checkable, shown as
  a step.
- **ObjectiveState**: a user's completion state for one objective.
- **Active scenario (slot)**: a scenario the user is progressing through; keeps its own
  unfinished session and current objective; at most 5 per user; the most recently opened one
  drives the Home hero.
- **Streak**: consecutive calendar days (device-local midnight boundary) on which at least
  one objective was completed; a missed day resets it.
- **Practice minutes**: accumulated session time reported by `feature-session`.
- **Access status** *(external, read-only)*: free / active / cancelled-with-end-date, owned by
  `feature-subscription`.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A returning user reaches their unfinished practice in at most 2 taps from app
  launch.
- **SC-002**: Starting any permitted scenario from the Scenarios screen takes at most 3 taps from app
  launch.
- **SC-003**: 100% of objective, streak, and minutes progress survives restart and reinstall
  with the same account.
- **SC-004**: The number of simultaneously active scenarios never exceeds 5 in any user state.
- **SC-005**: Every user action listed in FR-050 produces its analytics event exactly once.
- **SC-006**: From Home alone — no navigation — a user can state their streak, slot usage, and
  what they will practise next.

## Assumptions

- Tapping an available scenario both activates it and opens its practice session immediately;
  the prototype's "added to active" toast is prototype shorthand (confirmed by the product
  owner, 2026-10-09).
- The subscriber-with-no-active-scenario hero (FR-010, pick-from-Scenarios) is not drawn in
  the prototype; its content follows the start-free hero pattern but points to the Scenarios
  screen.
- Practice minutes come from `feature-session` session duration; this feature only accumulates
  and displays them.
- Scenario content (20 scenarios, objectives, AI behaviour) is authored elsewhere; the AI
  conversation rules in the Notion child page belong to `feature-session` content and are out
  of scope here.
- Screen titles and copy are Russian as drawn in the prototype, except that objectives are
  worded as «шаг» («Шаг 2 из 5») — the prototype's «урок» is superseded by the product
  decision of 2026-10-09; the tab bar order is Главная, Сценарии, Профиль, with Сценарии
  opening this feature's Scenarios screen.

## Dependencies

- `feature-session` — runs the practice conversation, reports objective achievement, early
  exit, and session minutes.
- `feature-subscription` — provides the access status and the paywall destination, and invokes
  the pending "open purchased scenario" continuation after purchase.
- `feature-auth` (001) — an authenticated account to bind progress to.
- Scenario content set — 20 scenarios with 5–6 objectives each, one marked free.
