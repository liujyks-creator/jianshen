# E15-1a Strength Auto-after-rest Transition Bell

**Date:** 2026-07-02
**Type:** implementation / sound feedback fix
**Scope:** strength execution sound request chain for `auto_after_rest` rest-end transitions

## User Feedback

1. Changing the global training preference from `manual_start` to `auto_after_rest` after creating an older strength plan does not make that old plan auto-start the next set.
2. When the strength plan was created with `auto_after_rest`, rest end correctly advances into the next active set, but the existing stage bell did not play.
3. Review feedback after the first implementation found the bell range was too wide: initial strength prepare, manual `开始本组`, manual `提前开始本组`, and `manual_start` rest-end prepare must not play the stage bell.

## Contract Clarification

The first feedback item is expected under the current contract: training preferences are defaults for new or edited plans. Runtime execution consumes the saved `StrengthExerciseBlock.setTimerMode` from the plan snapshot. This story does not turn the global preference into a runtime override for old plans.

## Root Cause

E15-1 made the strength engine consume `StrengthExerciseBlock.setTimerMode`, so `auto_after_rest` rest completion emits `WorkoutEvent.StrengthSetStarted` and enters `STRENGTH_ACTIVE_SET`. The sound dispatcher had no stage-bell mapping for `StrengthSetStarted`, and the strength route did not provide a cue for that auto-rest transition.

The first E15-1a implementation fixed the missing auto-rest bell, but it also let generic strength ready/start event handling remain too broad. `StrengthSetReady` could still consume the action cue as a stage bell, which made initial prepare and `manual_start` prepare paths audible.

## Fix

- `WorkoutSoundCueDispatcher.requestFor` no longer treats `StrengthSetReady` or `StrengthSetStarted` as generic stage-bell events.
- `WorkoutSoundCueDispatcher.requestForStrengthAutoRestTransition` is the only strength set-start bell request path, and it is only called by the strength route after the route proves the transition.
- `StrengthWorkoutSessionRoute` supplies the previous rest cue only when an engine tick naturally moves from `STRENGTH_REST` to `STRENGTH_ACTIVE_SET`.
- Natural rest-end transitions suppress simultaneous `NextExerciseReady` requests, so `manual_start` rest end into a new exercise prepare state does not ring.
- Manual `StartStrengthSet` commands, including tapping `提前开始本组`, still do not get a new bell from this story.
- Initial `StrengthSetReady` / prepare state and ordinary set-ready paths do not request the stage bell.
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

- Passed: `.\gradlew.bat app:testDebugUnitTest --tests "*StrengthWorkoutSoundCueRouteTest*"`
- Passed: `.\gradlew.bat app:testDebugUnitTest --tests "*Strength*" --tests "*Sound*" --tests "*Cue*" --tests "*Feedback*"`
- Passed: `.\gradlew.bat app:testDebugUnitTest`
- Passed: `.\gradlew.bat app:assembleDebug`
- Passed: `.\gradlew.bat app:lintDebug`
- Passed: `.\gradlew.bat app:check`
- Passed: `git diff --check`
- Passed: `git diff --cached --check`

## Prior Android Smoke Evidence

The review fix did not change visible UI and did not rerun emulator smoke. Current review-fix behavior is covered by focused route / dispatcher tests plus the full Gradle verification above. The previous E15-1a implementation smoke evidence remains useful for APK launch and strength auto-rest execution flow context:

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
