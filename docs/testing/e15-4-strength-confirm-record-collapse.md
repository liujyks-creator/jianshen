# E15-4 Strength confirm-record UI collapse

**Status:** Review fix implemented; needs review
**Date:** 2026-07-03
**Branch:** `codex/e15-4-strength-confirm-record-collapse`

## Scope

This story addresses the strength training confirm-record stage only. When a user finishes a set and enters `确认本组`, the previous full current-set card consumed too much vertical space before the editable confirmation card.

The implemented change collapses the current-set card only while the current strength step is `STRENGTH_CONFIRM_SET` and a pending draft exists.

Review fix: the confirmation card now prioritizes the effort choices above the editable actual weight / reps fields and uses tighter spacing, so the four effort labels and fixed bottom `确认本组` action are visible together on the 720x1280 smoke viewport.

## Implementation

- Added a focused UI-state signal in `StrengthWorkoutSessionScreenState`:
  - `isCurrentSetSummaryCollapsed`
  - `collapsedCurrentSetStatusLabel`
- Added `StrengthCollapsedCurrentSetPanel` for the confirm-record stage.
- The compact panel now keeps the status label inline with the phase / set-kind pills to reduce vertical height while preserving the same summary semantics.
- The compact panel keeps:
  - action name
  - current set progress
  - planned target summary
  - completed duration or paused status
  - warmup / working / drop / backoff set label
- The compact panel intentionally omits:
  - large timer typography
  - full progress bar
  - long cue copy
- Existing prepare, active-set and rest layouts continue to use the existing full `StrengthMainPanel`.
- Existing fixed bottom controls and bottom content reserve remain unchanged.
- The confirm card now renders:
  - compact title and planned summary
  - effort choices before actual inputs
  - actual weight and actual reps with the same planned-value prefill and validation semantics
  - rep quick options after the actual inputs
- The confirm command payload semantics remain unchanged.

## Regression Coverage

- `TrainingExecutionRegressionUiStateTest.strengthConfirmRecordCollapsesCurrentSetSummaryOnlyInConfirmStep`
- `TrainingExecutionRegressionUiStateTest.strengthConfirmRecordCompactPanelDoesNotRenderLargeMetricProgressOrLongCue`
- `TrainingExecutionRegressionUiStateTest.strengthConfirmRecordPrioritizesEffortChoicesBeforeActualInputsAndRepShortcuts`

## Verification

| Check | Result |
|---|---|
| `.\gradlew.bat app:testDebugUnitTest --tests "*TrainingExecutionRegressionUiStateTest"` | Passed |
| `.\gradlew.bat app:testDebugUnitTest --tests "*StrengthWorkout*"` | Passed |
| `.\gradlew.bat app:testDebugUnitTest --tests "*Strength*"` | Passed |
| `.\gradlew.bat app:testDebugUnitTest` | Passed |
| `.\gradlew.bat app:assembleDebug` | Passed |
| `.\gradlew.bat app:lintDebug` | Passed |
| `.\gradlew.bat app:check` | Passed |
| `git diff --check` | Passed |
| `git diff --cached --check` | Passed |

## Android Smoke

Evidence directory:

```text
C:/Users/25073/Desktop/jianshen/.local/smoke/e15-4-strength-confirm-record-collapse-review-fix/
```

Completed coverage:

- Launch the Android app on `TrainFlow_Pixel_API_36`.
- Enter a strength plan and start training.
- Start the first set.
- Complete the set and enter `确认本组`.
- Captured screenshots and UI trees showing:
  - compact current-set summary at the top
  - confirmation card moved up and compacted
  - planned values and actual inputs visible
  - `轻松` / `刚好` / `很吃力` / `动作变形` visible in the first viewport
  - fixed bottom `确认本组` action visible and not covering content
- Confirmed the set and verified the flow reaches `休息` with `提前开始本组`.
- Saved logcat and fatal / ANR scan.

Key evidence:

- `04-strength-prepare.png` / `.xml`: prepare state keeps existing full current-set card.
- `05-strength-active.png` / `.xml`: active set keeps existing large elapsed-time card.
- `06-strength-confirm.png` / `.xml`: confirm state uses compact summary and shows actual weight / reps inputs, all four effort labels and fixed `确认本组`.
- `06-strength-confirm-bounds-check.txt`: confirms the scroll viewport ends at `y=916`, fixed controls start at `y=916`, effort labels are visible at `y=736-772`, actual input content is visible through `y=916`, and fixed `确认本组` text is visible at `y=972-1012`.
- `07-strength-rest-after-confirm.png` / `.xml`: confirmation proceeds to rest.
- `logcat-fatal-anr-scan.txt`: no `FATAL EXCEPTION`, `ANR`, or `Application Not Responding` entries.

## Boundaries Confirmed

- Did not change `StrengthWorkoutEngine` semantics.
- Did not change `WorkoutCommand` or `WorkoutEvent`.
- Did not change Room schema, migration or `WorkoutSession` storage.
- Did not change E15-1 / E15-1a sound logic.
- Did not change E15-1b set timer mode semantics.
- Did not change TimerDial, E15-2 progress / clipping or E15-3 icons.
- Did not change records / history, completion pages, heart-rate or device boundaries.
- Did not add audio, image, drawable, dependency or new business capability.
