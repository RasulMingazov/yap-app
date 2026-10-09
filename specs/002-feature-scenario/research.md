# Research: Scenarios and Progress

**Feature ID**: `002-feature-scenario` | **Date**: 2026-10-09

No NEEDS CLARIFICATION markers remained in the Technical Context; the decisions below resolve
the open design choices the spec left to planning.

## R1 — Scenario content lives on the server

**Decision**: The scenario list (titles, order, objective definitions, the free flag) is server
data, seeded by the feature's Flyway migration and served inside `GET /v1/scenarios/state`.

**Rationale**: The paywall promise "19 новых сценариев и все следующие" means content must
grow without an app release. The payload is tiny (20 × 6 rows). Conversation prompts for the
AI are *not* part of this list — they belong to `feature-session`'s content.

**Alternatives**: bundling content in the app (rejected: release-coupled, and the server
would still need the list to validate progress); a CMS/config service (rejected: a third
system for 20 rows).

## R2 — One state endpoint, one domain aggregate

**Decision**: A single authenticated `GET /v1/scenarios/state` returns the scenario list + per-user
progress + slots + streak + minutes as one `ScenarioStateDto`. Mobile maps it to one
`ScenarioOverview` that both screens (and later Profile) observe.

**Rationale**: Home and Scenarios render different projections of the same facts; two
endpoints would race each other into inconsistent screens (slot count on Home vs rows on
Scenarios). One aggregate also makes the FR-006 cache rule binary: the snapshot either
exists or it does not.

**Alternatives**: per-screen endpoints (rejected above); GraphQL-style partial queries
(rejected: nothing in the stack needs it at this size).

**Scale check**: objectives travel only for active scenarios (≤ 5), other rows carry just
`objectiveCount`, so the payload stays ~20–25 KB even at 200 scenarios. Search over titles is
client-side filtering of the cached list — instant and offline-capable. The split point, if
the list ever outgrows one document (thousands of rows, server-side search/pagination), is
isolated in the data layer: a separate list endpoint slots in behind `ScenarioRepository`
without touching domain or presentation.

## R3 — Offline cache is a DataStore snapshot, stale-while-revalidate

**Decision**: `DefaultScenarioRepository` keeps `ScenarioOverview` in a `StateFlow`, persists
the last successful state as JSON in plain DataStore, emits the snapshot immediately on start,
and refreshes from the network on screen entry. Loading with no snapshot → `Loading`;
failure with no snapshot → `Unavailable` (full-screen retry, FR-006); snapshot present →
content, refresh failures stay silent.

**Rationale**: DataStore is already in the stack (001 stores the session in it); the payload
is one small document, exactly a snapshot's shape. No relational queries happen on device, so
adding SQLDelight would be dead weight. The snapshot is keyed by account id and dropped on
logout, so a device shared between accounts never leaks one user's progress to another.

**Alternatives**: SQLDelight (rejected: unneeded query power, new dependency against the
pinning rule); in-memory only (rejected: fails FR-004).

## R4 — Streak days are client-reported local dates

**Decision**: Every progress report carries the device-local date (`YYYY-MM-DD`). The server
stores these dates as opaque facts in `practice_day` and computes the streak as the run of
consecutive dates ending at the client's "today", which the state request passes as a query
parameter. The server never converts time zones.

**Rationale**: The clarified rule is device-local midnight (FR-033). Only the device knows
its zone at the moment of practice; storing resolved local dates makes the merge trivial
(set union) and keeps repeats from double-counting a day (unique constraint). Display
anchors at `today` when today already has a date and at `today - 1` otherwise, so an
existing run stays visible all morning instead of dropping to 0 before the first objective.

**Alternatives**: UTC boundaries (rejected in clarification); storing a profile time zone
(rejected: wrong after travel; more state to maintain).

## R5 — Slot limit and merge are transactional server rules

**Decision**: Every progress-mutating transaction (activate, repeat, report) first takes a
per-user row lock — `SELECT … FROM user_stats WHERE user_id = ? FOR UPDATE` (row upserted on
first use) — then counts active rows and rejects the sixth activation with
`slot_limit_reached`. Objective achievement is an idempotent insert keyed
`(user_id, scenario_id, attempt, objective_order)`; replays and out-of-order reports cannot
move progress backwards (FR-005). Both behaviors get Testcontainers tests per
`docs/testing/003-backend-integration.md`.

**Rationale**: SC-004 is an invariant, and the client cannot enforce it (two devices). A
plain count-then-insert at READ COMMITTED admits the race — two parallel requests both see
four active rows and both insert. One ordinary row lock serializes a single user's
mutations (zero cross-user contention) and also gives reports a stable order. The
constitution requires these semantics to be verified against a real database.

**Alternatives**: client-side enforcement only (rejected: trivially violated); SERIALIZABLE
with retry (rejected: retry loops for a problem one lock removes); five physical slot rows
with a unique constraint (rejected: more schema for the same guarantee).

## R6 — Seams for features that do not exist yet

**Decision**: The scenario domain owns three ports with placeholder adapters in `impl`:
`SessionGateway` (start/continue practice — placeholder opens a "скоро" sheet and reports
nothing), `AccessGateway` (observe access — placeholder emits Free), `PaywallGateway` (open
paywall — placeholder sheet explaining the subscription is coming). Server-side, an
`AccessPolicy` port defaults to `FreeOnlyAccessPolicy`. When `feature-session` /
`feature-subscription` land, they provide real adapters through their `api` modules and the
Koin/app graph rebinds; the ports and every consumer stay untouched.

**Rationale**: Same pattern 001 used for Apple/T-ID ("coming soon"); keeps this slice
shippable and testable end to end without inventing modules that own no behavior.

**Alternatives**: creating `feature-session:api`/`feature-subscription:api` now (rejected:
speculative contracts, empty modules); blocking this feature on those features (rejected:
inverts the roadmap).

## R7 — Analytics is a `core-common` port with a logging sink

**Decision**: `AnalyticsTracker.track(event: AnalyticsEvent)` in `core-common`;
`AnalyticsEvent` is a name plus string parameters. `shared-app` binds a logging
implementation. The seven FR-050 names are constants in the feature.

**Rationale**: Every Notion feature page carries a mandatory event table, so the port is
process-wide infrastructure by the second feature — the documented promotion criterion. No
vendor is chosen yet; the port keeps that decision open.

**Alternatives**: feature-owned port (rejected: duplicated immediately); adopting a vendor
SDK now (rejected: a product decision nobody has made).

## R8 — The tab shell lives in `app-root`

**Decision**: `MainPlaceholderScreen` becomes `MainScaffold`: the design's floating
three-tab bar, one back stack per tab (Navigation 3 multiple back stacks per the
`navigation-3` skill), re-tap scrolls to top. `HomeNavKey` and `ScenariosNavKey` live in
`feature-scenario:api` and their destinations in the feature's Koin module; the Profile tab
renders an `app-root` placeholder until `feature-profile`.

**Rationale**: `app-root` already owns the back-stack base and the logged-in swap; the shell
is cross-feature navigation chrome, which no single feature may own. Destinations stay with
their owner per `docs/mobile/003-dependency-injection.md`.

**Alternatives**: shell inside `feature-scenario` (rejected: it would own Profile's tab);
a `core-design` scaffold component holding navigation (rejected: `core-*` must stay
navigation-free except the declared contracts).

## R9 — Palette tokens are lifted from the design file, not guessed

**Decision**: Extend `YapColors` with the `feature_main.dc.html` theme dictionaries: dark
`bg #08070A`, `fg #FAF9F6`, `muted #8F8899`, `faint #6B6675`, card `#1A1920`, sheet
`#16151A`, accent fill `#D9FF57` on ink `#0B0A0D`; light `bg #FFFFFF`, `fg #0B0A0D`,
`muted #5F5A6B`, CTA black-on-lime inversion, plus hairline/track/pill/error pairs as drawn.
The FR-006 progress indicator uses the accent: lime in dark, black (`#0B0A0D`) in light.

**Rationale**: The design project is the palette's source of truth (established practice
from 001's refresh commit); both themes are fully specified in the prototype's `theme()`.

**Alternatives**: none worth recording — guessing colors is explicitly ruled out.

## R10 — Repeat is an attempt increment, not a delete

**Decision**: `user_scenario` carries `attempt INT`; repeating confirms, increments
`attempt`, sets status back to `active` with objective 1 current, and leaves prior
achievement rows and all `practice_day`/minutes untouched. "Result and error review" reset
(FR-034) falls out for free: they are attempt-scoped by key.

**Rationale**: Satisfies FR-034's keep-streak/keep-minutes rule with no compensating
deletes; preserves history for future "история диалогов" (Profile row already shown in the
design).

**Alternatives**: deleting attempt rows (rejected: destroys history and risks cascading
into streak facts); separate "completed archive" table (rejected: the attempt key is the
archive).

## R11 — Minutes ride on progress reports

**Decision**: `POST /v1/scenarios/{id}/progress` carries `elapsedSeconds` alongside the
achieved objective (or alone, for a session that ended without an achievement), plus two
guards: a client-generated `reportId` (UUID) recorded in `progress_report` so a network
retry is applied at most once — minutes cannot double — and the `attempt` the report belongs
to, so a delayed report from a finished attempt is rejected instead of polluting the next
one. A time-only report never touches `practice_day` (the streak is earned by objectives
alone, FR-033). The server accumulates into `user_stats.practice_seconds`; display rounds
to minutes.

**Rationale**: Minutes are session facts owned by `feature-session` eventually; giving them
their own endpoint now would freeze a contract for a feature that doesn't exist. One report
call is what the placeholder `SessionGateway` would have produced anyway.

**Alternatives**: separate minutes endpoint (rejected above); client-side accumulation
(rejected: FR-003 binds minutes to the account across devices).

## R12 — Lapsed access freezes active scenarios in place

**Decision**: When access lapses, a paid active scenario keeps `status = 'active'`, its
slot, and its attempt progress; only the derived `locked` flag flips. The UI shows it in
Активные with a lock, tapping leads to the paywall; resubscribing resumes it untouched. The
`status` column never changes on access transitions.

**Rationale**: FR-041 demands progress retention and seamless resume; freeing the slot on
lapse would either lose the "which 5" choice or overflow the limit on resubscribe. One
orthogonal pair — progress fact (`status`) × access fact (`locked`) — expresses every state
the single enum could not.

**Alternatives**: freeing slots on lapse (rejected above); a combined `locked_active`
status value (rejected: multiplies the enum and still loses the orthogonality).
