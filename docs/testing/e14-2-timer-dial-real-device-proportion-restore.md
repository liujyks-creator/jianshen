# E14.2 Timer Dial Real-device Proportion Restore

**Status:** Implemented; Timer Dial circle geometry confirmed by user screenshot, bottom `确认+15s` control pending next real-device confirmation
**Date:** 2026-06-21
**Scope:** Timed workout execution page layout proportion fix after heart-rate UI removal.

## Trigger

The user returned a real-device T-02 running timed execution screenshot after E11.3 / E14.1. Heart-rate UI was gone, but the timed execution page still looked vertically deformed:

- The plan title, total remaining time, Timer Dial, and bottom controls did not share vertical space like the accepted Timer Dial baseline.
- The upper half had too much empty space.
- The Timer Dial visual center sat too low.
- Bottom controls were visible, but the whole page no longer matched the E10.12 Timer Dial composition.

A second real-device screenshot showed that the vertical position was better, but the three Timer Dial primitives still visually collapsed into one another: the outer progress ring, inner marker / base-ring track, and center circle were too close, with the top count marker pressing into the progress arc.

The emulator smoke screenshot at `.local/smoke/e14-2-runtime/running.png` then exposed a separate geometry bug: the center circle was square/circular, but the outer and inner ring canvas was constrained by the compact pause-morph parent and rendered as a horizontal ellipse instead of a true concentric circle.

The user later confirmed the circle UI was normal after installing the uniquely named fixed APK, then reported a separate bottom-control issue: the rest-extension confirmation transition from `+15s` to confirm state made the middle button taller and misaligned the three bottom frames. This was treated as part of the E14.2 real-device execution-page polish because it affected the same timed execution bottom control band.

No screenshot, APK, log, `.local/verification`, `deliverables`, or other local verification artifact should be committed for this fix.

## Baseline Documents Checked

- `DESIGN.md`: training execution page primary information, Timer Dial, bottom control touch target rules.
- `docs/ui-extension-guide.md`: training execution page contract and no-heart-rate first-version boundary.
- `docs/planning/timer-dial-design-workflow.md`: E10.12 Timer Dial landing and E13.1 pause / resume morph regression baseline.
- `docs/planning/e10-training-mode-interaction-plan.md`: Timer Dial hierarchy and main operation immediate reachability.

## Cause

After heart-rate UI was removed, the running timed execution layout still kept a heart-rate-era vertical rhythm:

- The total remaining time lived inside an oversized status block.
- A large fixed title-to-total spacer and top elastic spacer left too much air above the Timer Dial.
- The bottom spacer was not dominant enough to pull the Timer Dial back toward the intended middle of the screen.

The issue was layout proportion, not a training engine, command, event, sound, rest extension, or heart-rate regression.

The later ellipse bug came from `TimerDialPauseMorph` using a compact parent height of `300dp` while the E10 Timer Dial canvas diameter is `320dp` / `324dp`. `Modifier.size(dialSize)` is still constrained by the parent maximum height, so the Canvas could be measured around `320dp x 300dp`; the center circle stayed round because its smaller `centerSizeDp` still fit inside the parent.

## Fix

- Added a named timed execution layout spec for compact and regular heights.
- Reduced the title-to-total spacer and total remaining time block height.
- Rebalanced elastic weights so the top elastic space is smaller than the bottom elastic space.
- Kept the Timer Dial compact visual size stable.
- Restored the E10-style three-circle Timer Dial geometry by reducing the center circle size and moving the inner marker / base-ring track inward enough to leave visible gaps from both the center circle and outer ring.
- Locked the Timer Dial Canvas to a required square size and made the compact morph container at least as tall as `timerDialLayoutSpec.dialSizeDp`, so ring geometry remains true concentric circles rather than an ellipse.
- Kept bottom controls in the fixed bottom area with at least `48dp` clickable height and navigation bar padding.
- Stabilized the bottom rest-extension confirmation UI after real-device feedback: the three bottom buttons now keep the same fixed height, the touch scale applies to the center label content instead of resizing the button frame, and the rest-extension labels use compact `+15s` / `确认+15s` / `已加+15s` text to avoid wrapping or clipping.
- Added regression tests for the no-heart-rate execution layout spec, Timer Dial internal ring / marker separation, square Canvas / morph container constraints, and stable bottom rest-extension confirmation labels.

## Guardrails

This fix does not:

- Restore heart-rate display.
- Restore manual heart-rate input.
- Restore average heart-rate trends.
- Connect BLE, Huawei SDK, Health Connect, HealthKit, Wear OS, or any real device source.
- Change `WorkoutCommand`, `WorkoutEvent`, training engine, rest extension recording, sound cue semantics, session records, or Room schema.
- Touch `.local/verification`.

## Follow-up Check

Ask for a new T-02 real-device screenshot from the generated debug APK. The expected result is:

- Plan title remains readable but small.
- Total remaining time is auxiliary and no longer pushes the Timer Dial down.
- Timer Dial sits closer to the screen middle.
- Bottom three controls remain fully visible and tappable.
- No heart-rate UI, manual input, unavailable placeholder, or average heart-rate trend appears.
- When the first `+15s` tap enters `确认+15s`, all three bottom buttons keep the same height and the center text stays on one line without clipping.
