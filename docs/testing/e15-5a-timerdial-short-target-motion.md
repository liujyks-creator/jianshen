# E15-5a TimerDial Short Target Motion

Date: 2026-07-03

## Scope

This story diagnoses the reported visible acceleration on 1s / 2s TimerDial targets in the production timed workout execution route. The diagnostic path is debug-only and launches the production `TimedWorkoutSessionRoute` with a timed composition plan containing:

- 2s action target
- 1s rest target
- reminder cues disabled

Evidence is stored under `.local/smoke/e15-5a-timerdial-short-target-motion/` and is intentionally not tracked by Git.

## Baseline Finding

The acceleration reproduced on the first manual start path. In the baseline run, the route-level 1s tick loop had already been running while the ready gate was visible. When the user tapped start, the first active tick could arrive before a full second of active time elapsed.

Representative baseline evidence:

- `.local/smoke/e15-5a-timerdial-short-target-motion/baseline/baseline-active2.png`
- `.local/smoke/e15-5a-timerdial-short-target-motion/baseline/baseline-active2.xml`
- `.local/smoke/e15-5a-timerdial-short-target-motion/baseline/baseline-screenrecord.mp4`
- `.local/smoke/e15-5a-timerdial-short-target-motion/baseline/baseline-device-frame-active-progress-summary.txt`
- `.local/smoke/e15-5a-timerdial-short-target-motion/baseline/baseline-active2-logcat-fatal-anr-strict.txt`

Observed baseline symptom:

- The fixed diagnostic plan was launched at 720x1280 on `TrainFlow_Pixel_API_36`.
- Around the first post-start active capture, the 2s work target had already advanced to `00:01` and visually near the half-progress region.
- Device-frame sampling recorded work progress around `0.52` at tap+824ms and around `0.67` at tap+1140ms.

This matched the real-device report that the visible issue happens after manually starting or manually advancing into a group, while naturally flowing groups are not the primary trigger.

## Root Cause

`TimedWorkoutSessionRoute` owned a `LaunchedEffect(plan.id, sessionId)` route clock:

- It started before the session left the ready gate.
- It delayed for 1000ms on its own phase.
- After the user tapped start or skip, the existing delay could complete almost immediately and dispatch `TimedWorkoutEngine.tick(...)`.

That made `activeElapsedSec` jump by one second early for the newly entered target. TimerDial then consumed a valid, but prematurely advanced, production engine state. The short-target smooth projection was not the root cause.

## Fix

The route now resets the timed route clock anchor for manual commands that create a new wall-clock phase:

- `StartSession`
- `PauseSession`
- `ResumeSession`
- `SkipStep`

The clock `LaunchedEffect` is keyed by that anchor, and the delayed loop checks that the anchor it launched with is still current before ticking. This prevents a stale pre-click delay from advancing the newly started or manually skipped-to target.

The fix does not change timed engine semantics, `WorkoutCommand`, `WorkoutEvent`, Room schemas, TimerDial canvas geometry, v2 planned-duration ratios, or rest-extension semantics.

## Fixed Evidence

Representative fixed evidence:

- `.local/smoke/e15-5a-timerdial-short-target-motion/fixed/fixed-active2.png`
- `.local/smoke/e15-5a-timerdial-short-target-motion/fixed/fixed-active2.xml`
- `.local/smoke/e15-5a-timerdial-short-target-motion/fixed/fixed-screenrecord.mp4`
- `.local/smoke/e15-5a-timerdial-short-target-motion/fixed/fixed-device-frame-active-progress-summary.txt`
- `.local/smoke/e15-5a-timerdial-short-target-motion/fixed/fixed-skip-active.png`
- `.local/smoke/e15-5a-timerdial-short-target-motion/fixed/reduce-motion/reduce-motion-active.png`
- `.local/smoke/e15-5a-timerdial-short-target-motion/fixed/logcat-after-frame-run-fatal-anr-strict.txt`

Observed fixed behavior:

- The same post-start capture path shows the 2s work target still at `00:02` / total `00:03`, rather than prematurely dropping to `00:01`.
- The fixed frame run remains monotonic before target reset/completion.
- The manual skip smoke shows the 1s rest target still active at `00:01` after skip, rather than completing from a stale tick phase.
- The reduce-motion smoke shows no continuous current-segment projection before the next discrete tick.

## Sampling Notes

Device `screencap` timestamps were recorded immediately before each PNG capture, so the PNG image can lag the timestamp by capture overhead. The screenshot/frame summaries are used as reproducible visual evidence and before/after comparison, while exact 1s / 2s projection linearity is covered by the existing focused TimerDial tests:

- `oneSecondTargetProjectionIsLinearMonotonicAndClamped`
- `twoSecondTargetProjectionKeepsEqualLinearFrameStepsAcrossEngineAnchors`
- reduce-motion projection tests in `TimerDialUiStateTest`
