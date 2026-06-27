# E14.4-2b-5a Timeline Adapter Model / Tests

**Date:** 2026-06-27
**Status:** Implemented and pushed as `6888e31`; closed for E14.4-2b-5b planning handoff

## Scope

This slice adds only a pure adapter-owned timeline model for timed composition v2 payloads.

Implemented:

- `TimedCompositionTimeline`
- `TimedCompositionTimelineStep`
- `TimedCompositionTimelineStageKind`
- `TimedCompositionTimelineStepKind`
- `TimedCompositionTimelineTargetKind`
- `TimedCompositionTimelineAdapter`

The adapter input is `TimedCompositionBlock` with `compositionVersion == 2`. Unsupported versions fail closed before normalization. Legacy `TimedCircuitBlock` / `TimedExerciseItem` structures are not adapter inputs and continue to belong to the existing legacy engine path.

## Explicit Non-Scope

This slice does not:

- integrate with `TimedWorkoutEngine`;
- enable v2 start training from editor or plan detail;
- modify TimerDial production mapping;
- modify Room schema or migrations;
- modify `WorkoutCommand` / `WorkoutEvent`;
- modify session record models or mappers;
- add debug seeds, smoke plans, APK output, emulator smoke, screenshots, `.local/smoke`, or `.local/verification`.

## Expansion Rules

- `warmupSec > 0` emits one warmup work step before all rounds.
- Each round expands normalized `stageGroups` by order.
- Each stage group expands normalized `targets` by order.
- `action` and `custom` targets emit timed work steps.
- `rest` targets emit timed rest steps.
- `restBetweenRoundsSec > 0` emits a synthetic between-round rest only after non-final rounds.
- `cooldownSec > 0` emits one cooldown work step after all rounds.
- Zero-duration warmup, cooldown, and between-round rest do not emit steps.
- Stage group duration remains derived from targets; timeline steps use target durations, not the stage group total.

## Stable Metadata

Each step carries:

- `compositionVersion`
- `compositionBlockId`
- deterministic `id`
- deterministic `timelineStageId`
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
- optional resolved `cueSettings`

Warmup, cooldown, and between-round rest use adapter-owned synthetic stage / target ids and kinds. These synthetic ids are timeline-only and are not written back into the v2 payload.

## Test Coverage

Added focused unit coverage for:

- warmup / cooldown generation;
- rounds x stageGroups x targets expansion order;
- between-round rest insertion only between rounds;
- zero-duration warmup / cooldown / between rest omission;
- target-duration timeline steps despite stage-group derived duration;
- complete stable metadata;
- `stageInstanceIndex` and `targetInstanceIndex`;
- target order normalization;
- max-5 target normalization from the model layer;
- unsupported composition versions failing closed;
- legacy blocks not being adapter inputs;
- adapter terms staying out of engine, TimerDial, and route sources.

## Boundary

The adapter is a future engine-integration input only. E14.4-2b-5b documents how the existing engine should later consume this timeline without changing legacy execution, commands/events, records, or TimerDial in the planning gate. The next implementation step is E14.4-2b-5b-1 engine adapter bridge tests; E14.4-2b-6 TimerDial mapping must consume the adapter-expanded timeline later and must not parse raw v2 JSON directly.
