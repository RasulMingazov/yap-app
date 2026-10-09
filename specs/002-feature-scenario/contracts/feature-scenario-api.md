# Contract: `feature-scenario` mobile surface and seams

**Feature ID**: `002-feature-scenario` | **Date**: 2026-10-09

## `api` module — what other features may see

```kotlin
package app.yap.feature.scenario.api

data object HomeNavKey : NavKey
data object ScenariosNavKey : NavKey

// entity/ — ScenarioOverview, Scenario, ScenarioStatus, Objective, Streak, SlotUsage
//   (shapes in data-model.md; immutable value types, no DTO leaks)

// usecase/
interface ObserveScenarioOverviewUseCase {
    operator fun invoke(): Flow<OverviewState>   // Profile reads streak/minutes/completed later
}
```

Nothing else is public: activation, repeat, progress reporting, and the gateways are
`internal` to `impl` until a real external consumer exists.

## `impl` domain ports (the seams, research R6)

```kotlin
package app.yap.feature.scenario.domain

internal interface ScenarioRepository {
    val state: Flow<OverviewState>
    suspend fun refresh(today: LocalDate): Result<Unit>
    suspend fun activate(id: ScenarioId): ActivationResult
    suspend fun repeat(id: ScenarioId): ActivationResult
}

internal interface SessionGateway {          // feature-session adapts this later
    suspend fun open(scenario: Scenario)     // placeholder: ComingSoonSessionGateway
}

internal interface AccessGateway {           // feature-subscription adapts this later
    fun observe(): Flow<Access>              // placeholder: FreeAccessGateway emits Access.Free
}

internal interface PaywallGateway {          // feature-subscription adapts this later
    suspend fun open(source: PaywallSource)  // placeholder sheet until then
}

// Carries what analytics and the post-purchase continuation need: the future subscription
// adapter re-opens scenarioId after a successful purchase (FR-022, US4-AS3).
internal data class PaywallSource(val scenarioId: ScenarioId?, val origin: PaywallOrigin)

internal enum class PaywallOrigin { HeroPromo, LockedPreview, LockedRow }

internal sealed interface ActivationResult {
    data object Opened : ActivationResult
    data object AccessRequired : ActivationResult   // → PaywallGateway
    data object SlotLimitReached : ActivationResult // → snackbar, slot_limit_reached event
    data class Failed(val error: ApiError) : ActivationResult
}
```

Swap plan: when a real feature lands, its `api` exposes the behavior, `feature-scenario/impl`
gains the adapter (e.g. `SessionGatewayAdapter(sessionApi)`), and the Koin binding flips.
Ports, use cases, view models, and tests stay untouched.

## Use cases (`internal`, one behavior each)

| Use case | Behavior |
| --- | --- |
| `RefreshOverviewUseCase` | `repository.refresh(today)` on screen entry; errors stay silent when `Ready` (R3) |
| `OpenScenarioUseCase` | active → `SessionGateway.open`; available → `activate` then open; locked → `PaywallGateway`; maps `ActivationResult` |
| `RepeatScenarioUseCase` | confirmed restart → `repository.repeat` then `SessionGateway.open` |
| `ObserveScenarioOverviewUseCase` | the `api` observation above |

## Presentation contracts

Both screens follow `docs/mobile/presentation/001-view-models.md`: `UiState` + `News`, events
in, navigation intent through `Navigator`.

- **HomeViewModel** — projects `OverviewState` into hero (one of the five FR-010 variants,
  step wording «Шаг X из N»),
  active cards, slot capacity card/notice, locked preview row, streak week. Events: primary
  action, card tap, add-slot tap (→ `ScenariosNavKey`), locked tap, retry. Fires `home_view`,
  `continue_click`, `scenario_open`, `locked_content_click`, `slot_limit_reached`.
- **ScenariosViewModel** — projects the aggregate into filter rail + grouped rows (grouping
  rules of FR-020/021 — grouping is presentation logic, the domain only supplies statuses).
  Events: filter select, row tap, repeat confirm/cancel, retry. Fires `scenarios_view`,
  `scenarios_filter_select`, `scenario_open`, `locked_content_click`, `slot_limit_reached`.
- **Repeat confirmation** — a `BottomSheetSceneStrategy` destination owned by this feature
  (same mechanism as 001's provider sheet), carrying the scenario id; result returns via the
  established result API.

Loading/error rendering (FR-006): `Loading` → the design's plain progress indicator in the
accent color (lime dark / black light, core-design token); `Unavailable` → full-screen retry;
`Ready(isRefreshing)` renders content without blocking.

## Analytics names (FR-050)

`home_view`, `scenarios_view`, `scenarios_filter_select`, `continue_click`, `scenario_open`,
`slot_limit_reached`, `locked_content_click` — constants in `impl`, fired exactly once per
triggering user action through `AnalyticsTracker` ([app-shell.md](app-shell.md)). Parameters:
scenario-scoped events (`scenario_open`, `locked_content_click`, `slot_limit_reached`) carry
`scenario_id`; `scenarios_filter_select` carries `filter`; `locked_content_click` adds
`origin` from `PaywallOrigin`.
