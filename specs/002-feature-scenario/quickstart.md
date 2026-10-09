# Quickstart: Scenarios and Progress

**Feature ID**: `002-feature-scenario` | **Date**: 2026-10-09

Validation guide — proves the slice works end to end. Contracts:
[scenario-api.md](contracts/scenario-api.md); model: [data-model.md](data-model.md).

## Prerequisites

- JDK 17, Android SDK (compileSdk 37), Docker running (Testcontainers).
- `git config core.hooksPath .githooks` once per clone.
- Server env: the 001 variables plus nothing new — `V2__scenario.sql` migrates and seeds on
  boot.

## Build and automated verification

```bash
./gradlew build                                               # required gate: compile, tests, Detekt
./gradlew :apps:mobile:shared-app:compileKotlinIosSimulatorArm64   # KMP boundary changed
./gradlew :services:server:feature-scenario:test                   # Testcontainers suite alone
```

The server suite must cover (red-first per constitution III): slot limit under two
concurrent activations — exactly one `409`, serialized by the per-user lock; replayed
`reportId` changes nothing (minutes not doubled); time-only report adds minutes but no
streak day; stale-`attempt` report rejected after a repeat; out-of-order report rejected;
completion frees the slot in the same transaction; repeat increments attempt and preserves
`practice_day`/minutes; streak anchored at yesterday until today's first objective, across
a gap, and at the week edge.

## API walkthrough (server running locally)

```bash
TOKEN=...   # from the 001 login flow
B=http://localhost:8080/v1/scenarios

curl -H "Authorization: Bearer $TOKEN" "$B/state?today=2026-10-09"       # 20 scenarios, 1 free
curl -X POST -H "Authorization: Bearer $TOKEN" $B/cafe-visit/activate    # 200, slotsUsed=1
curl -X POST -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"reportId":"5f0c…","attempt":1,"achievedObjective":1,"localDate":"2026-10-09","elapsedSeconds":180}' \
  $B/cafe-visit/progress                                                 # 200, streakDays=1
curl -X POST -H "Authorization: Bearer $TOKEN" $B/job-interview/activate # 403 access_required
```

Expected: the sixth distinct activation answers `409 slot_limit_reached`; achieving the last
objective flips the scenario to `completed` and `slotsUsed` drops; re-sending the same
`progress` body (same `reportId`) changes nothing — `practiceSeconds` and `streakDays`
included.

## App walkthrough

```bash
./gradlew :apps:mobile:android-app:installDebug   # or open the Xcode host for iOS
```

1. Log in (001 flow) → the tab shell appears: Главная active, Сценарии, Профиль (заглушка).
2. Fresh account: hero offers the free scenario, step 1, «Начать»; streak block shows 0.
3. Tap «Начать» → the placeholder session sheet («скоро») — the `SessionGateway` seam.
4. Сценарии tab: groups Доступные (1 free) / По подписке (19, lock); filters Все + groups;
   `scenarios_view` and filter events visible in the debug analytics log.
5. Tap a locked scenario → placeholder paywall sheet, `locked_content_click` logged.
6. Report progress via the API walkthrough above, pull the app to foreground → Home hero
   switches to «Продолжить» with «Шаг 2 из N»; streak shows 1 with today marked.
7. Airplane mode, relaunch → both screens render the cached state; «Продолжить» explains
   that practice needs a connection (FR-004).
8. Fresh install, server stopped, login impossible offline — instead: server up, log in,
   stop server before first state load → full-screen error with «Повторить» (FR-006); start
   server, retry → content.

## Done means

`./gradlew build` green, the Testcontainers suite green, and walkthrough steps 1–8 observed
on a device/emulator. Checklist `checklists/requirements.md` stays all-pass; deviations land
in the PR description per constitution V.
