# E15-5c Strength Completion Sticky Return Action

**Status:** Implemented; needs review
**Date:** 2026-07-04
**Branch:** `codex/e15-5c-strength-completion-sticky-return`

## Scope

This story fixes the strength workout completed / abandoned terminal UI so the primary return action is always available without scrolling through the recap content.

The change is intentionally limited to the strength terminal presentation:

- Moves `返回计划` out of `StrengthTerminalPanel`.
- Adds a screen-level fixed bottom `StrengthTerminalReturnAction`.
- Reuses the existing fixed-bottom controls safe-area padding and minimum button height.
- Reserves fixed-bottom content padding for terminal scroll content so recap details are not hidden behind the return bar.
- Keeps the existing `onBackToPlans` callback and `返回计划` route semantics.

## Implementation

- `StrengthWorkoutSessionRoute.kt`
  - Terminal scroll content now uses `bottomControlsSpec.fixedBottomContentReserve`.
  - `StrengthTerminalPanel` only renders terminal title, summary, and recap details.
  - `StrengthTerminalReturnAction` renders a fixed bottom action surface with `navigationBarsPadding()` and a single `返回计划` primary button.
  - Non-terminal `StrengthSessionControls` are unchanged.

- `TrainingExecutionRegressionUiStateTest.kt`
  - Adds a source-level regression test that locks `返回计划` to the fixed bottom action and keeps it out of the recap panel.

## Android Smoke

Evidence directory:

```text
C:/Users/25073/Desktop/jianshen/.local/smoke/e15-5c-strength-completion-sticky-return/
```

AVD:

```text
TrainFlow_Pixel_API_36
720x1280, density 320
```

Captured evidence:

- `03-strength-prepare.*`: non-terminal prepare state keeps fixed `开始本组`; no `返回计划`.
- `04-strength-active.*`: active set state keeps existing execution controls; no terminal return action.
- `05-strength-confirm.*`: confirm-record still shows compact summary, effort options, actual inputs, and fixed `确认本组`.
- `06-strength-rest.*`: rest state keeps fixed `提前开始本组`; no terminal return action.
- `07-completed-top-sticky-return.*`: completed first viewport shows `完成` / `已完成`, terminal copy, and fixed `返回计划`.
- `08-completed-bottom-content-clear.*`: scrolled-bottom recap content remains above the fixed return action.
- `09-after-return-destination.*`: tapping `返回计划` returns to the existing plan destination.
- `12-abandoned-terminal-sticky-return.*`: abandoned terminal shows `已结束` / `力量训练已提前结束` and fixed `返回计划`, with no `已完成`.
- `ui-tree-checks.txt`: text-count and bounds checks.
- `logcat-fatal-anr-scan.txt`: no fatal / ANR matches.

Key UI tree checks:

```text
completed 返回计划 count=1
completed 已完成 count=1
completed bottom last detail bounds: [72,991][432,1032]
completed bottom return bounds: [304,1088][416,1128]
abandoned 返回计划 count=1
abandoned 已完成 count=0
prepare 返回计划 count=0
active 返回计划 count=0
confirm 确认本组 count=2
rest 返回计划 count=0
```

## Verification

Passed:

```powershell
.\gradlew.bat app:testDebugUnitTest --tests "*TrainingExecutionRegressionUiStateTest"
.\gradlew.bat app:testDebugUnitTest --tests "*StrengthWorkoutSession*"
.\gradlew.bat app:testDebugUnitTest --tests "*StrengthWorkout*"
.\gradlew.bat app:testDebugUnitTest --tests "*Strength*"
.\gradlew.bat app:testDebugUnitTest
.\gradlew.bat app:assembleDebug
.\gradlew.bat app:lintDebug
.\gradlew.bat app:check
git diff --check
git diff --cached --check
```

## Boundaries Confirmed

- Did not change `StrengthWorkoutEngine` semantics.
- Did not change `WorkoutCommand` or `WorkoutEvent`.
- Did not change Room schema, migration, session record, or strength summary data semantics.
- Did not change E15-1 / E15-1a sound logic.
- Did not change TimerDial, E15-5a route clock, E15-3 icons, records/history, heart-rate, or device boundaries.
- Did not change strength target-group color or set timer mode selector.
- Did not restore heart-rate UI, manual heart-rate input, or average heart-rate trend.
- Did not stage `.local/`, APKs, screenshots, logs, `countdown_beep1.mp3`, `deliverables/`, or `人工/`.

## Notes

Existing docs-only dirty changes in `DESIGN.md`, `docs/planning/decision-log.md`, `docs/project-status.md`, `docs/roadmap-backlog.md`, and `docs/ui-extension-guide.md` were present before this story. They were not modified for this commit.
