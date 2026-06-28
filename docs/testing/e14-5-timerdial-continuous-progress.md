# E14.5 TimerDial Continuous Progress

**Date:** 2026-06-28
**Status:** Complete; focused TimerDial tests, full unit tests, build/lint, Android smoke, self-review, and boundary checks passed.

## Scope

This gate fixes the normal-motion TimerDial ring progress appearing to advance in one-second jumps during timed training execution.

The fix is intentionally limited to TimerDial continuous projection identity and regression coverage. It does not change TimerDial geometry, v2 outer-ring semantic mapping, workout engines, timeline semantics, Room, session records, workout commands, workout events, or heart-rate UI.

## Inputs Read

- `AGENTS.md`
- `DEV_STORY_PROMPT_TEMPLATE.md`
- `CODE_REVIEW_PROMPT_TEMPLATE.md`
- `docs/project-status.md`
- `docs/roadmap-backlog.md`
- `docs/planning/decision-log.md`
- `docs/planning/product-brief.md`
- `docs/planning/prd.md`
- `docs/planning/ux-design.md`
- `docs/planning/data-contracts.md`
- `docs/architecture.md`
- `docs/readiness-report.md`
- `DESIGN.md`
- `docs/ui-extension-guide.md`
- `docs/planning/timer-dial-design-workflow.md`
- `docs/planning/e10-training-mode-interaction-plan.md`
- `docs/testing/e14-2-timer-dial-real-device-proportion-restore.md`
- `docs/testing/e14-4-1-training-execution-common-polish.md`
- `docs/testing/e14-4-2b-5c-session-record-compatibility.md`
- `docs/setup.md`
- `docs/new-computer-setup.md`
- `skills/bmad-method/SKILL.md`
- `huashu-design` skill, for animation and visual QA discipline only
- Android emulator QA skill

## Root Cause

TimerDial already had continuous projection logic, but the projection coroutine key included values that update every engine tick:

- `totalProgress`
- `currentStageProgress`
- `currentStageRemainingSec`
- per-segment `progress`

Those values caused `remember(...)` and `LaunchedEffect(...)` for the projection frame loop to restart every second. The rendered ring therefore kept re-anchoring on second ticks and still looked discrete even in normal motion.

## Production Fix

Updated `TimerDial` to separate projection identity from projection input:

- `TimerDialSmoothProgressIdentity` contains stable boundary fields: current segment id, paused/toggle/projectable state, and segment structure.
- `TimerDialSmoothProgressAnchor` contains tick-updated progress and remaining-time values.
- The frame-loop `LaunchedEffect` is keyed by identity and reduce-motion mode, so normal-motion projection no longer restarts every second within the same stage.
- A separate anchor effect re-anchors the projection when progress input changes, without replacing the running frame loop.
- Reduce motion, pause, terminal states, and non-projectable states still disable projection and snap to engine tick state.

Changed production files:

- `app/src/main/java/com/liujyks/trainflow/feature/workoutsession/TimerDial.kt`
- `app/src/main/java/com/liujyks/trainflow/feature/workoutsession/TimerDialUiState.kt`

## Regression Coverage

Updated `TimerDialUiStateTest` with focused coverage for:

- Smooth-progress identity remains stable across same-stage one-second ticks.
- Smooth-progress anchor changes across same-stage ticks and carries updated progress input.
- Current segment / stage switch changes projection identity.
- Pause disables projection and changes identity; resume on the same segment returns to the active identity.

Existing TimerDial coverage continues to guard:

- Normal-motion smooth projection.
- Reduce-motion discrete fallback.
- Paused, completed, and abandoned freeze behavior.
- Rest extension monotonic progress.
- TimerDial square / concentric geometry source-pattern guards.
- `+15s` / `确认+15s` UI behavior through training execution regressions.

## Boundaries Preserved

- No TimerDial outer-ring v2 semantic mapping.
- No timed composition timeline / adapter semantic change.
- No `TimedWorkoutEngine` v2 bridge semantic change.
- No Room schema or migration change.
- No session record model change.
- No `WorkoutCommand` or `WorkoutEvent` change.
- No heart-rate UI, manual input, unavailable placeholder, or statistics restoration.
- No execution-page major layout change.
- No `.local/verification` output.

## Verification

Executed:

```powershell
. .\.local\env.ps1
.\gradlew.bat app:testDebugUnitTest --tests "*TimerDial*" --tests "*TrainingExecution*" --no-daemon --console=plain
.\gradlew.bat app:testDebugUnitTest --no-daemon --console=plain
.\gradlew.bat app:assembleDebug app:lintDebug --no-daemon --console=plain
git diff --check
```

Results:

- Focused TimerDial / training execution tests passed.
- Full debug unit tests passed.
- `assembleDebug` and `lintDebug` passed.
- `git diff --check` reported only existing Windows line-ending conversion warnings, with no whitespace errors.
- Pre-stage `git diff --cached --name-only` was empty.
- Engine / Room / schema boundary diff check returned empty.
- TimerDial v2 mapping forbidden search returned no matches.
- Old debug / smoke entry search returned no matches.
- Heart-rate search matched only existing docs / regression guards; this task did not restore production heart-rate UI/input/statistics.
- Forbidden design-skill search returned no matches.

## Android Smoke

Evidence path:

`.local/smoke/e14-5-timerdial-continuous-progress/`

Result: passed on AVD `TrainFlow_Pixel_API_36`.

Smoke coverage:

- Installed current debug APK, cleared app data, and launched TrainFlow.
- Entered the default timed composition editor path from `编辑计时计划`.
- Started the timed workout through the ready gate.
- Captured a running TimerDial image sequence over multiple seconds plus a short screen recording to observe continuous ring motion between one-second text updates.
- Captured paused state and confirmed the dial freezes.
- Resumed training and confirmed projection continues on the active stage.
- Skipped into rest, triggered `+15s`, and confirmed the rest ring did not reset or move backward after extension.
- Dumped UI tree and logcat tail.
- UI tree forbidden-term scan covered `HeartRatePanel`, `ManualHeartRate`, `未获取心率`, `手动心率`, `平均心率趋势`, `COMPOSITION_V2`, and `TimedComposition`; result: no matches.
- TrainFlow fatal / crash logcat scan returned no matches.
- AVD was shut down after smoke; final `adb devices` returned an empty device list.

## Next

E14.4-2b-6 TimerDial mapping planning gate is now documented separately. Continue with E14.4-2b-6a TimerDial mapping model/state tests, and keep the E14.5 smooth-progress identity / anchor split independent from any outer-ring semantic mapping.
