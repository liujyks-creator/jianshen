# E15-1 Strength Rest Cue + Auto-start Regression

**Date:** 2026-07-01
**Type:** implementation / regression fix
**Scope:** strength execution cue dispatch and strength rest auto-start behavior

## User Feedback

1. Strength workout rest countdown did not play the final 5-second warning beep.
2. Training preferences had strength set timer default mode set to `auto_after_rest`, but after rest the strength session still waited for manual start.

## Root Cause

- Strength rest cue generation was stricter than timed training: if `CountdownCue.thresholdSec` was greater than the rest duration, strength emitted no `WorkoutEvent.RestEnding` events. Timed training already clips the threshold to the current stage duration.
- Newly created strength plans did not persist cue defaults into `PlanPreferences.cueSettings`, so the route could reach the sound dispatcher with no cue settings.
- `StrengthExerciseBlock.setTimerMode` was saved from editor defaults but the strength engine did not consume it when a rest completed; every rest ended through the manual prepare-state path.

## Fix

- Strength rest cue generation now clips the cue threshold to the current rest duration, so the default 5-second cue covers rests shorter than 5 seconds and emits remaining seconds down to 1.
- Strength plan creation/editing now carries `PlanEditorDefaults` cue settings into `PlanPreferences.cueSettings`, while preserving existing reminder and non-cue preferences when editing.
- Strength rest completion now branches by the next set's `setTimerMode`:
  - `manual_start` moves to `STRENGTH_PREPARE_SET` and waits for `StartStrengthSet`.
  - `auto_after_rest` moves directly into `STRENGTH_ACTIVE_SET`.
- Existing `WorkoutSoundCueDispatcher` mapping remains unchanged: `WorkoutEvent.RestEnding` uses `CueSettings.restEnding` and dispatches `COUNTDOWN_BEEP`; `soundEnabled=false` blocks the request.

## Boundaries

- No new audio resource was added.
- No audio focus request, ducking, or external-audio pause behavior was added.
- `WorkoutCommand` and `WorkoutEvent` semantics were not changed.
- Room schema, migrations, TimerDial, records/history, completion page, built-in icons, heart-rate UI, BLE, Huawei, Health Connect, HealthKit, Wear OS, and medical alert boundaries were not changed.
- Strength completion bell and strength haptics parity remain follow-up items.

## Verification

- `.\gradlew.bat app:testDebugUnitTest --tests "*Strength*" --tests "*Sound*" --tests "*Cue*" --tests "*Feedback*" --no-daemon --console=plain`
- `.\gradlew.bat app:testDebugUnitTest --no-daemon --console=plain`
- `.\gradlew.bat app:assembleDebug app:lintDebug --no-daemon --console=plain`
- `git diff --check`
- `git diff --cached --check`

## Android Smoke Evidence

Evidence directory:

```text
C:/Users/25073/Desktop/jianshen/.local/smoke/e15-1-strength-rest-cue-auto-start/
```

Completed coverage:

- Emulator smoke installed and launched the debug APK on `TrainFlow_Pixel_API_36`.
- Settings UI tree captured the default countdown threshold `5`, enabled rest cue / sound settings, and selected `休息后自动 · auto_after_rest`.
- Strength execution smoke entered `休息倒计时` after start -> complete -> confirm.
- Without tapping `提前开始本组`, the session advanced after rest to `本组进行中` / `第 2 / 3 组 · 总 2 / 6`, proving the auto-after-rest path on Android.
- Focused tests saved in `focused-test-output.txt` cover remaining 5..1 countdown beep dispatch, `soundEnabled=false`, non-rest no countdown beep, manual vs auto-after-rest, and timed cue/sound regression coverage.
- UI tree forbidden-word scan had no matches.
- logcat fatal / ANR scan had no matches.
- AVD was shut down and `adb devices` was empty afterward.
