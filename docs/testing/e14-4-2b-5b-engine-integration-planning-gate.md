# E14.4-2b-5b Engine Integration Planning Gate

**Date:** 2026-06-27
**Status:** Docs-only planning gate complete; no engine, UI route, TimerDial, Room, or test implementation

## Scope

This gate plans how the existing pure `TimedCompositionTimelineAdapter` should later connect to the current `TimedWorkoutEngine`.

It is intentionally planning-only:

- no Kotlin / Compose / Room / test implementation;
- no `TimedWorkoutEngine` changes;
- no `WorkoutCommand` / `WorkoutEvent` changes;
- no session record model changes;
- no v2 start-training enablement;
- no TimerDial production mapping;
- no APK generation, AVD launch, smoke screenshots, or `.local` output.

## Inputs Read

- `AGENTS.md`
- `DEV_STORY_PROMPT_TEMPLATE.md`
- `CODE_REVIEW_PROMPT_TEMPLATE.md`
- `docs/project-status.md`
- `docs/roadmap-backlog.md`
- `docs/planning/decision-log.md`
- `docs/planning/product-brief.md`
- `docs/planning/prd.md`
- `docs/planning/ux-design.md`
- `docs/planning/data-contracts.md`
- `docs/architecture.md`
- `docs/readiness-report.md`
- `DESIGN.md`
- `docs/ui-extension-guide.md`
- `docs/planning/timer-dial-design-workflow.md`
- `docs/planning/e10-training-mode-interaction-plan.md`
- `docs/testing/e14-4-2b-process-reset.md`
- `docs/testing/e14-4-2b-timed-composition-data-model-decision.md`
- `docs/testing/e14-4-2b-timed-composition-timerdial-semantics.md`
- `docs/testing/e14-4-2b-4-editor-ui-gate.md`
- `docs/testing/e14-4-2b-5-engine-timeline-planning-gate.md`
- `docs/testing/e14-4-2b-5a-timeline-adapter-model-tests.md`
- `docs/setup.md`
- `docs/new-computer-setup.md`
- `skills/bmad-method/SKILL.md`
- `test-android-apps:android-emulator-qa` skill, only to confirm that this round should not run UI smoke or start an AVD.

## Source Files Audited Read-Only

- `app/src/main/java/com/liujyks/trainflow/core/engine/TimedWorkoutEngine.kt`
- `app/src/main/java/com/liujyks/trainflow/core/model/WorkoutCommand.kt`
- `app/src/main/java/com/liujyks/trainflow/core/model/WorkoutEvent.kt`
- `app/src/main/java/com/liujyks/trainflow/feature/workoutsession/TimedWorkoutSessionRoute.kt`
- `app/src/main/java/com/liujyks/trainflow/core/model/TimedCompositionTimeline.kt`
- `app/src/main/java/com/liujyks/trainflow/core/model/WorkoutPlan.kt`
- `app/src/main/java/com/liujyks/trainflow/feature/plans/TimedCompositionPlanEditorUiState.kt`
- timed engine, workout session UI-state, rest-extension, ready-gate, timeline adapter, record mapper, history, storage, and boundary guard tests
- TimerDial source files, only to confirm that this gate does not implement TimerDial mapping

The current source paths place `TimedWorkoutEngine` under `core/engine` and `WorkoutCommand` / `WorkoutEvent` under `core/model`.

## Startup Findings

- `main...origin/main` was aligned before edits.
- Recent history included:
  - `6888e31 Add timed composition timeline adapter model`
  - `3def36d Document timed composition engine timeline planning`
  - `d8d784d Implement timed composition editor UI gate`
  - `7405350 Implement timed composition v2 model foundation`
- App source/test diff was empty before edits.
- Android paths existed for `adb.exe`, `emulator.exe`, and AVD `TrainFlow_Pixel_API_36`.
- The AVD was not started.
- Existing untracked forbidden/local artifacts were left untouched.

## Existing Engine Step Representation

`TimedWorkoutEngine.create(planSnapshot, sessionId)` validates a timed or follow-along plan and expands the snapshot into a flat list of `TimedSessionStep`.

The current legacy expansion boundary is:

- `WorkoutPlanSnapshot.toTimedSteps()` sorts plan blocks by `order`.
- Each block delegates to `PlanBlock.toTimedSteps(globalCues)`.
- `WarmupBlock`, `StretchBlock`, `CooldownBlock`, `RestBlock`, and `TimedCircuitBlock` map to legacy timed steps.
- Unsupported block kinds currently return no timed steps.
- A `TimedCompositionBlock` therefore produces no executable steps today; the E14.4-2b-4 disabled-start behavior remains correct.

Each `TimedSessionStep` currently carries generic execution fields:

- deterministic `id`;
- `kind` as work or rest;
- `sessionStepKind`;
- source `blockId`;
- optional `itemId`;
- optional `exerciseId`;
- title, duration, round, round count, stage type, icon, color, and resolved ending cue settings.

The engine then drives ticking, pause/resume, skip, rest extension, completion, step history, rest extension history, and events from this flat step list.

## Minimum V2 Integration Point

The minimum safe bridge should live at the engine timeline construction boundary, not in `TimedWorkoutSessionRoute` and not in TimerDial.

Recommended bridge:

1. Keep legacy `TimedCircuitBlock` expansion exactly as-is.
2. Add a dedicated v2 branch for `TimedCompositionBlock` in the snapshot-to-engine-step path.
3. Call `TimedCompositionTimelineAdapter.expand(block)` after the snapshot block has been deserialized and normalized.
4. Convert each adapter timeline step to `TimedSessionStep`.
5. Preserve deterministic adapter step ids as engine step ids where possible.
6. Map `compositionBlockId` to `TimedSessionStep.blockId`.
7. Map real target ids and synthetic boundary/rest target ids to `TimedSessionStep.itemId`.
8. Preserve title, duration, work/rest kind, color, icon, cue settings, and round information.
9. Do not expose raw v2 JSON parsing to the route, TimerDial, records, or UI controls.

This keeps the existing command/event-driven engine as the owner of runtime state while making the adapter the owner of v2 timeline semantics.

## V2 Start-Training Open Conditions

V2 start must remain disabled until a later implementation story proves all of the following:

- bridge tests cover `TimedCompositionBlock` to `TimedSessionStep` conversion;
- existing legacy `TimedWorkoutEngine` tests still pass unchanged;
- v2 warmup, cooldown, rounds, stageGroups, targets, target rests, and between-round rests execute in the documented order;
- rest extension remains active-rest-only and does not modify the plan snapshot;
- terminal session records can reconstruct v2 step/rest descriptors from `WorkoutSession.planSnapshot` plus deterministic step ids;
- unsupported composition versions fail closed and do not create partial sessions;
- route-level disabled-start copy is removed only after the engine bridge and route coverage are accepted;
- no Room schema migration is required for the first bridge.

Until those checks pass, editor and plan-detail v2 start entries should continue to show `待执行映射完成后可开始`.

## Legacy And V2 Coexistence

- Legacy timed plans continue through the existing `TimedCircuitBlock` engine path.
- V2 composition plans use the adapter-expanded timeline path only after bridge implementation.
- Old plans and old `WorkoutSession.planSnapshot` payloads are not rewritten.
- Compatibility wrappers may display legacy plans in the v2 editor, but viewing does not convert or save v2.
- Explicit save / conversion remains the only route that writes a current plan as composition v2.
- Unsupported v2 versions fail closed before engine execution.

## Session Record Compatibility

The first bridge does not need a Room migration or new session record fields if deterministic adapter ids are preserved.

Compatibility conclusion:

- `WorkoutSession.planSnapshot` already stores the immutable v2 JSON payload.
- `SessionStepRecord` can continue to store actual execution step records by deterministic step id.
- V2 descriptors can be reconstructed later from the historical snapshot and adapter timeline expansion.
- `TimedRestExtensionRecord` can keep using rest step identity, round, planned rest, and extension timing.
- If a future record UX requires persisted `compositionVersion`, `stageGroupId`, `targetId`, or target metadata fields outside snapshot reconstruction, that must be split into a separate migration / compatibility story.

## Rest Extension Gap And Strategy

Current engine behavior:

- `ExtendRest(seconds)` only succeeds when the engine is active and the current step is rest.
- The engine increases `remainingSec` and `extendedRestSec`.
- Rest extension history stores a generic rest step identity and previous work/rest context.
- The current record mapper uses existing step `itemId` / `blockId` values to create rest and previous-stage descriptors.

Current gap for v2:

- The engine history does not have explicit `compositionVersion`, `stageGroupId`, `targetId`, `targetKind`, `stageInstanceIndex`, or `targetInstanceIndex` fields.

Minimum strategy:

- Preserve adapter step id as the engine step id.
- Put the composition block id in `TimedSessionStep.blockId`.
- Put the v2 real target id or synthetic boundary/rest target id in `TimedSessionStep.itemId`.
- Let records and E12 descriptors reconstruct richer metadata from `WorkoutSession.planSnapshot` plus the adapter timeline.
- Do not add new persisted fields in the first bridge.
- If reconstruction is insufficient in E14.4-2b-5c, split a migration / compatibility story before changing Room or record contracts.

Rest extension remains active-rest-only. It must not insert a new target, resize planned TimerDial ratios, rewrite the plan snapshot, or create medical / safety interrupt behavior.

## WorkoutCommand And WorkoutEvent Impact

Conclusion: keep both unchanged for the first v2 engine bridge.

Current commands already cover the needed runtime controls:

- start, pause, resume, skip, extend rest, and end session for timed execution;
- strength-specific commands remain unrelated.

Current events already cover the needed execution signals:

- session started / paused / resumed / completed;
- timed work started / ending;
- rest started / ending.

If future analytics, sound cues, or UI consumers need composition metadata directly in events, that should be a separate documented decision. The bridge should not overload commands or events in this planning gate.

## TimerDial Future Input Boundary

E14.4-2b-5b does not implement TimerDial mapping.

Future TimerDial work should consume engine-visible adapter-derived timeline metadata, not raw v2 JSON. The needed inputs are:

- total v2 stage instance count for the existing 12 o'clock marker;
- current stage instance index and progress;
- current target instance index and progress;
- the current stage group's target list with planned duration ratios, colors, names, and active/completed state;
- rest extension state that preserves planned ratios and clamps progress monotonically.

TimerDial production UI, geometry, center control, inner progress, bottom controls, and animation rules remain out of scope for this gate.

## E12 Records And Trends Impact

E12 can remain snapshot-driven.

Required later descriptors:

- `compositionVersion`;
- composition block id;
- stage group id;
- target id;
- target kind;
- round index;
- stage instance index;
- target instance index;
- target order;
- planned duration;
- structure signature derived from the historical snapshot.

Legacy and v2 structures are not comparable by default. A compatibility mapper may only compare them after proving structural equivalence from snapshots. Extra rest for v2 binds to the active rest target id or deterministic synthetic between-round rest id.

## Recommended Implementation Split

1. **E14.4-2b-5b-1 engine adapter bridge tests**
   - Add focused tests for v2 adapter timeline to engine-step conversion.
   - Assert legacy engine behavior remains unchanged.
   - Assert unsupported versions fail closed.
   - Assert route start remains disabled until the implementation gate explicitly opens it.

2. **E14.4-2b-5b-2 minimum engine bridge**
   - Add the v2 branch at the engine timeline construction boundary.
   - Convert adapter steps to existing `TimedSessionStep`.
   - Keep `WorkoutCommand`, `WorkoutEvent`, Room, session record models, and TimerDial unchanged.

3. **E14.4-2b-5b-3 v2 start gate**
   - Only after bridge tests pass, remove the v2 disabled-start copy in the editor / plan detail route.
   - Add route coverage for v2 start readiness without TimerDial mapping changes.

4. **E14.4-2b-5c session record compatibility**
   - Verify terminal records, rest extension records, history descriptors, and E12 trend descriptors reconstruct from snapshot plus deterministic ids.
   - Split migration work only if reconstruction is insufficient.

5. **E14.4-2b-6 TimerDial mapping**
   - Consume adapter-derived timeline metadata after engine/record compatibility is accepted.
   - Do not jump directly here from this gate.

## Rollback Plan

If the later engine bridge causes regression:

- remove or disable only the v2 branch at the engine timeline construction boundary;
- leave the legacy `TimedCircuitBlock` engine path unchanged;
- keep v2 editor payload save support intact but restore/keep v2 start disabled;
- do not rewrite existing plans or historical snapshots;
- no Room rollback is needed if the first bridge avoids schema changes;
- if a release accidentally exposes v2 start too early, revert the route/start-gate enablement and keep v2 plans editable but not executable.

## Self-Review Checklist

- This document says the bridge is planned, not implemented.
- This document keeps v2 start disabled until later coverage exists.
- This document does not claim TimerDial mapping exists.
- This document does not require immediate Room migration.
- This document is consistent with E14.4-2b-3, E14.4-2b-4, E14.4-2b-5, and E14.4-2b-5a.
- Legacy execution remains unaffected.
- Rollback is explicit and limited to the future v2 branch / start gate.
