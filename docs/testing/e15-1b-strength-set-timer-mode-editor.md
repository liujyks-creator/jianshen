# E15-1b Strength Set Timer Mode Editor Control

**Date:** 2026-07-02
**Type:** implementation / editor control
**Scope:** strength plan editor state, visible editor control, saved plan detail summary

## User Value

Older saved strength plans keep using the `StrengthExerciseBlock.setTimerMode` value stored in the plan. This story adds an explicit control in the strength plan editor so users can open an older plan, choose how the next set starts after rest, and save the updated plan back to the same plan id.

## Implementation

- `StrengthPlanEditorScreenState` now exposes an explicit `updateStrengthSetTimerMode` editor transition.
- `StrengthPlanEditorRoute` shows a `本组计时模式` card with two choices:
  - `手动开始下一组`
  - `休息后自动开始下一组`
- Saving a strength plan writes the selected mode to every saved `StrengthExerciseBlock.setTimerMode` in the current editor draft.
- Editing an existing saved plan backfills the saved plan mode first; changed global training preferences do not override it.
- Plan detail summary now displays the saved user-facing mode label, without exposing `manual_start` or `auto_after_rest`.

## Boundaries

- Training preferences still only provide defaults for newly created editor drafts.
- Runtime execution still consumes the saved `StrengthExerciseBlock.setTimerMode` from the plan snapshot.
- No `StrengthWorkoutEngine` state machine change was made.
- No E15-1 / E15-1a sound behavior was changed.
- No new audio resources, audio focus, ducking, TTS, voice control, Room schema, migration, TimerDial, records/history, heart-rate UI, BLE, Huawei, Health Connect, HealthKit, Wear OS, or medical alert behavior was added.

## Verification

- Passed: `.\gradlew.bat app:testDebugUnitTest --tests "*StrengthPlanEditor*"`
- Passed: `.\gradlew.bat app:testDebugUnitTest --tests "*PlanManagementUiStateTest"`
- Passed: `.\gradlew.bat app:testDebugUnitTest --tests "*StrengthWorkout*"`
- Passed: `.\gradlew.bat app:testDebugUnitTest`
- Passed: `.\gradlew.bat app:assembleDebug`
- Passed: `.\gradlew.bat app:lintDebug`
- Passed: `.\gradlew.bat app:check`
- Passed: `git diff --check`
- Passed: `git diff --cached --check`

## Android Smoke

Evidence directory: `.local/smoke/e15-1b-strength-set-timer-mode-editor/`

- Checked `.\.local\android-sdk\platform-tools\adb.exe devices`; no online device was present initially.
- Checked `.\.local\android-sdk\emulator\emulator.exe -list-avds`; default `TrainFlow_Pixel_API_36` was available.
- Started `TrainFlow_Pixel_API_36`, installed `app/build/outputs/apk/debug/app-debug.apk`, and launched `com.liujyks.trainflow/.app.MainActivity`.
- Opened the strength plan editor and confirmed the `本组计时模式` control is visible.
- Confirmed the captured editor tree shows `手动开始下一组` selected by default for the current fresh editor draft.
- Switched the editor control to `休息后自动开始下一组`, saved the plan, and opened the saved plan detail.
- Confirmed the captured plan detail tree shows `按动作休息 · 休息后自动开始下一组 · 计划值预填实际记录`.
- Confirmed captured UI trees do not expose raw `manual_start` or `auto_after_rest` contract tokens.
- Confirmed smoke logcat has no TrainFlow crash, fatal exception, or concrete ANR signatures.

The emulator smoke focused on the editor UI and saved plan detail path for this story. Execution behavior for `manual_start` and `auto_after_rest` remains covered by the focused `*StrengthWorkout*` unit tests and the E15-1 / E15-1a verification chain.
