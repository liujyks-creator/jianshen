# E13 Sound Cue Asset / Audio Coexistence Audit And QA Gate

**Status:** Audit / review / QA planning gate complete
**Date:** 2026-07-01
**Scope:** Existing Android E13 sound cue implementation, production audio assets, audio coexistence boundary, tests, and follow-up real-device QA plan.

This gate does not redo the sound system, replace audio resources, add resources, change training engine semantics, change `WorkoutCommand` / `WorkoutEvent`, change Room schema, restore heart-rate UI, or connect any device / health platform.

## 1. Inputs Read

Repository docs and templates:

- `AGENTS.md`
- `DEV_STORY_PROMPT_TEMPLATE.md`
- `CODE_REVIEW_PROMPT_TEMPLATE.md`
- `docs/project-status.md`
- `docs/roadmap-backlog.md`
- `docs/planning/decision-log.md`
- `docs/planning/data-contracts.md`
- `docs/architecture.md`
- `DESIGN.md`
- `docs/ui-extension-guide.md`
- `docs/testing/mvp-acceptance-checklist.md`
- `docs/testing/permission-privacy-readiness-checklist.md`
- `docs/testing/training-state-recovery-checklist.md`
- `docs/testing/user-test-issue-template.md`
- `docs/setup.md`
- `docs/new-computer-setup.md`
- AGENTS baseline supplement: `docs/planning/product-brief.md`, `docs/planning/prd.md`, `docs/planning/ux-design.md`, `docs/readiness-report.md`

Skills:

- `skills/bmad-method/SKILL.md`
- `test-android-apps:android-emulator-qa`

## 2. Source Audit Scope

Media implementation:

- `app/src/main/java/com/liujyks/trainflow/core/media/AndroidWorkoutSoundCuePlayer.kt`
- `app/src/main/java/com/liujyks/trainflow/core/media/WorkoutSoundCueDispatcher.kt`
- `app/src/main/java/com/liujyks/trainflow/core/media/CountdownReminderFeedbackDispatcher.kt`
- `app/src/main/java/com/liujyks/trainflow/core/media/MediaBoundary.kt`

Route integration:

- `app/src/main/java/com/liujyks/trainflow/feature/workoutsession/TimedWorkoutSessionRoute.kt`
- `app/src/main/java/com/liujyks/trainflow/feature/workoutsession/StrengthWorkoutSessionRoute.kt`

Tests:

- `app/src/test/java/com/liujyks/trainflow/core/media/WorkoutSoundCueDispatcherTest.kt`
- `app/src/test/java/com/liujyks/trainflow/core/media/CountdownReminderFeedbackDispatcherTest.kt`
- `app/src/test/java/com/liujyks/trainflow/feature/workoutsession/TimedWorkoutSoundCueRouteTest.kt`
- Audio boundary assertion in `app/src/test/java/com/liujyks/trainflow/feature/workoutsession/TrainingExecutionRegressionUiStateTest.kt`

Resources and local-only assets:

- `app/src/main/res/raw/countdown_beep1.mp3`
- `app/src/main/res/raw/stage_bell_copper_clean.mp3`
- root `countdown_beep1.mp3` as an untracked forbidden duplicate
- `.local/audio/` filenames, formats, and sizes only; no submission

## 3. Current Implementation State

E13.1 is already implemented as a narrow event-consumer layer:

- `WorkoutSoundCueDispatcher` maps existing `WorkoutEvent` values plus `CountdownCue` settings into `WorkoutSoundCueRequest`.
- `WorkoutSoundCueController` de-duplicates by `eventKey` so the same engine event does not replay sound on route recomposition or repeated dispatch.
- `AndroidWorkoutSoundCuePlayer` uses `SoundPool` and loads the two production raw resources.
- `TimedWorkoutSessionRoute` wires sound and timed countdown haptics after engine results, while filtering the initial first-stage start bell.
- `StrengthWorkoutSessionRoute` wires sound cues from strength engine events and plan-level `CueSettings`.
- `MediaBoundary` remains a package boundary marker; media does not drive workout state.

No Android production code, tests, resources, or Room schema were changed in this audit.

## 4. Event To Cue Mapping

| Event | Sound kind | Production asset | Gate conditions | Notes |
|---|---|---|---|---|
| `TimedWorkEnding` | `COUNTDOWN_BEEP` | `countdown_beep1.mp3` | cue exists, `enabled`, `soundEnabled`, `remainingSec > 0`, `remainingSec <= thresholdSec` | Plays each remaining second in the final cue window. |
| `RestEnding` | `COUNTDOWN_BEEP` | `countdown_beep1.mp3` | same countdown gate | Applies to timed rest and strength rest-ending events when the route supplies a rest cue. |
| `TimedWorkStarted` | `STAGE_BELL` | `stage_bell_copper_clean.mp3` | route supplies the previously completed step cue | Initial first stage is filtered; skipped previous steps do not own a transition bell. |
| `RestStarted` | `STAGE_BELL` | `stage_bell_copper_clean.mp3` | route supplies the previous completed timed step cue, or strength route supplies rest cue | Represents the 0-second boundary after the previous completed step. |
| `SessionCompleted` | `STAGE_BELL` | `stage_bell_copper_clean.mp3` | timed route supplies the final completed step cue | Gives the final 0-second bell for completed timed sessions. Strength route currently does not supply a completion cue. |
| `StrengthSetReady` | `STAGE_BELL` | `stage_bell_copper_clean.mp3` | plan action cue exists and sound is enabled | Covers set-ready preparation boundaries. |
| `NextExerciseReady` | `STAGE_BELL` | `stage_bell_copper_clean.mp3` | plan action cue exists and sound is enabled | Covers next-exercise transition boundaries. |

Events that do not currently request sound in route usage include `SessionStarted`, `SessionPaused`, `SessionResumed`, `StrengthSetStarted`, and `StrengthSetCompleted`. `WorkoutSoundCueDispatcher.requestFor` can map `SessionCompleted` if a cue is supplied, but the strength route does not currently provide one.

## 5. CueSettings Behavior

`CountdownCue.enabled` gates the whole cue. `CountdownCue.soundEnabled` gates sound playback requests. `CountdownCue.thresholdSec` gates only countdown beeps, not stage-transition bells. `CountdownCue.vibrationEnabled` and `emphasisAnimationEnabled` are carried by `CountdownReminderFeedbackDispatcher`.

Timed route behavior:

- Sound uses `WorkoutSoundCueDispatcher`.
- Vibration uses `CountdownReminderFeedbackDispatcher` plus Compose `HapticFeedback`.
- Timed haptics only fire when `vibrationEnabled` is true.

Strength route behavior:

- Sound uses `WorkoutSoundCueDispatcher` and plan-level `CueSettings`.
- This E13 sound path does not wire strength haptics through `CountdownReminderFeedbackDispatcher`.

This is acceptable for the E13 sound cue / audio coexistence gate. If product expectations require strength vibration parity, split a focused haptics/reminder story rather than changing the sound cue boundary here.

## 6. Production Audio Asset Boundary

Tracked production resources:

| File | Size | Current use |
|---|---:|---|
| `app/src/main/res/raw/countdown_beep1.mp3` | 14,196 bytes | Final N / ... / 1 second countdown beep. |
| `app/src/main/res/raw/stage_bell_copper_clean.mp3` | 22,613 bytes | Stage / rest / set-ready transition bell. |

These two `res/raw` MP3 files are the only submitted production sound assets for E13. They are the app resource copies referenced by `R.raw.countdown_beep1` and `R.raw.stage_bell_copper_clean`.

Forbidden / local-only copies:

- Root `countdown_beep1.mp3` remains an untracked forbidden duplicate. It must not be staged, committed, moved, replaced, or used as a new production source.
- `.local/audio/` remains local history / candidate material only:
  - `9910_decoded_mono.wav` - 732,780 bytes
  - `boxing_bell_start.wav` - 127,934 bytes
  - `crisp_stage_bell.wav` - 83,834 bytes
  - `stage_bell_copper_clean.mp3` - 22,613 bytes
  - `stage_bell_copper_clean.wav` - 119,148 bytes
  - `stage_bell_single_clean.mp3` - 9,280 bytes
  - `stage_bell_single_clean.wav` - 44,206 bytes

`.local/audio/` must not be staged or committed. It can be referenced only as local provenance / history for the already submitted raw resource copy.

## 7. Audio Coexistence Strategy

The current Android playback layer satisfies the E13.1 code-side coexistence boundary:

- Uses `SoundPool`.
- Uses `AudioAttributes.USAGE_MEDIA`.
- Uses `AudioAttributes.CONTENT_TYPE_MUSIC`.
- Does not call `requestAudioFocus`.
- Does not use `AudioFocusRequest`.
- Does not use `AUDIOFOCUS_GAIN` or `AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK`.
- Does not call `setWillPauseWhenDucked`.
- Does not call `adjustStreamVolume`.
- Does not intentionally duck, pause, lower, or steal other apps' audio.
- Releases the player on route disposal.

This is a code-side strategy, not a guarantee that every Android device, Bluetooth stack, volume mix, or third-party media app behaves identically. True-device speaker and Bluetooth coexistence QA remains required.

## 8. Fail-safe Behavior

The sound path is fail-safe enough for the first version:

- Null requests are ignored.
- Duplicate event keys are ignored.
- Missing sound IDs return without playback.
- Sounds requested before `SoundPool` load completion are queued by kind and played after successful load.
- Load failure results in no playback rather than a training-state failure.
- `SoundPool.play` return value is intentionally not used to drive training state.

Audio can fail silently, but it cannot alter engine state, `WorkoutCommand`, `WorkoutEvent`, records, Room, TimerDial, or route navigation.

## 9. Test Coverage

Existing coverage:

- `WorkoutSoundCueDispatcherTest`
  - final N seconds to beep mapping
  - timed phase transition bells
  - strength set-ready bell
  - session completed bell when a cue is supplied
  - rest ending beep
  - disabled / out-of-threshold no-op
  - action vs rest cue selection
  - repeated event key de-duplication
  - coexistence policy does not request focus / duck / pause
- `CountdownReminderFeedbackDispatcherTest`
  - action vs rest feedback settings
  - disabled / out-of-threshold no-op
  - audio prompt copy does not promise identical device behavior
- `TimedWorkoutSoundCueRouteTest`
  - transition bell uses the previous completed step cue
  - next step cue does not incorrectly own the transition bell
  - initial first stage start has no bell
  - skipped previous step has no transition bell
  - final completed timed stage keeps the final bell cue
  - previous enabled stage still beeps before owning transition bell
- `TrainingExecutionRegressionUiStateTest`
  - code text and policy guard against audio focus, ducking, pause-on-duck, and stream-volume adjustment

Current test gaps:

- No instrumented test proves `SoundPool` can load and play the two raw resources on a device.
- No automated test can prove real Bluetooth / speaker coexistence with third-party music or video apps.
- Strength haptics are not wired through the timed countdown feedback dispatcher path.
- Settings/mute UX beyond existing plan cue settings remains a separate story if users need a more visible global mute surface.

## 10. QA Gate Result

Code-side E13.1 can be treated as satisfying the first-version sound cue / audio coexistence boundary:

- The two confirmed raw assets are used as intended.
- Timed and strength routes are both connected to sound cue dispatch.
- Sound playback respects `soundEnabled`.
- Vibration is separate from sound and does not affect playback.
- No disruptive audio focus / ducking path is present.
- Audio failure does not affect training state.

This gate should move to true-device QA rather than a sound-system rewrite.

## 11. Recommended True-device QA

Run this on at least one physical Android device, ideally once through phone speaker and once through Bluetooth audio:

1. Record device model, Android version, output path, volume state, and media app used.
2. Start external music or video playback before opening TrainFlow.
3. Start a short timed plan with work and rest stages that can trigger final 3 to 5 second reminders.
4. Verify final countdown beeps are audible but do not pause, lower, duck, or steal focus from the external app.
5. Verify stage bell plays at the 0-second boundary after completed stages and at timed completion.
6. Turn sound off in cue settings and verify countdown / transition sounds stop while training state still works.
7. Repeat a rest stage and verify rest countdown behavior separately from work countdown behavior.
8. Repeat on Bluetooth audio, noting whether cue audibility or external playback changes.
9. If external audio is paused, ducked, lowered, routed incorrectly, or the cue is inaudible, file a device-specific issue using `docs/testing/user-test-issue-template.md`.

## 12. Follow-up Split

- If true-device QA passes: mark E13.1 sound cue / coexistence as ready for user-test baseline.
- If code gaps are considered too thin: add focused tests only, without changing resources or engine semantics.
- If users need a clearer global mute/settings affordance: split a settings / mute story.
- If a device or media app still ducks / pauses / masks the cue: split a platform audio adaptation story.
- If fixed female phase cue is still desired: keep it in E13.2, without user arbitrary TTS or automatic voice coach.
