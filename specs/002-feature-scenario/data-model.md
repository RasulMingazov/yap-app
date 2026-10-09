# Data Model: Scenarios and Progress

**Feature ID**: `002-feature-scenario` | **Date**: 2026-10-09

Server PostgreSQL is the source of truth; the mobile domain model is a projection delivered
by `GET /v1/scenarios/state` and snapshotted for offline reads ([research.md](research.md)
R2, R3).

## Server schema (`V2__scenario.sql`)

```sql
scenario                 -- the scenario list, seeded in the migration (R1)
  id            TEXT PK            -- stable slug, e.g. 'cafe-visit'
  title         TEXT NOT NULL      -- Russian display title
  position      INT  NOT NULL UNIQUE
  is_free       BOOLEAN NOT NULL DEFAULT FALSE   -- exactly one TRUE in the seed

scenario_objective
  scenario_id   TEXT REFERENCES scenario
  objective_order INT NOT NULL     -- 1-based, sequential
  title         TEXT NOT NULL
  PRIMARY KEY (scenario_id, objective_order)

user_scenario            -- one row per user per scenario ever touched
  user_id       UUID REFERENCES accounts
  scenario_id   TEXT REFERENCES scenario
  status        TEXT NOT NULL      -- 'active' | 'completed'
  attempt       INT  NOT NULL DEFAULT 1          -- repeat = attempt + 1 (R10)
  current_objective INT NOT NULL DEFAULT 1
  opened_at     TIMESTAMPTZ NOT NULL             -- newest active drives the Home hero
  completed_at  TIMESTAMPTZ NULL
  PRIMARY KEY (user_id, scenario_id)

user_objective           -- insert-only achievement facts (R5, FR-005)
  user_id       UUID
  scenario_id   TEXT
  attempt       INT
  objective_order INT
  achieved_on   DATE NOT NULL      -- device-local date as reported (R4)
  PRIMARY KEY (user_id, scenario_id, attempt, objective_order)

practice_day             -- streak facts; never deleted (R4, R10)
  user_id       UUID
  local_date    DATE
  PRIMARY KEY (user_id, local_date)

user_stats               -- also the per-user lock anchor for activation (R5)
  user_id       UUID PK
  practice_seconds BIGINT NOT NULL DEFAULT 0     -- minutes = seconds / 60, floor (R11)

progress_report          -- idempotency ledger for progress reports (R11)
  user_id       UUID
  report_id     UUID
  received_at   TIMESTAMPTZ NOT NULL
  PRIMARY KEY (user_id, report_id)
```

### Invariants (transactional, Testcontainers-verified)

- `COUNT(user_scenario WHERE status = 'active') <= 5` per user — counted after taking the
  per-user row lock (`SELECT … FROM user_stats WHERE user_id = ? FOR UPDATE`, row upserted
  first), so two concurrent activations serialize and exactly one sixth is refused with
  `slot_limit_reached` (FR-030, SC-004). A plain count-then-insert at READ COMMITTED admits
  the race.
- Progress reports are applied at most once: `(user_id, report_id)` is recorded in
  `progress_report`; a replay answers with the current aggregate and changes nothing —
  minutes cannot double on a network retry.
- A report's `attempt` must equal `user_scenario.attempt`; a delayed report from a previous
  attempt is rejected, never applied to the new one.
- `user_objective` inserts are idempotent (`ON CONFLICT DO NOTHING`); `current_objective`
  only ever increases within an attempt — `GREATEST(current, achieved + 1)` (FR-005).
- `practice_day` is written only in the same transaction that inserted a **new** achievement
  row — time-only and replayed reports never earn a streak day (FR-033).
- Achieving the last objective sets `status = 'completed'`, stamps `completed_at`, frees the
  slot in the same transaction (FR-031).
- Activation of a non-free scenario passes `AccessPolicy.hasAccess(userId)` first —
  `FreeOnlyAccessPolicy` answers false until `feature-subscription` (R6); refusal answers
  `access_required`.

### Derived values (computed per state request, never stored)

- **Streak** — run of consecutive `practice_day` dates anchored at `today` when today has a
  date, otherwise at `today - 1`; zero only when neither exists. A user who practised
  yesterday sees their run all morning, with "one objective today keeps it" (FR-014); the
  week view marks the dates of the client's current week (FR-033).
- **Slots** — `used = COUNT(active)`, capacity fixed at 5.
- **Scenario status and lockedness** — orthogonal: `status` (`available`/`active`/
  `completed`) comes from `user_scenario`; `locked` is computed per request from
  `AccessPolicy` for non-free scenarios. An active scenario of a lapsed subscriber stays
  `active` **and keeps its slot**, rendered locked until access returns (FR-041); the
  `status` column never changes on access transitions.

## Mobile domain model (`feature-scenario`)

```text
ScenarioOverview                        # the one observed aggregate (R2)
├── scenarios: List<Scenario>           # list order = position
│     Scenario(id, title, objectiveCount, objectives: List<Objective>, isFree, status, locked)
│       status: ScenarioStatus = Available | Active(attempt, currentObjective) | Completed
│       locked: Boolean                     # access fact, orthogonal to status (R12)
│       Objective(order, title, achieved)   # populated for active scenarios only (≤ 5)
├── lastOpened: Scenario?               # drives the Home hero (FR-010)
├── slots: SlotUsage(used, capacity=5)
├── streak: Streak(days, weekDays: List<WeekDay(date, practised, isToday)>)
└── practiceMinutes: Int
```

State of the repository exposure (FR-004, FR-006):

```text
OverviewState = Loading            # no snapshot yet, fetch in flight  → progress indicator
             | Unavailable         # no snapshot, fetch failed         → full-screen retry
             | Ready(overview, isRefreshing)   # snapshot or fresh data → content
```

## Scenario lifecycle (per user)

```text
            activate (slot free, access ok)             last objective achieved
 Available ────────────────────────────────▶ Active ────────────────────────────▶ Completed
   ▲                                           │  ▲                                  │
   │                                           │  └─ objective achieved:             │
   │                                           │     current_objective += 1          │
   └───────────── repeat confirmed: attempt += 1, current = 1 ◀──────────────────────┘

 locked is the orthogonal access axis, not a status:  unlocked ⇄ locked on access change,
 at any status — status, progress, and the occupied slot never move (FR-041, R12).
 Active + locked renders in Активные with a lock and leads to the paywall;
 Available + locked activates only through the paywall.
```

## Wire DTOs

Defined in [contracts/scenario-api.md](contracts/scenario-api.md); `shared/contract/scenario`
holds `ScenarioStateDto` and its parts plus `ReportProgressRequestDto` — nothing else crosses
the wire.
