# E15-1a Strength Auto-after-rest Transition Bell

**Date:** 2026-07-02
**Type:** implementation / sound feedback fix
**Scope:** strength execution sound request chain for `auto_after_rest` rest-end transitions

## User Feedback

1. Changing the global training preference from `manual_start` to `auto_after_rest` after creating an older strength plan does not make that old plan auto-start the next set.
2. When the strength plan was created with `auto_after_rest`, rest end correctly advances into the next active set, but the existing stage bell did not play.

## Contract Clarification

The first feedback item is expected under the current contract: training preferences are defaults for new or edited plans. Runtime execution consumes the saved `StrengthExerciseBlock.setTimerMode` from the plan snapshot. This story does not turn the global preference into a runtime override for old plans.

## Root Cause

E15-1 made the strength engine consume `StrengthExerciseBlock.setTimerMode`, so `auto_after_rest` rest completion emits `WorkoutEvent.StrengthSetStarted` and enters `STRENGTH_ACTIVE_SET`. The sound dispatcher had no stage-bell mapping for `StrengthSetStarted`, and the strength route did not provide a cue for that auto-rest transition.

## Fix

- `WorkoutSoundCueDispatcher` can now create a `STAGE_BELL` request for `WorkoutEvent.StrengthSetStarted` when a caller explicitly supplies a cue.
- `StrengthWorkoutSessionRoute` supplies the previous rest cue only when an engine tick moves from `STRENGTH_REST` to `STRENGTH_ACTIVE_SET`.
- Manual `StartStrengthSet` commands, including tapping `提前开始本组`, still do not get a new bell from this story.
- Auto-rest transitions suppress the simultaneous `NextExerciseReady` bell so the active-start transition owns a single stage bell request.
- `soundEnabled=false` on the rest cue still blocks the transition bell request.

## Boundaries

- Reuses `app/src/main/res/raw/stage_bell_copper_clean.mp3` through existing `WorkoutSoundCueKind.STAGE_BELL`.
- No new audio resource was added.
- No audio focus request, ducking, external-audio pause, or stream-volume adjustment was added.
- `WorkoutCommand` and `WorkoutEvent` semantics were not changed.
- Room schema, migrations, TimerDial, records/history, completion page, icons, heart-rate UI, BLE, Huawei, Health Connect, HealthKit, Wear OS, voice control, TTS, and medical alert boundaries were not changed.
- Existing plan data semantics are preserved: saved `StrengthExerciseBlock.setTimerMode` drives execution.

## Verification

Executed verification:

- Passed: `.\gradlew.bat app:testDebugUnitTest --tests "*Strength*" --tests "*Sound*" --tests "*Cue*" --tests "*Feedback*"`
- Passed: `.\gradlew.bat app:testDebugUnitTest`
- Passed: `.\gradlew.bat app:assembleDebug`
- Passed: `.\gradlew.bat app:lintDebug`
- Passed: `.\gradlew.bat app:check`
- Passed: `git diff --check`
- Passed: `git diff --cached --check`

## Android Smoke Evidence

Evidence directory:

```text
C:/Users/25073/Desktop/jianshen/.local/smoke/e15-1a-strength-auto-rest-bell/
```

Smoke result:

- Confirmed `.local/android-sdk` exists and `TrainFlow_Pixel_API_36` is available.
- Started `TrainFlow_Pixel_API_36`; `adb devices` reported `emulator-5554 device`.
- Installed `app/build/outputs/apk/debug/app-debug.apk` and launched `com.liujyks.trainflow/.app.MainActivity`.
- Confirmed settings had strength mode `auto_after_rest` selected and sound enabled.
- Started the strength plan from the editor, completed set 1, confirmed the prefilled record, and entered rest.
- Did not tap `提前开始本组`.
- After rest naturally ended, UI tree `ui-strength-auto-set2-active.xml` showed `本组进行中` and `第 2 / 3 组 · 总 2 / 6`.
- `focused-sound-request-test-output.txt` and `sound-request-test-source-evidence.txt` prove the auto-rest transition requests `WorkoutSoundCueKind.STAGE_BELL`, while `soundEnabled=false` blocks the request.
- `ui-forbidden-scan.txt` found no forbidden terms in the final execution UI tree.
- `logcat-fatal-anr-scan.txt` found no fatal exception, ANR, or Application Not Responding entry.

The emulator smoke did not prove real-world audibility directly; sound feedback is proven by the focused sound-request test and the route/dispatcher request chain.
