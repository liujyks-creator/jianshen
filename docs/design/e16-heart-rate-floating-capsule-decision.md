# E16 Heart Rate Floating Capsule Decision

**Status:** Current product / design decision for next visual revision
**Date:** 2026-07-07
**Scope:** Heart-rate display, recording boundary, and E16-3a visual planning input

## Decision Summary

Future TrainFlow heart-rate display should use an **in-app draggable floating heart-rate capsule**.

This replaces the E16-3 initial recommendation of a top status pill. The top-pill direction is now considered an overlap-risk exploration because it can compete with the existing `进行中` session status, long plan titles, strength confirm-record, TimerDial, and fixed bottom controls.

The floating capsule is an app-shell overlay inside TrainFlow. It must not use Android system overlay / "display over other apps" permission, and it must not appear outside TrainFlow.

## Product Rules

- If the heart-rate preference is off, no floating capsule is shown.
- If the heart-rate preference is on, the capsule can appear on TrainFlow pages.
- Without an active workout, the capsule may show connection state or live bpm but must not write training records.
- During a timed workout, heart-rate samples are recorded once per second from workout start to terminal state.
- During a strength workout, heart-rate samples are recorded once per second from workout start to terminal state, including active set, rest, and confirm-record.
- The capsule is an overlay and must not reflow or resize training-page content.
- The capsule must never be allowed to settle over primary actions, bottom navigation, confirm-record controls, text inputs, keyboard area, or system gesture / status bars.
- Dragging may pass through unsafe areas, but release must snap to a safe edge position.
- A tap expands / collapses the capsule; drag begins only after a movement threshold or long-press style intent to reduce accidental taps.

## State Model

Do not mix connection states and heart-rate zone states.

### Connection / Data States

These states apply when no usable bpm is available:

- `未启用`
- `未连接源`
- `权限未赋予`
- `蓝牙关闭`
- `正在连接`
- `等待数据`
- `数据过期`
- `离线`

These states should use neutral or weak warning colors. They should not show zone names, zone colors, medical warnings, or training intensity advice.

### Heart-Rate Zone States

When bpm is available and the user has configured age, the user-facing capsule text should be:

- `低强度 {bpm} bpm`
- `热身 {bpm} bpm`
- `燃脂 {bpm} bpm`
- `有氧 {bpm} bpm`
- `无氧 {bpm} bpm`
- `极限 {bpm} bpm`
- `超过上限 {bpm} bpm`

The whole capsule should follow the zone color. `live bpm` is an internal/provider state, not the final user-facing label.

If age is missing, show bpm without zone classification, for example `心率 105 bpm`, using a neutral color.

## Zone Rules

Age is provided by the user in training preferences. The first product rule may estimate maximum heart rate from age with the common `220 - age` formula, and future versions may allow manual override.

Use the estimate as a training aid, not as a medical diagnosis.

| Zone | Percent of estimated max | Visual color |
|---|---:|---|
| 低强度 | <50% | gray-blue |
| 热身 | 50-60% | blue |
| 燃脂 | 60-70% | green |
| 有氧 | 70-80% | yellow |
| 无氧 | 80-90% | orange |
| 极限 | 90-100% | red |
| 超过上限 | above user alert threshold | deep red |

`超过上限` means the current bpm is above the user's configured alert threshold, not necessarily above the theoretical maximum heart rate. It is visual-only in the first implementation: deep red state, no sound, no vibration, no forced pause, and no medical wording.

## Recording Boundary

Heart-rate recording is only meaningful during an active workout because later analysis is tied to workout context.

Record at 1-second sampling during active timed and strength sessions. The future record model should preserve enough context for later analysis, such as:

- elapsed time
- bpm
- computed zone
- percent of estimated max
- source label / source kind
- stale / freshness state
- timed step or strength action / set context when available

Do not write samples when no workout is active. Non-workout display is live-only.

## Future Analysis

Future analysis can derive:

- average heart rate
- peak heart rate
- time in each zone
- time above user alert threshold
- heart-rate drop during rest
- strength set-to-set heart-rate change
- timed workout intensity curve

These are future analysis features. They must remain non-medical, must not diagnose disease, and must not automatically interrupt training.

## E16-3a Visual Planning Requirements

E16-3a should update the HTML high-fidelity prototype to make the floating capsule the primary direction. It must cover:

- collapsed capsule
- expanded capsule
- drag and snap-to-left / snap-to-right behavior
- unsafe-area exclusion around fixed bottom controls, confirm-record controls, inputs, keyboard, status bar, and gesture nav
- connection / data states without bpm
- zone + bpm states with age configured
- bpm-only state when age is missing
- over-limit deep red state
- strength active, rest, confirm-record
- timed TimerDial active
- non-workout page display without recording
- 720x1280 no-overlap evidence

E16-3a remains design-only unless a later story explicitly enters Android implementation.
