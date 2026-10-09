# Contract: Scenario API

**Feature ID**: `002-feature-scenario` | **Date**: 2026-10-09

Four authenticated endpoints under `/v1/scenarios`. Authentication is 001's access token;
`401` handling follows the established `ApiError.Unauthorized` path. Serialized types live in
`shared/contract/scenario`; errors reuse `shared/contract/common` `ErrorResponseDto` with the
codes below added to `ApiErrorCode`.

```kotlin
// shared/contract/common — new codes
const val ACCESS_REQUIRED = "access_required"
const val SLOT_LIMIT_REACHED = "slot_limit_reached"
const val NOT_FOUND = "not_found"
```

## Shared DTOs

```kotlin
// shared/contract/scenario — field order follows the wire contract
@Serializable data class ScenarioStateDto(
    val scenarios: List<ScenarioDto>,
    val slotsUsed: Int,
    val slotCapacity: Int,
    val streakDays: Int,
    val practisedDates: List<String>,   // ISO local dates within the requested week
    val practiceSeconds: Long,
)

@Serializable data class ScenarioDto(
    val id: String,
    val title: String,
    val position: Int,
    val free: Boolean,
    val status: String,                 // progress fact: "available" | "active" | "completed"
    val locked: Boolean,                // access fact: paid scenario without access (FR-040/041)
    val attempt: Int?,                  // active only; echoed back in progress reports
    val currentObjective: Int?,         // active only
    val openedAtEpochSeconds: Long?,    // active only; newest-opened drives the Home hero (FR-010)
    val objectiveCount: Int,
    val objectives: List<ObjectiveDto>, // active scenarios only (≤ 5); empty otherwise —
                                        //   keeps the payload flat as the list grows
)

@Serializable data class ObjectiveDto(
    val order: Int,
    val title: String,
    val achieved: Boolean,              // within the current attempt
)

@Serializable data class ReportProgressRequestDto(
    val reportId: String,               // client-generated UUID; idempotency key (one apply)
    val attempt: Int,                   // must match the scenario's current attempt
    val achievedObjective: Int?,        // null: time-only report — never affects the streak
    val localDate: String,              // device-local ISO date (research R4)
    val elapsedSeconds: Long,
)
```

`status` and `locked` are orthogonal: `status` is the user's progress fact, `locked` is the
access fact computed via `AccessPolicy` — a lapsed subscriber's active scenario is
`status = "active", locked = true`, keeps its slot, and leads to the paywall. Grouping on the
Scenarios screen derives from the pair: Активные = active (lock badge when locked),
Доступные = available ∧ ¬locked, По подписке = available ∧ locked, Завершённые = completed.
The client never decides lockedness itself, it only renders it. `practisedDates` carries the dates needed for the week
view; the streak count is server-computed so every device agrees with the merge.

## `GET /v1/scenarios/state?today=YYYY-MM-DD`

The whole aggregate for Home and Scenarios. `today` is the device-local date used as the
streak anchor and week reference (research R4).

| Status | Body | When |
| --- | --- | --- |
| `200` | `ScenarioStateDto` | Always, for an authenticated user (empty progress is a valid state) |
| `400` | `ErrorResponseDto(invalid_request)` | Malformed `today` |
| `401` | `ErrorResponseDto(unauthorized)` | Missing/expired token |

## `POST /v1/scenarios/{id}/activate`

Occupy a slot and make the scenario active (FR-022, FR-030). Idempotent for an already-active
scenario (`200`, no change).

| Status | Body | When |
| --- | --- | --- |
| `200` | `ScenarioStateDto` | Activated (or already active); fresh aggregate returned |
| `401` | `ErrorResponseDto(unauthorized)` | Missing/expired token |
| `403` | `ErrorResponseDto(access_required)` | Non-free scenario without access (FR-040) |
| `404` | `ErrorResponseDto(not_found)` | Unknown scenario id |
| `409` | `ErrorResponseDto(slot_limit_reached)` | Five scenarios already active (SC-004) |

## `POST /v1/scenarios/{id}/repeat`

Confirmed restart of a completed scenario (FR-034): `attempt + 1`, status back to active,
objective 1 current. Subject to the same access and slot rules as activation.

| Status | Body | When |
| --- | --- | --- |
| `200` | `ScenarioStateDto` | Restarted; fresh aggregate returned |
| `401` / `403` / `404` / `409` | as for activate | same rules |
| `409` | `ErrorResponseDto(invalid_request)` | Scenario is not completed |

## `POST /v1/scenarios/{id}/progress`

Progress report for the active attempt — the seam `feature-session` will call
([research.md](../research.md) R11); until then it is exercised by tests and the quickstart.

**Request**: `ReportProgressRequestDto`

Behaviour in one transaction, guarded by the per-user lock (research R5):

1. `reportId` already seen → `200` with the current aggregate, nothing re-applied — a network
   retry cannot double minutes or anything else.
2. `attempt` must equal the scenario's current attempt — a delayed report from a finished
   attempt (e.g. another device reporting after a repeat) is rejected, never applied to the
   new attempt.
3. Achievement insert is idempotent; `current_objective = GREATEST(...)`; the last objective
   completes the scenario and frees the slot (FR-031).
4. `practice_day` is written **only when a new achievement row was inserted** — a time-only
   report (`achievedObjective = null`) or a replayed objective never earns a streak day
   (FR-033).
5. `practice_seconds` accumulates once per `reportId`.

Reports for an inactive scenario, an objective beyond the next, a stale attempt, or a bad
date are answered with `invalid_request` — progress cannot skip ahead, only replays are
tolerated.

| Status | Body | When |
| --- | --- | --- |
| `200` | `ScenarioStateDto` | Recorded (or replayed `reportId`); fresh aggregate returned |
| `400` | `ErrorResponseDto(invalid_request)` | Not active, objective out of order, stale attempt, bad date |
| `401` | `ErrorResponseDto(unauthorized)` | Missing/expired token |
| `404` | `ErrorResponseDto(not_found)` | Unknown scenario id |

## Mobile mapping

`ScenarioRemoteDataSource` speaks these DTOs through `core-network`'s `ApiClient`
(`authenticated = true`); `DefaultScenarioRepository` maps DTO → `ScenarioOverview` and is the
only layer that sees them. The DataStore snapshot is keyed by the signed-in account and
dropped on logout — an offline device never shows another account's progress. Every mutating call returns the fresh aggregate, so the repository
replaces its state and snapshot from the response — no client-side arithmetic on slots,
streak, or status (FR-005 merge stays server-owned).
