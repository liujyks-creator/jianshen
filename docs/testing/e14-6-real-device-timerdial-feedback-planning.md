# E14.6 Real-device TimerDial Feedback Planning

**Date:** 2026-06-28
**Status:** Planning complete; implementation split into follow-up stories
**Nature:** docs-only / planning-only / real-device feedback triage

**Follow-up status:** E14.6-1, E14.6-2, E14.6-2b, E14.6-2c, and E14.6-2d are closed. E14.6-3 stage style / icon planning is now recorded in `docs/testing/e14-6-3-stage-style-icon-planning.md`.

## Scope

This gate records real-device TimerDial feedback and splits it into follow-up stories. It does not implement fixes.

User evidence was confirmed as present outside the repository:

- Video: `C:/Users/25073/Downloads/Record_2026-06-28-19-41-10_168a3d1b6f3b7170206ab..mp4`
- Screenshot: `C:/Users/25073/Downloads/Screenshot_2026-06-28-19-37-49-17_168a3d1b6f3b71..jpg`

The files remain read-only evidence. They were not copied into the repository and must not be staged or committed.

## Real-device Feedback Triage

| Feedback | Story | Priority | Planning conclusion |
|---|---|---:|---|
| TimerDial outer ring animates but appears to jump forward and rebound each second. | E14.6-1 TimerDial progress rebound fix | 1 | Treat as the current real-device experience bug. Fix separately from mapping or visual redesign. |
| Completed state is unattractive because it remains on the execution page with the large dial and cards. | E14.6-2 Completion recap page redesign | 2 | Completed training should transition into a recap page, not stay on the execution surface. |
| Warmup, cooldown, and between-round rest are stages and need stage colors; rounds do not need colors. Center icons should be stage-configurable. | E14.6-3 Stage style system planning / design | 3 | Treat as a style / editor / data-contract story. Do not mix into the rebound fix. |
| The pale support ring below the inner stage ring could be thicker. | TimerDial visual polish follow-up | Later | Record as visual polish. Do not include in E14.6-1 unless a future story explicitly allows it. |

## E14.6-1 TimerDial Progress Rebound Fix

Problem:

- On a real device, normal-motion TimerDial progress is animated, but each second the active outer ring appears to move forward and then rebound.
- The goal is not merely "has animation"; the visible outer ring and current active segment must advance monotonically and continuously between engine ticks.

Target:

- Under normal motion, the outer ring / current active segment advances smoothly and monotonically.
- Engine second text can still update on ticks; the ring should not visually jump forward and snap back.
- Pause, completed, abandoned, ready, reduce-motion, and rest extension monotonic behavior remain preserved.

Boundaries:

- Do not change outer-ring semantic mapping.
- Do not change TimerDial Canvas geometry or the E14.2 square / concentric layout.
- Do not change `TimedWorkoutEngine`, timeline adapter semantics, `WorkoutCommand`, `WorkoutEvent`, Room, or session records.
- Do not combine this with completion-page redesign or stage icon / color work.

Verification for the implementation story:

- Add or update monotonic progress regression tests so same-segment ticks cannot cause backward visual progress.
- Record a real-device or emulator screen capture under normal motion and inspect the active segment over multiple ticks.
- Include pause / resume and `+15s` rest-extension regression coverage.

## E14.6-2 Completion Recap Page Redesign

Problem:

- Completion currently reads like the execution page stopped, with the large dial and cards still dominating the screen.
- The finished state should feel like a transition into review, not a frozen training surface.

Target:

- After completion, enter a dedicated "本次数据统计复盘页面".
- Top: a celebratory completion effect and a clear completed label.
- Middle: reuse existing recap content, data overview, and session summary. This can include current timed / strength summary and already available record data.
- Bottom: a return entry.
- Do not leave the user on an execution dial plus debug-like cards as the primary completed experience.

Boundaries:

- Do not change `WorkoutSession` semantics.
- Do not rewrite session record storage.
- Do not mix in E12 records / trends polish.
- Do not require new medical, heart-rate, device, or recovery claims.

Verification for the implementation story:

- Complete a timed session and confirm the route / UI state reaches the recap page.
- Confirm the page still uses the historical session snapshot / summary data rather than editing the plan or inventing data.
- Confirm return behavior and small-screen bottom affordance.

## E14.6-3 Stage Style System Planning / Design

Problem:

- Warmup duration, cooldown duration, and between-round rest are stage-like execution surfaces, but current style planning is stronger for repeated stage groups / targets than for those boundary stages.
- Rounds are structural counters; they should not receive their own colors.
- Center icons need to be configurable by stage / target, with an internal white icon set first.

Target:

- Warmup, cooldown, and between-round rest are treated as stages for style resolution and may have their own colors in the TimerDial / editor surfaces.
- Rounds remain structure only; no round color is needed.
- Stage groups and targets continue to support stable `iconKey` values.
- First version provides a project-owned built-in white icon set. Icons render on the stage color in the center circle or on stage color blocks.
- User-uploaded custom images are not part of the first version. Reserve an interface / setting path for later, but place upload / custom image support in a post-MVP or later story.
- Style tokens and icon keys should be documented before implementation, including default fallback behavior for warmup, cooldown, between-round rest, action / custom / rest targets, and invalid color / icon keys.

E14.6-3 planning result:

- Style means color plus stable built-in `iconKey`.
- First built-in key set should cover at least `warmup`, `work`, `speed_up`, `sprint`, `rest`, `recover_breathe`, `cooldown`, `strength`, `mobility`, and `custom`.
- Stored data keeps icon keys, not image paths, vector paths, resource paths, URLs, or uploaded assets.
- Color fallback should be active target -> parent stage group -> warmup / cooldown / between-round rest default -> type safe default.
- Icon fallback should be active target -> parent stage group -> boundary / type default -> `custom`.
- Existing repeated stage / target models already carry `colorHex` / `iconKey`; warmup / cooldown / between-round rest should first use default style token resolution.
- If user-editable persisted style for warmup / cooldown / between-round rest is desired, split E14.6-3a data contract / model decision before implementation.
- Recommended follow-up split is E14.6-3a data contract / model decision, E14.6-3b editor UI style picker, and E14.6-3c TimerDial consumption / visual QA.

Boundaries:

- Do not silently add a Room schema migration.
- Do not change engine, session record, command, event, or E12 trend semantics as part of style planning.
- If user-editable style fields for warmup / cooldown / between-round rest need new persisted payload fields, split a model / serializer decision story first. JSON payload extension does not automatically imply a Room table / column change.
- Do not support user image upload in the first stage style implementation.

Verification for the implementation story:

- Contract tests should cover built-in style fallback, invalid icon fallback, and no color assigned to rounds.
- Visual QA should check warmup, cooldown, between-round rest, repeated stage groups, internal rest targets, and legacy timed fallback.
- Accessibility should confirm icon-only states have labels and do not rely on color alone.

## Recommended Sequence

1. E14.6-1 TimerDial progress rebound fix.
2. E14.6-2 Completion recap page redesign.
3. E14.6-3 Stage style system planning / design.

Rationale:

- The rebound is the current visible real-device bug and should be fixed first.
- The completion recap page improves the test loop after sessions complete.
- The stage style system touches editor, data contract, icon registry, and visual design boundaries, so it should follow as a larger planning / design story.

## Explicit Non-goals

- Do not restore heart-rate UI, manual heart-rate input, unavailable heart-rate placeholders, or average heart-rate trend.
- Do not connect BLE, Huawei SDK, Health Connect, HealthKit, Wear OS, or any medical-grade health feature.
- Do not use heart rate for diagnosis, dangerous alerts, or training interruption.
- Do not change Room schema unless a later style-system story explicitly decides it.
- Do not mix E12 records / trends polish into E14.6.
- Do not repair TimerDial progress in this docs-only gate.
- Do not redo the completion page in this docs-only gate.
- Do not implement the stage icon / color system in this docs-only gate.

## Self-review

- Planning only: pass.
- Three issues split into independent stories: pass.
- E14.6-1 priority is explicit: pass.
- Completion target is a dedicated recap page, not execution-page cards: pass.
- Warmup, cooldown, and between-round rest are recorded as stages with colors; rounds are not: pass.
- First icon version is a built-in white icon set; user-uploaded images are later: pass.
- E12 trends and unrelated UI polish are excluded: pass.
- User media and `.local` artifacts are not copied or committed: pass.
