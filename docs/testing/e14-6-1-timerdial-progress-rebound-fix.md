# E14.6-1 TimerDial Progress Rebound Fix

**Date:** 2026-06-28
**Status:** Complete; focused tests, full unit tests, build/lint, Android smoke, self-review, and boundary checks passed before commit.

## Scope

This task fixes the real-device TimerDial normal-motion issue where the active outer ring / active segment appeared to jump forward on each one-second engine tick and then snap back.

The fix is intentionally limited to displayed progress projection and regression coverage. It does not redesign the completion recap page, add a stage color / icon system, change v2 outer-ring semantic mapping, change TimerDial Canvas geometry or layout, or touch engine / timeline / session record / Room / command / event semantics.

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
- `docs/testing/e14-5-timerdial-continuous-progress.md`
- `docs/testing/e14-6-real-device-timerdial-feedback-planning.md`
- `docs/testing/e14-4-2b-6b-timerdial-production-mapping.md`
- `docs/testing/e14-4-2b-6c-timerdial-mapping-smoke-visual-qa.md`
- `docs/setup.md`
- `docs/new-computer-setup.md`
- `skills/bmad-method/SKILL.md`
- `huashu-design` skill and its animation / verification references, for motion and visual QA discipline only
- Android emulator QA skill

## Root Cause

E14.5 correctly separated TimerDial smooth-progress identity from tick-updated anchor data so the frame loop no longer restarts every second. The remaining rebound came from the handoff between recomposition and the anchor side effect:

- A new one-second engine tick produced a new `smoothProgressAnchor`.
- During the recomposition before the anchor side effect applied, `TimerDial` could still reuse the previous frame timestamp / anchor timestamp.
- The new base progress was then projected with stale elapsed time, briefly showing progress ahead of the new tick anchor.
- Once the anchor effect ran, the displayed value snapped back to the real tick anchor.

On a real device this looked like the active ring jumping forward once per second and then returning.

## Production Fix

Changed production files:

- `app/src/main/java/com/liujyks/trainflow/feature/workoutsession/TimerDial.kt`
- `app/src/main/java/com/liujyks/trainflow/feature/workoutsession/TimerDialUiState.kt`

The fix adds an explicit displayed-progress layer:

- Track whether the current smooth-progress anchor has actually been applied before using frame elapsed time.
- If a new anchor is pending, elapsed projection is treated as `0ms`, preventing stale elapsed time from being applied to a fresh tick base.
- Compute displayed total progress and current-stage progress through `monotonicDisplayedProgress(...)`.
- Within the same smooth identity, displayed active progress is clamped to `max(previousDisplayed, projectedOrAnchored)`.
- The clamp is reset by identity boundaries rather than by ordinary one-second ticks.

## Reset Boundaries

Identity reset is allowed when the smooth-progress identity changes, including stage / segment switches, skip-driven segment changes, pause / resume projectability changes, terminal / non-projectable states, and reduce-motion mode changes.

Rest extension does not reset the identity by itself. It may update the anchor and planned timing, but displayed progress remains monotonic and cannot move backward.

Pause, terminal states, and reduce motion remain independent boundaries: they do not use continuous projection and do not inherit a previous running floor across their own identity.

## Regression Coverage

Updated tests cover:

- Pending per-second anchor updates cannot reuse stale projection elapsed time.
- Same-stage anchor updates cannot reduce displayed active progress.
- Active v2 segment displayed progress remains monotonic across a one-second tick.
- Stage identity changes can reset displayed progress.
- Pause and terminal freeze boundaries still hold.
- Reduce motion remains discrete and does not use continuous projection.
- Rest extension does not move displayed progress backward.

Changed test files:

- `app/src/test/java/com/liujyks/trainflow/feature/workoutsession/TimerDialUiStateTest.kt`
- `app/src/test/java/com/liujyks/trainflow/feature/workoutsession/TimerDialCompositionMappingTest.kt`
- `app/src/test/java/com/liujyks/trainflow/feature/workoutsession/TimerDialMotionTest.kt`

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
- `git diff --check` reported only Windows line-ending conversion warnings, with no whitespace errors.
- Pre-stage `git diff --cached --name-only` was empty.
- Engine / timeline / Room / schema / command / event boundary diff returned empty.
- TimerDial geometry diff review showed only smooth-progress state / elapsed / displayed-progress changes; no Canvas geometry, size, layout, or bottom-control changes.
- v2 mapping semantic drift search matched only existing `TimerDialUiState` mapper references; `TimerDial.kt` had no new mapping references.
- Old smoke entry search returned no matches.
- Heart-rate regression search matched only existing documentation and regression guard references; this task did not restore production heart-rate UI / input / statistics.
- Forbidden design-skill search returned no matches.

## Android Smoke

Evidence path:

`.local/smoke/e14-6-1-timerdial-progress-rebound-fix/`

Smoke targets:

- Run current debug APK on AVD `TrainFlow_Pixel_API_36`.
- Capture active TimerDial over several seconds and inspect for no forward-then-back rebound on one-second ticks.
- Capture pause / resume.
- Capture rest extension if feasible.
- Capture legacy or simple timed plan if feasible.
- Dump UI tree and logcat tail.
- Scan UI tree for forbidden debug / heart-rate terms.
- Shut down the AVD and confirm `adb devices` is empty.

Result: passed on AVD `TrainFlow_Pixel_API_36`.

Evidence captured:

- `running.mp4`
- `running-before.png`
- `running-0.png` through `running-5.png`
- `paused.png` / `paused.xml`
- `resumed.png` / `resumed.xml`
- `rest-before.png` / `rest-confirm.png` / `rest-after.png`
- `rest2-before.png` / `rest2-after.png`
- `launch.xml`, `editor.xml`, `ready.xml`, `running.xml`, rest UI trees
- `logcat-tail.txt`

Smoke observations:

- Default timed composition plan entered from `编辑计时计划`, then started through the ready gate.
- Running TimerDial video and screenshot sequence showed no forward-then-back rebound on one-second ticks.
- Screenshot sequence outer-arc endpoint check was monotonic: `67.35`, `73.26`, `79.26`, `85.24`, `91.22`, `97.18` degrees for `running-0.png` through `running-5.png`.
- Pause captured a frozen paused state with the center action changing to resume.
- Resume returned to running state.
- Rest extension was captured on a one-minute rest: remaining time changed from `00:56` to `01:08` and total remaining changed from `09:26` to `09:38`, confirming the extension without backward visual reset.
- UI tree forbidden-term scan covered `COMPOSITION_V2`, `TimedComposition`, `HeartRatePanel`, `ManualHeartRate`, `未获取心率`, `手动心率`, and `平均心率趋势`; result: no matches.
- Logcat crash scan returned no matches.
- Legacy / simple timed plan was not separately captured in this smoke because the current default local entry is the v2 timed composition path; legacy behavior remains covered by focused tests and old-entry search.
- AVD was shut down after smoke; final `adb devices` returned an empty device list.

## Boundaries Preserved

- No completion recap page redesign.
- No stage color / icon system.
- No TimerDial outer-ring v2 semantic remapping.
- No TimerDial Canvas geometry, size, layout, or bottom-control change.
- No timed engine, timeline adapter semantic, session record, Room, command, or event change.
- No heart-rate UI, manual input, unavailable placeholder, or average trend restoration.
- No `.local/verification` output.

## Next

E14.6-2 Completion recap page redesign planning / visual gate remains the next story. It should be handled separately from TimerDial progress projection.
