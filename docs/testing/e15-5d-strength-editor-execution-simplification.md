# E15-5d Strength Editor And Execution Simplification

## Story Scope

- Removed the unfinished strength target-set color placeholder from the strength plan editor.
- Kept target-set weight and reps side by side, and moved rest seconds to a full-width input row.
- Removed strength execution short-cue copy from prepare, active, and rest current-set cards.
- Reduced the next-set card to a summary containing the next exercise, set index, total set index, weight, and reps.
- Preserved timed training and follow-along `shortCue` behavior.

## Verification

Commands run from repository root:

```powershell
.\gradlew.bat app:testDebugUnitTest --tests "*StrengthPlanEditor*"
.\gradlew.bat app:testDebugUnitTest --tests "*StrengthWorkoutSession*"
.\gradlew.bat app:testDebugUnitTest --tests "*TrainingExecutionRegressionUiStateTest"
.\gradlew.bat app:testDebugUnitTest
.\gradlew.bat app:assembleDebug
.\gradlew.bat app:lintDebug
.\gradlew.bat app:check
git diff --check
```

All commands passed. `git diff --check` reported only Windows line-ending conversion warnings and no whitespace errors.

## Android Smoke

- AVD: `TrainFlow_Pixel_API_36`
- Size: `720x1280`
- Evidence path: `.local/smoke/e15-5d-strength-editor-execution-simplification/`

Captured evidence:

- `editor-expanded-set.png` / `ui-strength-editor-expanded-set.xml`: target set expands without the color placeholder.
- `set-target-inputs.png` / `ui-strength-editor-set-target-inputs.xml`: weight and reps inputs remain visible and clickable.
- `set-rest-input.png` / `ui-strength-editor-set-rest-input.xml`: rest seconds input is full-width and clickable.
- `strength-prepare.png` / `ui-strength-session-prepare.xml`: prepare current-set card has no action short cue.
- `strength-active.png` / `ui-strength-session-active.xml`: active current-set card has no action short cue.
- `strength-rest.png` / `ui-strength-session-rest.xml`: rest current-set card has no action short cue.
- `strength-rest-next-set.png` / `ui-strength-session-rest-next-set.xml`: next-set card is a compact summary and omits the repeated explanatory sentence.
- `strength-confirm.png` / `ui-strength-session-confirm.xml`: confirm-record first-screen visibility remains intact.
- `strength-completed.png` / `ui-strength-session-completed.xml`: completed state keeps sticky `返回计划`.
- `strength-abandoned.png` / `ui-strength-session-abandoned.xml`: abandoned state keeps sticky `返回计划`.
- `logcat.txt` and `logcat-fatal-anr-scan.txt`: fatal / ANR scan was clean.

## Boundary Check

- Did not add `StrengthSetPlan.colorHex`.
- Did not change plan snapshot JSON, serializers, Room schema, migrations, session records, history, trends, commands, events, sounds, route clock, selector, sticky return behavior, icons, heart-rate UI, or action-library content.
- Smoke evidence remains under `.local/` and is not for commit.
