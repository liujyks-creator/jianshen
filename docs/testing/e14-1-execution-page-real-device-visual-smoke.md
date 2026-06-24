# E14.1 Execution Page Real-device Visual Smoke Checklist

**Status:** Prepared, pending real-device screenshots
**Date:** 2026-06-21
**Scope:** Debug APK screenshot-level visual smoke for current TrainFlow execution, history, and settings surfaces.

## Build Artifact

- APK: `C:\Users\25073\Desktop\jianshen\app\build\outputs\apk\debug\app-debug.apk`
- SHA256: `D22CC67CEC7ADC13549A10F4CAC35B4458F580848B6909C10D31A0E2C93545EE`
- Local ADB state at preparation time: `adb devices` returned no attached devices, so no real-device adb smoke or screenshots were run from Codex.

## Scope Guard

This smoke is only for real-device visual validation. It must not add or restore:

- Heart-rate cards in timed, strength, or follow-along execution pages.
- Manual heart-rate input.
- `未获取心率` placeholders.
- Average heart-rate trend cards or chart rows.
- BLE, Huawei SDK, Health Connect, HealthKit, Wear OS, or real-device health permissions.
- Medical judgment, danger alerts, training interruption rules, or intensity decisions based on heart rate.

Do not store screenshots, logs, APK copies, or temporary verification output in Git. Do not touch `.local/verification` for this E14.1 checklist.

## Device Passes

Run the screenshot matrix on at least one real Android phone after installing the APK above.

Recommended visual passes:

1. Normal system font and display size.
2. Large font or large display size for high-risk screens: timed running/rest/paused, strength active/confirm/rest, follow-along running/paused, history charts, and settings privacy copy.
3. If a small-screen phone is available, repeat the high-risk screens there.

For every screenshot, check:

- Bottom action buttons are fully visible and tappable above the system navigation area.
- Main countdown, Timer Dial or circle, and current action/stage text do not squeeze each other.
- Text does not overflow, clip, or overlap adjacent UI.
- No heart-rate card, manual heart-rate input, `未获取心率`, or `平均心率趋势` appears.
- Under large font/display size, the screen remains operable without hiding the primary action.

## Screenshot Matrix

| ID | Area | Required state | How to reach | Specific checks |
|---|---|---|---|---|
| T-01 | Timed execution | Ready | Start a saved timed plan and stop before tapping the center start control. | Ready gate is calm; center start circle is visible; no timer tick starts; no heart-rate UI. |
| T-02 | Timed execution | Running work/custom stage | Tap the center start control. | Timer Dial is centered; total remaining, current stage, and bottom controls do not collide; pause/skip/end are visible. |
| T-03 | Timed execution | Paused | Tap the center dial while running. | Paused state is visually clear; resume target remains obvious; bottom controls stay visible; no layout jump hides actions. |
| T-04 | Timed execution | Rest | Wait for or skip into a rest stage. | Rest countdown and next-stage cue are readable; `+15秒`, skip, pause/resume, and end remain accessible; no heart-rate placeholder. |
| T-05 | Timed execution | Completed | Let a short timed plan finish. | Summary/completed state shows completion info without squeezing primary summary actions; no average heart-rate trend. |
| T-06 | Timed execution | Abandoned | End a running timed session and confirm. | End confirmation is visible; abandoned summary is readable; no hidden bottom action or heart-rate copy. |
| S-01 | Strength execution | Prepare | Start a strength plan before tapping `开始本组`. | Current exercise, set target, and `开始本组` are visible; bottom controls are not below nav bar. |
| S-02 | Strength execution | Active | Tap `开始本组`. | Set timer and planned weight/reps are readable; `完成本组`, pause/resume, and end remain reachable. |
| S-03 | Strength execution | Confirm | Tap `完成本组`. | Confirm layer leaves room for weight, reps, effort choices, and confirm action; fixed controls do not cover inputs. |
| S-04 | Strength execution | Rest | Confirm a set and enter rest. | Rest countdown, next set target, early-start action, pause/resume, and end are visible; text does not wrap into controls. |
| S-05 | Strength execution | Completed | Complete the final planned set. | Completed summary is readable; set records and action totals do not overlap; no heart-rate trend or placeholder. |
| S-06 | Strength execution | Abandoned | End a running strength session and confirm. | Abandoned state is explicit; controls and summary content remain visible; no medical/heart-rate judgment copy. |
| F-01 | Follow-along | Running | Start the basic follow-along preset. | Current action/media area, countdown, and bottom controls are visible; follow-along still feels partial, not a full course platform. |
| F-02 | Follow-along | Paused | Pause from running follow-along. | Paused/resume target is obvious; current action and timer are not obscured; no heart-rate UI. |
| F-03 | Follow-along | Completed | Let a short follow-along finish. | Completed summary is readable; no average heart-rate trend; recovery/record actions do not overlap. |
| F-04 | Follow-along | Abandoned | End a running follow-along and confirm. | Abandoned state and confirmation path are readable; bottom controls remain reachable. |
| H-01 | History | Charts/trends | Open Records after at least one real persisted session. | Non-heart-rate charts/trends render; empty/insufficient-data states do not fake data; no `平均心率趋势`. |
| H-02 | History | Record detail | Open a session detail from Records. | Total/effective/paused/rest fields are readable; plan snapshot detail does not overflow; no heart-rate placeholder. |
| P-01 | Settings | Permission/privacy area | Open Settings and permission/privacy copy. | Copy says first version does not display/record/stat heart rate; text wraps cleanly; no health device setup CTA appears. |

## Result Notes Template

2026-06-21 user screenshot finding:

- T-02 / timed running showed an oversized header: the plan title `纯间歇计时器` and hard-coded `WORKOUTS` label both rendered as prominent title text, competing with the main countdown and Timer Dial. Fix: keep only the plan title as a smaller single-line header and remove the hard-coded `WORKOUTS` label from production execution UI.
- A follow-up T-02 screenshot still looked vertically deformed after the header fix: removing the heart-rate panel left the old heart-rate-era weighted spacer layout in place, so Timer Dial and bottom controls were pulled too far apart. Fix: keep heart rate removed, rebalance the no-heart-rate layout with named top/bottom elastic weights, and scale the compact Timer Dial visual slightly so real-device compact height does not look overstretched.

Use this section after the user returns screenshots or observations.

| ID | Result | Notes |
|---|---|---|
| T-01 | Pending |  |
| T-02 | Pending |  |
| T-03 | Pending |  |
| T-04 | Pending |  |
| T-05 | Pending |  |
| T-06 | Pending |  |
| S-01 | Pending |  |
| S-02 | Pending |  |
| S-03 | Pending |  |
| S-04 | Pending |  |
| S-05 | Pending |  |
| S-06 | Pending |  |
| F-01 | Pending |  |
| F-02 | Pending |  |
| F-03 | Pending |  |
| F-04 | Pending |  |
| H-01 | Pending |  |
| H-02 | Pending |  |
| P-01 | Pending |  |

## E14.1 Exit Criteria

- APK path and SHA256 are recorded.
- Unit tests, debug build, lint, and `git diff --check` pass locally.
- Real-device screenshot matrix is ready for manual capture.
- No production UI regression restores heart-rate display, manual heart-rate input, unavailable heart-rate placeholders, or average heart-rate trends.
- Any future fix found from screenshots is limited to the smallest layout change needed to keep execution pages readable and operable.
