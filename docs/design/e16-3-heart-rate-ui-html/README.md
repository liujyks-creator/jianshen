# E16-3a Heart-rate Floating Capsule HTML Visual Revision

**Status:** Recommended visual direction for future heart-rate UI
**Date:** 2026-07-08
**Scope:** HTML / CSS / JS high-fidelity visual planning only

## Entry

- `docs/design/e16-3-heart-rate-ui-html/index.html`

Open the HTML in a browser. The prototype is interactive and supports:

- page scenario switching
- no-bpm connection / data states
- bpm + age zone states
- bpm-only state when age is missing
- collapsed / expanded capsule
- tap to expand / collapse
- drag with movement threshold
- snap-left / snap-right safe-edge behavior
- unsafe-area overlay
- keyboard-area simulation for confirm-record
- built-in no-overlap evidence via `window.collectHeartRateCapsuleEvidence()`

## Recommended Direction

E16-3a recommends an **in-app draggable floating heart-rate capsule**.

This is the only current recommended direction. The capsule is a TrainFlow app-shell overlay:

- It does not use Android system overlay / "display over other apps" permission.
- It does not appear outside TrainFlow.
- It does not participate in page layout.
- It must not push, resize, or reflow TimerDial, strength active/rest/confirm-record, completion recap, fixed bottom actions, or bottom navigation.

## Interaction Rules Covered

- A tap on the capsule only expands or collapses heart-rate detail.
- Drag begins only after a visible movement threshold (`10px` in the prototype) to avoid confusing a light tap with drag.
- While the capsule covers part of the page, the tap target is the capsule itself; taps do not pass through to underlying buttons.
- Releasing after drag snaps the capsule to the left or right safe edge.
- When the confirm-record keyboard area is visible, the prototype forces the capsule back to compact/collapsed mode because the full expanded detail cannot fit safely between confirm controls and the keyboard.
- The snap target recalculates safe Y placement and does not settle over:
  - fixed bottom primary action
  - bottom navigation
  - confirm-record card
  - confirm-record actual weight / reps inputs
  - effort selection
  - keyboard area
  - status bar
  - system gesture navigation area

## State Coverage

### No bpm

The prototype includes all no-bpm states:

- `未启用`
- `未连接源`
- `权限未赋予`
- `蓝牙关闭`
- `正在连接`
- `等待数据`
- `数据过期`
- `离线`

These states display connection / data status only. They do not show zone names, bpm placeholders, debug BLE details, medical wording, or training recommendations.

### Bpm + Age

The prototype includes all zone + bpm states:

- `低强度 88 bpm`
- `热身 105 bpm`
- `燃脂 122 bpm`
- `有氧 143 bpm`
- `无氧 165 bpm`
- `极限 180 bpm`
- `超过上限 188 bpm`

Zone color follows the current zone. `超过上限` uses deep red as a visual-only prompt. It must not trigger sound, vibration, forced pause, medical alert wording, or training interruption.

### Bpm Without Age

When age is missing, the prototype shows bpm only:

- `心率 105 bpm`

It does not show a zone label or percent range.

## Scenario Coverage

The prototype covers:

- timed TimerDial active
- strength active
- strength rest
- strength confirm-record
- strength completion
- ordinary non-workout page

Non-workout display is live-only and does not record. Workout scenarios express the future 1-second sampling boundary, but this HTML does not implement a record model.

## Superseded Explorations

The original E16-3 top-pill recommendation is no longer current.

The HTML keeps the old directions only as labeled historical references:

- Superseded A: top status pill
- Superseded B: current-card corner badge
- Superseded C: bottom micro-status

Do not use these as Android implementation guidance.

## Android Follow-up Boundary

This visual plan does not:

- modify Android Kotlin
- modify production manifest or Gradle
- add production BLE permissions
- connect the BLE provider to training UI
- write heart-rate samples or summaries
- modify Room, session records, records/history/trends
- modify `WorkoutCommand`
- modify `WorkoutEvent`
- modify `TimedWorkoutEngine`
- modify `StrengthWorkoutEngine`
- modify `TimerDial`
- modify sound, vibration, notification, or cue logic

Future Android implementation still needs separate stories for:

- explicit opt-in
- heart-rate display preference
- source selection / device status entry
- permission rationale
- privacy copy
- non-medical copy
- stale / offline policy
- `HeartRateState` to UI mapping
- recording model and 1-second sampling persistence
- Android UI implementation
- 720x1280 visual QA on real execution screens

## Verification Checklist

- Browser opens `index.html` and renders nonblank.
- 720x1280 viewport renders the mobile prototype without horizontal overflow.
- Tap toggles collapsed / expanded capsule.
- Drag only starts after movement threshold.
- Drag release snaps left or right.
- `window.collectHeartRateCapsuleEvidence()` reports `pass: true` for timed, strength active, strength rest, strength confirm-record, strength completion, ordinary page, and confirm-record with keyboard area.
- Strength confirm-record keeps effort choices, actual weight, actual reps, and fixed `确认本组` protected from capsule settlement.
- Bottom buttons and bottom navigation remain visible and stable.
- Old top pill is not marked as recommended.
- No Android, Kotlin, manifest, Gradle, Room, record, history, trend, command, event, TimerDial, or sound files are changed.
