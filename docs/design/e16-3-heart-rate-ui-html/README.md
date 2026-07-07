# E16-3 Heart Rate UI HTML Visual Planning

**Status:** HTML visual planning ready for review
**Date:** 2026-07-07
**Scope:** HTML / CSS / JS high-fidelity visual planning only

## Entry

- `docs/design/e16-3-heart-rate-ui-html/index.html`

Open the file in a browser and use the controls at the top to switch:

- Variant A: top status pill
- Variant B: current-card corner badge
- Variant C: bottom control micro-status

The same entry also switches through the required heart-rate states:

- default clean training page with no heart-rate entry
- heart rate not enabled
- permission not granted
- bluetooth disabled
- connecting
- connected waiting for data
- live bpm
- stale / disconnected

## Recommended Direction

Recommend **Variant A: top status pill**.

Why:

- It follows the current TrainFlow execution-page language: compact status pill, dark execution surface, clear current action and fixed bottom control.
- It does not add a heart-rate card.
- It does not restore `未获取心率`, manual heart-rate input, or average heart-rate trend.
- It avoids touching the TimerDial center, strength current-set metric, target weight / reps, and confirm-record form.
- It works across strength active, strength rest, strength confirm-record, and timed training.
- It can disappear entirely when heart rate is not enabled or the training page has no heart-rate entry.

## Alternatives

### Variant B: Current-Card Corner Badge

This keeps heart rate visually close to the active training object, which is useful in timed active or strength active states.

Why it is not first choice:

- It adds extra noise inside the current set card.
- It is riskier in confirm-record because the current-set summary is already intentionally collapsed to protect the confirmation form.
- It may make heart rate feel attached to the set result, even though E16-3 must not record heart rate into the session.

### Variant C: Bottom Control Micro-Status

This keeps source status near the thumb zone and avoids the header.

Why it is not first choice:

- It reduces breathing room around fixed bottom controls.
- It is most likely to compete with `确认本组`, `完成本组`, `提前开始本组`, `+15s`, and `结束训练`.
- It has the highest small-screen risk because bottom controls already need navigation-bar padding and stable button height.

## State Language

Use compact source-state language:

- `需授权`
- `蓝牙关闭`
- `连接中`
- `等待读数`
- `86 bpm`
- `数据中断`

Avoid:

- `未获取心率`
- `-- bpm`
- medical alert language
- training intensity recommendations
- debug BLE terms such as GATT, service, characteristic, CCCD, scan window, or device address

## Boundaries

This visual plan does not:

- modify Android Kotlin
- modify production manifest
- add BLE permissions to production
- connect the BLE provider
- write heart-rate samples or summaries
- modify Room, records, history, trends, `WorkoutCommand`, `WorkoutEvent`, TimedWorkoutEngine, StrengthWorkoutEngine, TimerDial, or sound logic

Future Android implementation still needs a separate story for:

- explicit opt-in
- permission rationale
- privacy and non-medical copy
- source selection / selected-device affordance
- stale data policy
- production UI mapping from abstract `HeartRateState`
- visual QA on 720x1280, especially strength confirm-record

## Verification Checklist

- Browser opens the HTML and renders nonblank.
- 720x1280 viewport renders a usable mobile layout.
- Strength confirm-record still shows effort choices, actual weight, actual reps, and fixed `确认本组` without overlap.
- Bottom buttons remain visible and stable.
- Heart rate is absent in default clean / not-enabled states.
- No Android, Kotlin, manifest, Gradle, Room, record, history, trend, command, event, TimerDial, or sound files are changed.
