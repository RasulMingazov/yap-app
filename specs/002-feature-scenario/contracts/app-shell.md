# Contract: app shell and core additions

**Feature ID**: `002-feature-scenario` | **Date**: 2026-10-09

## `app-root` — main scaffold (research R8)

`MainPlaceholderScreen` is replaced by `MainScaffold`:

- The design's floating pill tab bar: Главная / Сценарии / Профиль, lime indicator,
  spring transition, `aria`-equivalent content descriptions as drawn in
  `feature_main.dc.html`.
- One back stack per tab (Navigation 3 multiple back stacks via the `navigation-3` skill);
  re-tapping the current tab scrolls its content to top.
- Home and Scenarios resolve `HomeNavKey` / `ScenariosNavKey` destinations declared in
  `feature-scenario`'s Koin module; the Profile tab renders `ProfilePlaceholderScreen`
  (name, "скоро" note) owned by `app-root` until `feature-profile`.
- The collapsing section title bar (64→44 px, scale on scroll) is part of the scaffold for
  Scenarios/Profile; Home scrolls without a toolbar, exactly as prototyped.

`app-root` keeps its session-state base-swap behavior from 001 untouched; `MainScaffold` is
simply what `LoggedIn` now shows.

## `core-common` — analytics port (research R7)

```kotlin
package app.yap.core.common.analytics

interface AnalyticsTracker {
    fun track(event: AnalyticsEvent)
}

data class AnalyticsEvent(val name: String, val params: Map<String, String> = emptyMap())
```

`shared-app` binds `LoggingAnalyticsTracker` (logs through the existing logger at debug).
No vendor SDK is chosen by this feature; the port is the whole decision.

## `core-design` — palette tokens (research R9)

`YapColors` gains the main-flow tokens lifted from `feature_main.dc.html` `theme()` — both
themes fully specified there: backgrounds (`#08070A` / `#FFFFFF`), foreground, muted, faint,
hairlines, card (`#1A1920` / `#0B0A0D`), sheet, chip/track, pill, CTA, error pair, scrim,
nav-bar background, and the shared accent `#D9FF57` on ink `#0B0A0D`. The FR-006 progress
indicator color is the accent-on-surface pair: lime in dark, `#0B0A0D` in light.

No new components move into `core-design`; hero, cards, pips, streak week, filter rail, and
rows are feature-owned composables per `docs/mobile/presentation/002-ui-compose.md`.

## Server `app` — graph additions

`Application.kt` wires `ScenarioService` (with `FreeOnlyAccessPolicy`) and installs
`scenarioRoutes` beside the auth routes; Flyway picks up `V2__scenario.sql` from the new
module's `db/migration` automatically. Error mapping reuses 001's `ErrorMapping.kt` with the
new `ApiErrorCode` constants.
