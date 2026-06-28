# E14.4-2b-6c TimerDial mapping smoke / visual QA review gate

**Date:** 2026-06-28
**Nature:** review-only / visual QA / docs-only
**Evidence reviewed:** `.local/smoke/e14-4-2b-6b-timerdial-production-mapping/`

## Scope

This gate re-reviewed the E14.4-2b-6b production TimerDial mapping smoke evidence against the accepted E14.4-2b-6 planning gate, E14.4-2b-6a expectation tests, E14.4-2b-6b production mapping notes, and E14.5 continuous progress boundary.

No Kotlin, Compose, Room, engine, route, test, APK, `.local/verification`, or new smoke artifact changes are part of this review. The existing 6b evidence was sufficient, so AVD was not started.

## Evidence Inventory

The reviewed smoke folder contains 284 files: 123 PNG screenshots, 151 UI tree XML files, 1 MP4 screen recording, and 9 SQL setup/helper files.

| Review area | Evidence | Conclusion |
|---|---|---|
| V2 one target | `13-one-target-running.png`, `14-one-target-running-later.png`, matching UI trees | Pass. Single target renders as a full outer-ring segment and does not look visually odd. |
| V2 two targets | `49-two-work-running.png`, `52-two-rest-active.png`, pause/resume variants and UI trees | Pass. Two target proportions are visually clear and center circle spacing remains stable. |
| V2 3-5 target bucket | `58-five-multi-running.png`, `63-five-ext-probe-*.png/xml` | Pass for the max-density representative. Dedicated 3-target and 4-target screenshots were not captured; this is non-blocking because the five-target case is the stricter visual density case and 6a covers 3-5 mapping expectations. |
| Rest extension | `68-five-rest-extension-before.png`, `69-five-rest-extension-after.png`, `72-five-confirm-extension-before.png`, `73-five-confirm-extension-after.png` | Pass. No sixth segment, no visible ring reset, and no obvious backwards progress after extension. |
| Warmup fallback | `76-boundary-warmup-fallback.png`, `89-boundary-long-warmup-fallback.png` | Pass. Fallback looks like a normal active TimerDial state, not an error state. |
| Cooldown fallback | `78-boundary-cooldown-fallback.png`, `92-boundary-long-cooldown.png` | Pass. Cooldown fallback remains visually consistent with current-stage progress. |
| Between-round rest fallback | `77-boundary-between-round-rest-fallback.png`, `81-boundary-between-round-rest-fast.png`, `91-boundary-long-between-round-rest.png` | Pass. Synthetic rest fallback is readable and not confused with a broken target ratio. |
| Legacy timed plan | `84-legacy-ready.png`, `85-legacy-work-running.png`, `87-legacy-rest-running.png` | Pass. Legacy plan still uses legacy-like work/rest TimerDial behavior and is not forced into v2 target-ratio semantics. |
| Continuous progress | `59-five-continuous-progress.mp4`, `63-five-ext-probe-*.png/xml` | Pass from existing evidence. The reviewed sequence shows monotonic active-state progress without visual reset. |
| Pause / resume | `50-two-paused.png`, `51-two-resumed.png`, earlier two-target pause/resume variants | Pass. Pause freezes into the existing paused center-control state; resume returns to the running TimerDial with stable ring geometry. |
| UI tree forbidden scan | `rg` over the 6b smoke folder | Pass. No forbidden engineering labels or heart-rate UI terms were found in the smoke evidence. |

## Visual QA

- TimerDial remains a true circle with concentric inner / center geometry; no ellipse regression was observed.
- Outer-ring target ratios are readable for one target, two targets, and the five-target max-density case.
- Target segments do not crowd or squeeze the center circle.
- Active, completed, and future segments remain distinguishable through color, thickness, and progress.
- Target color fallback appears visually safe in reviewed action, custom/rest, warmup, cooldown, between-round rest, and legacy states.
- Warmup, cooldown, and between-round fallback states read as intentional current-stage progress, not as a missing-mapping error.
- Rest extension preserves the planned ratio view and monotonic progress; no sixth segment, ring reset, or backwards motion was observed.
- The legacy TimerDial still looks like the pre-v2 work/rest flow.

## Motion QA

- E14.5 continuous progress remains independently preserved: the 6b screen recording and probe sequence show smooth active-state advancement without structural identity churn.
- Pause and resume remain reasonable from the existing evidence: pause freezes the training into the current paused affordance, and resume restores the ringed running state.
- Reduce-motion behavior was not explicitly covered by the 6b smoke evidence. This is recorded as a follow-up coverage gap, not a blocking visual defect.

## Boundary QA

- UI trees in the 6b smoke folder do not expose engineering-only composition labels.
- Smoke evidence does not show heart-rate UI, manual heart-rate input, unavailable heart-rate placeholder, or average heart-rate trend UI.
- No evidence suggests TimerDial Canvas geometry / layout drift.
- No evidence suggests engine, Room, session record, command, event, route, or timeline semantics drift.
- This review did not enter E12 records/trends polish or any unrelated UI polish.

## Product Acceptance

- V2 outer ring semantics match the accepted rule: the current stage group owns 1-5 target segments, weighted by planned duration ratio.
- The 12 o'clock numeric marker continues to express the inner total stage count, not the current target count.
- Rest extension keeps the planned ratio and monotonic progress rule.
- Legacy plans are not silently upgraded into v2 target-ratio display.

## Follow-up

1. Add a future reduce-motion smoke when motion accessibility evidence is next refreshed.
2. Optionally capture dedicated 3-target and 4-target visual screenshots if a future visual regression pack wants every bucket represented, even though the five-target max-density case already covers the stricter layout risk.
3. Keep E12 records/trends and any further UI polish as separate stories; do not mix them into this review gate.

## Self-review

- This review was not rewritten as a new implementation task.
- Conclusions above are tied to existing 6b smoke evidence; reduce-motion and exact 3/4-target visual captures are not overclaimed.
- Legacy behavior was checked and recorded as unaffected.
- E14.5 continuous progress remains an independent boundary and was not merged into a new mapping fix.
- E12 records/trends and unrelated UI polish were not included.
- Uncovered items are recorded as follow-up instead of being treated as blocking defects.

## Result

No blocking smoke, visual, motion, product, or boundary issue was found. E14.4-2b-6c can close as a docs-only review gate, and the E14.4-2b TimerDial mapping chain can be considered ready to close unless the follow-up coverage items are explicitly pulled forward.
