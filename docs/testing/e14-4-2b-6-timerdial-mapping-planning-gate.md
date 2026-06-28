# E14.4-2b-6 TimerDial Mapping Planning Gate

**Date:** 2026-06-28
**Status:** Planning gate complete; docs-only source-boundary audit. No TimerDial production mapping is implemented in this gate.
**Scope:** Plan how timed composition v2 maps to existing TimerDial UI state and ring semantics.

## Boundary

This gate is intentionally planning-only. It does not change Kotlin, Compose, Room, tests, workout engines, timeline adapter semantics, session records, `WorkoutCommand`, `WorkoutEvent`, TimerDial continuous progress, APK output, AVD state, `.local/smoke`, or `.local/verification`.

The current production TimerDial UI remains the baseline. E14.2 square / concentric proportions and E14.5 continuous projection identity must be preserved.

## Inputs Read

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
- `docs/planning/timer-dial-design-workflow.md`
- `docs/planning/e10-training-mode-interaction-plan.md`
- `docs/testing/e14-2-timer-dial-real-device-proportion-restore.md`
- `docs/testing/e14-5-timerdial-continuous-progress.md`
- `docs/testing/e14-4-2b-timed-composition-timerdial-semantics.md`
- `docs/testing/e14-4-2b-5-engine-timeline-planning-gate.md`
- `docs/testing/e14-4-2b-5a-timeline-adapter-model-tests.md`
- `docs/testing/e14-4-2b-5b-2-minimum-engine-bridge.md`
- `docs/testing/e14-4-2b-5b-3-v2-start-gate-smoke.md`
- `docs/testing/e14-4-2b-5c-session-record-compatibility.md`
- `docs/setup.md`
- `docs/new-computer-setup.md`
- `skills/bmad-method/SKILL.md`
- `huashu-design` skill, for TimerDial visual / motion planning discipline only
- Android emulator QA skill, only to confirm this round performs path checks and no smoke launch

## Source Audit

Read-only audit covered:

- `app/src/main/java/com/liujyks/trainflow/feature/workoutsession/TimerDial.kt`
- `app/src/main/java/com/liujyks/trainflow/feature/workoutsession/TimerDialUiState.kt`
- `app/src/main/java/com/liujyks/trainflow/feature/workoutsession/TimedWorkoutSessionRoute.kt`
- `app/src/main/java/com/liujyks/trainflow/feature/workoutsession/TimedWorkoutSessionUiState.kt`
- `app/src/main/java/com/liujyks/trainflow/core/model/TimedCompositionTimeline.kt`
- `app/src/main/java/com/liujyks/trainflow/core/engine/TimedWorkoutEngine.kt`
- TimerDial UI state / motion tests
- Training execution regression tests
- Timed composition adapter / bridge / source-boundary tests
- Timed composition session record compatibility tests

No source conflict was found that blocks this planning gate. Current source confirms that v2 execution can enter the existing timed route through the minimum engine bridge, while TimerDial still consumes legacy-like `TimedWorkoutEngineState.toTimerDialUiState(...)` mapping.

## Current TimerDial UI State

Current `TimerDialUiState` already expresses these surfaces:

- Inner ring / total progress: `totalProgress`, `totalWorkoutStageCount`, and `completedWorkoutStageCount`.
- Current stage progress: `currentStageProgress`, `currentStageRemainingSec`, and `currentStageTimeText`.
- Segments / markers: `stageSegments` plus `innerMarkerData()`.
- 12 o'clock number marker: the first inner marker has role `TOTAL_COUNT` and uses `totalWorkoutStageCount.toString()`.
- Center content: play / pause glyph, current stage index, current stage remaining text, current stage color / text color, and `centerActionLabel`.
- Rest extension state: handled by the timed route's bottom `+15s` / `确认+15s` control and by engine `restExtensionHistory`; the dial only receives the resulting remaining time and monotonic progress.
- Continuous progress inputs: E14.5 splits `smoothProgressIdentity()` from `smoothProgressAnchor()`.

Current outer ring behavior is legacy cycle-based. `stageSegments` are derived from the current work/rest cycle by pairing a work step with the following rest step, or a rest step with the previous work step when they share block and round. Warmup, cooldown, and isolated rest-like steps naturally fall back to one segment. Segment sweep is already duration-ratio based inside the current `stageSegments` list.

Current inner total marker behavior is not v2-aware. It counts current legacy workout cycles by work/custom steps and paired rests, excluding warmup, cooldown, and rest-only steps from the total marker count.

## V2 Mapping Inputs

The adapter-derived timeline can provide the required mapping inputs:

- `timelineStageId`
- `timelineStageKind`
- `stageInstanceIndex`
- `targetInstanceIndex`
- `stageGroupId`
- `targetId`
- `targetKind`
- `roundIndex`
- `stageGroupIndex`
- `targetIndex`
- `plannedDurationSec`
- `displayName`
- `colorHex`
- optional `iconKey`
- optional resolved cue settings
- work/rest flags through `stepKind`, `isWork`, and `isRest`

The minimum engine bridge currently maps adapter step id to `TimedSessionStep.id`, composition block id to `TimedSessionStep.blockId`, and real or synthetic target id to `TimedSessionStep.itemId`. Mapping can therefore be reconstructed from the immutable plan / snapshot by expanding the v2 block and looking up the active engine step id. This planning gate does not require Room, session record, or engine model changes.

## Inner Ring Semantics

For v2, the inner ring should continue to express total stage progress for the whole workout.

The 12 o'clock number marker continues to express the inner total stage count, not target count. Target count must not replace the marker.

Recommended v2 total stage count:

```text
warmup
+ rounds * stageGroups
+ between-round rests
+ cooldown
```

More explicitly, absent zero-duration boundaries do not count:

```text
(warmupSec > 0 ? 1 : 0)
+ rounds * stageGroups.size
+ (restBetweenRoundsSec > 0 ? max(rounds - 1, 0) : 0)
+ (cooldownSec > 0 ? 1 : 0)
```

Every target inside one repeated `stageGroup` belongs to the same inner stage instance. Targets affect the outer ring only.

## Outer Ring Semantics

For a v2 repeated `stageGroup`, the outer ring represents the current stage group's 1-5 targets by planned duration ratio.

- 1 target: one full-ring segment.
- 2 targets: two segments split by each target's `plannedDurationSec`.
- 3-5 targets: multiple segments split by each target's `plannedDurationSec`.
- `action`, `custom`, and `rest` targets all participate in the planned ratio.
- Active target is highlighted / active.
- Completed targets are marked completed / elapsed.
- Future targets remain planned / future.
- Target color wins first.
- If a target color is missing or invalid, fall back to the stage group color.
- If the stage group color is missing or invalid, fall back to the existing stage-type color or default safe color.

Legacy timed plans continue using the existing TimerDial segment semantics. They should not be forced into v2 target-ratio semantics.

The existing `TimerDialStageSegmentUiState` shape can represent the first v2 outer-ring state without persisted data changes: segment id, label, stage type, planned duration, progress, current flag, and color. E14.4-2b-6a should prove this with mapping/state tests before any production mapping. If implementation later needs a small feature-layer descriptor to keep tests unambiguous, that descriptor must stay non-persisted and must not affect Room, session records, commands, or events.

## Boundary Stage Fallback

Warmup, cooldown, and synthetic between-round rest are not `stageGroup` targets.

For those stages, the outer ring should use fallback semantics: a single segment that behaves like current-stage progress or legacy-like current stage progress.

Synthetic between-round rest is a rest step, and `+15s` can apply to it while active, but it does not belong to a `stageGroup` target-ratio set. The 12 o'clock number marker still comes from the inner total stage count.

## Rest Extension

`+15s` must not change planned target ratio.

It also must not:

- insert a target;
- add a sixth segment;
- mutate `WorkoutPlan`;
- mutate the historical plan snapshot;
- recalculate outer-ring planned ratios;
- change session record model shape.

When an active rest target is extended, its segment can stay active / highlighted while the center countdown reflects the longer remaining time. The planned segment sweep remains anchored to the original planned duration. Outer and inner progress must remain monotonic and must not move backward.

Synthetic between-round rest follows the same rule: extension changes active remaining time and records extra rest, but it does not create a target or resize a planned-ratio segment.

## Continuous Progress Boundary

E14.5 remains independent and must be preserved.

Do not put per-second `remainingSec`, `currentStageProgress`, `totalProgress`, or per-segment `progress` back into the animation identity key. Mapping implementation must keep smooth identity stable across ordinary same-target second ticks. Planned-ratio structure may participate in identity only as stable segment structure, not as tick-updated progress.

For v2 implementation, the expected split is:

- identity changes when active target / stage changes, pause state changes, projectability changes, or stable segment structure changes;
- anchor changes when per-second progress or remaining time changes.

## Data And Model Impact

No Room migration is required for this mapping plan.

No session record model change is required.

No engine, timeline adapter, `WorkoutCommand`, or `WorkoutEvent` change is required by this planning gate.

Recommended first implementation path:

1. Re-expand the v2 block from the current plan / immutable snapshot.
2. Match active engine step id to adapter timeline step id.
3. Build an ephemeral TimerDial mapping model from adapter metadata.
4. Project that model into existing `TimerDialUiState` fields where possible.
5. Keep legacy plans on the current mapper.

E14.4-2b-6a should add source-boundary tests that allow v2 timeline terms only in the narrow future mapping test / mapper surface, not in TimerDial drawing code, unrelated routes, commands, events, Room, or session records.

## Testing Plan

E14.4-2b-6a should be test-first and should not implement production drawing changes.

Required coverage:

- Mapping unit tests for v2 total stage count: warmup + rounds * stageGroups + between-round rests + cooldown.
- Mapping tests proving target count does not replace the 12 o'clock number marker.
- TimerDial UI state tests for 1 target, 2 targets, and 3-5 targets.
- Work / custom / rest target participation in duration ratio.
- Active / completed / future target state projection.
- Color fallback: target, stage group, stage type / safe default.
- Warmup, cooldown, and synthetic between-round rest fallback to single current-stage semantics.
- Rest extension tests proving no planned-ratio recalculation, no inserted target, no sixth segment, and monotonic progress.
- Legacy timed plans retain current segment semantics.
- Source-boundary tests keep v2 timeline terms out of TimerDial drawing, route glue, commands, events, Room, and session records except for the explicitly allowed mapper/test surface.
- Continuous progress non-regression tests keep E14.5 identity stable across same-target second ticks.

Smoke matrix for E14.4-2b-6c:

- v2 stage group with 1 target.
- v2 stage group with 2 targets.
- v2 stage group with 3-5 targets.
- mixed work / custom / rest targets.
- synthetic between-round rest.
- warmup.
- cooldown.
- legacy timed plan.
- rest extension on a real rest target and on synthetic between-round rest.
- pause / resume and reduce-motion spot checks.

## Implementation Split

Recommended split:

1. E14.4-2b-6a TimerDial mapping model/state tests.
2. E14.4-2b-6b production mapping implementation.
3. E14.4-2b-6c smoke / visual QA.

E14.4-2b-6a should decide, through tests, whether existing `TimerDialUiState` is enough or whether a small non-persisted feature-layer mapping descriptor is useful. E14.4-2b-6b may then implement only the minimum mapper needed to satisfy those tests. E14.4-2b-6c should verify rendered behavior without changing mapping semantics.

## Rollback Plan

If v2 TimerDial mapping regresses, the safe fallback is to route v2 outer ring back to legacy-like single current-stage progress while keeping v2 execution and session records intact.

E14.5 continuous progress is independent and should not be reverted unless a future failure directly implicates that fix. E14.2 square / concentric geometry should also remain intact.

## Self-Review

- This document does not say TimerDial mapping is implemented.
- This document does not say the v2 outer ring is online.
- This document does not mix in a new continuous progress implementation.
- This document keeps the 12 o'clock number marker as total inner stage count, not target count.
- This document explicitly supports 1-5 outer-ring targets, not only two segments.
- This document says rest extension does not recalculate planned ratio and does not insert a target.
- This document preserves E14.2 proportion and E14.5 continuous progress boundaries.
- This document preserves legacy TimerDial semantics.
- This document does not require Room, session record, engine, timeline adapter, command, or event changes.

## Next

Proceed to E14.4-2b-6a TimerDial mapping model/state tests. Do not directly modify production TimerDial mapping in the next step unless a test-first 6a gate has been accepted.
