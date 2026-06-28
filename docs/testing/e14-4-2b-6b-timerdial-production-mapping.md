# E14.4-2b-6b TimerDial Production Mapping

**日期:** 2026-06-28
**状态:** Implementation / review gate complete in this task.

## Scope

This gate connects the E14.4-2b-6a timed composition TimerDial mapping expectations to production `TimerDialUiState` / mapper code.

Implemented:

- `TimedWorkoutSessionRoute` now passes the current `WorkoutPlan` as read-only context into the timed session UI mapper.
- `TimerDialUiState` maps active v2 `TimedCompositionBlock` steps through `TimedCompositionTimelineAdapter` metadata without changing `TimedWorkoutEngine` or `TimedCompositionTimeline`.
- Stage-group outer ring segments are derived from current `timelineStageId` targets and keep planned `durationSec` ratios for 1-5 targets.
- Warmup, cooldown, and synthetic between-round rest use a single current-stage fallback segment.
- Target color fallback is `target color -> stageGroup color -> stage type safe default`.
- Rest extension preserves planned segment durations and segment count; `+15s` only changes current rest remaining time and monotonic progress.
- 6a expectation tests now exercise the production mapper through `TimedWorkoutEngine` plus `toTimedWorkoutSessionScreenState(plan = plan)`.

Not changed:

- `TimerDial.kt` Canvas geometry, size constraints, center, drawing primitives, page layout, and bottom controls.
- E14.5 continuous progress identity / anchor split.
- `TimedWorkoutEngine` semantics.
- `TimedCompositionTimeline` semantics.
- Room schema / migrations, session record model, `WorkoutCommand`, and `WorkoutEvent`.
- Heart-rate UI, manual heart-rate input, unavailable heart-rate placeholder, or heart-rate statistics.

## Mapping Summary

- Inner ring: `totalWorkoutStageCount` comes from the current v2 timeline stage instance count, not current stage-group target count.
- Outer ring: the current v2 stage group maps its 1-5 targets by planned duration ratio; one target occupies the full outer ring, two targets keep their two planned proportions, and 3-5 targets include action / custom / rest targets together.
- State: targets before the active target are completed, the active target is active, and later targets are future.
- Fallback: warmup, cooldown, and synthetic between-round rest use one current-stage segment with legacy-like current-stage progress.
- Legacy: legacy timed plans keep existing TimerDial work/rest cycle semantics when no v2 plan context is supplied.
- Continuous progress: smooth identity remains structural only; per-second progress / remaining fields stay in the anchor path.

## Verification

- Focused tests:
  - `.\gradlew.bat app:testDebugUnitTest --tests "*TimerDial*Composition*" --tests "*TimerDial*UiState*" --tests "*TrainingExecution*" --no-daemon --console=plain`
- Full unit tests:
  - `.\gradlew.bat app:testDebugUnitTest --no-daemon --console=plain`
- Build/lint:
  - `.\gradlew.bat app:assembleDebug app:lintDebug --no-daemon --console=plain`

Android smoke evidence is stored under `.local/smoke/e14-4-2b-6b-timerdial-production-mapping/` and must not be staged.

Smoke covered v2 1 target, v2 2 targets, v2 5 targets with custom/rest targets, confirmed `+15s` rest extension, warmup / cooldown / synthetic between-round rest fallback, legacy timed plan work/rest, continuous progress screenrecord, pause/resume, and forbidden UI tree text scan.
