# E14.4-2b-5 Engine Timeline Planning Gate

**Date:** 2026-06-27
**Status:** Docs-only planning gate complete; E14.4-2b-5a adapter-only implementation pushed as `6888e31`; E14.4-2b-5b engine integration planning gate documented separately

## Scope

This gate plans how the accepted timed composition v2 payload should later expand into a training execution timeline. It is a source-boundary audit and data-impact assessment only.

This gate does not implement `TimedWorkoutEngine` v2 support, TimerDial mapping, Room migrations, `WorkoutCommand` / `WorkoutEvent` changes, session record model changes, Kotlin / Compose / test code, APK generation, emulator smoke, or screenshots.

## Inputs Read

- `AGENTS.md`
- `DEV_STORY_PROMPT_TEMPLATE.md`
- `CODE_REVIEW_PROMPT_TEMPLATE.md`
- `docs/project-status.md`
- `docs/planning/decision-log.md`
- `docs/planning/product-brief.md`
- `docs/planning/prd.md`
- `docs/planning/ux-design.md`
- `docs/planning/data-contracts.md`
- `docs/architecture.md`
- `docs/roadmap-backlog.md`
- `docs/readiness-report.md`
- `DESIGN.md`
- `docs/ui-extension-guide.md`
- `docs/planning/timer-dial-design-workflow.md`
- `docs/planning/e10-training-mode-interaction-plan.md`
- `docs/testing/e14-4-2b-process-reset.md`
- `docs/testing/e14-4-2b-timed-composition-timerdial-semantics.md`
- `docs/testing/e14-4-2b-timed-composition-data-model-decision.md`
- `docs/testing/e14-4-2b-4-editor-ui-gate.md`
- `docs/setup.md`
- `docs/new-computer-setup.md`
- `skills/bmad-method/SKILL.md`
- `test-android-apps:android-emulator-qa` skill, only to confirm that this round should not run UI smoke while still recording local emulator paths.

Read-only source files inspected:

- `app/src/main/java/com/liujyks/trainflow/core/model/WorkoutPlan.kt`
- `app/src/main/java/com/liujyks/trainflow/core/model/CommonContracts.kt`
- `app/src/main/java/com/liujyks/trainflow/core/model/WorkoutSession.kt`
- `app/src/main/java/com/liujyks/trainflow/core/data/WorkoutPlanSnapshotStorageJson.kt`
- `app/src/main/java/com/liujyks/trainflow/core/data/WorkoutSessionRecordMappers.kt`
- `app/src/main/java/com/liujyks/trainflow/core/engine/TimedWorkoutEngine.kt`
- `app/src/main/java/com/liujyks/trainflow/core/engine/TimedWorkoutSessionHistory.kt`
- `app/src/main/java/com/liujyks/trainflow/feature/plans/TimedCompositionEditorDraftAdapter.kt`
- `app/src/main/java/com/liujyks/trainflow/feature/plans/TimedCompositionPlanEditorUiState.kt`
- `app/src/main/java/com/liujyks/trainflow/feature/plans/TimedPlanEditorRoute.kt`
- `app/src/main/java/com/liujyks/trainflow/feature/plans/PlanManagementUiState.kt`
- `app/src/main/java/com/liujyks/trainflow/core/model/WorkoutCommand.kt`
- `app/src/main/java/com/liujyks/trainflow/core/model/WorkoutEvent.kt`
- `app/src/main/java/com/liujyks/trainflow/feature/workoutsession/TimedWorkoutSessionRoute.kt`
- `app/src/main/java/com/liujyks/trainflow/feature/workoutsession/TimerDial.kt`
- `app/src/main/java/com/liujyks/trainflow/feature/workoutsession/TimerDialUiState.kt`
- `app/src/main/java/com/liujyks/trainflow/feature/history/HistoryUiState.kt`

## Startup Checks

- `git status --short` showed docs planning changes plus existing untracked local / forbidden artifacts: root APK, `countdown_beep1.mp3`, `deliverables/`, and `人工/`. Forbidden artifacts were not touched.
- `git rev-list --left-right --count main...origin/main` returned `0 0`.
- `git log -5 --oneline` included `d8d784d Implement timed composition editor UI gate` and `7405350 Implement timed composition v2 model foundation`.
- `git diff --name-only -- app/src/main app/src/test` was empty before edits.
- `. .\.local\env.ps1` loaded `JAVA_HOME` and `ANDROID_HOME`.
- Android local path check passed:
  - `.local/android-sdk/platform-tools/adb.exe` exists.
  - `.local/android-sdk/emulator/emulator.exe` exists.
  - AVD list includes `TrainFlow_Pixel_API_36`.
- No AVD was started, no APK was installed, and no smoke output was written.

## Current Source Boundary Findings

- `TimedCompositionBlock` v2, stage groups, targets, normalization, serializer round-trip, and editor draft adapter foundation are present.
- The editor UI can save v2 payload and deliberately disables starting training for v2 plans with `待执行映射完成后可开始`.
- Plan detail also blocks v2 start while allowing v2 plans to remain editable.
- `TimedWorkoutEngine` currently expands legacy timed blocks only. `TimedCompositionBlock` falls through to no timed steps, so the E14.4-2b-4 disabled-start strategy remains correct.
- `WorkoutSession.planSnapshot` is written from the original `WorkoutPlan.toSnapshot()` at terminal record time. This is the right boundary for preserving v2 JSON without rewriting historical sessions.
- `SessionStepRecord` is still actual-step oriented and does not carry composition metadata fields.
- `TimedRestExtensionRecord` can identify the rest step, round, rest stage id, previous stage id, planned rest, and extension timing, but it does not have explicit `compositionVersion`, `stageGroupId`, or `targetId` fields.
- `TimerDialUiState` currently maps the outer ring to the current legacy work + rest cycle. The inner total marker currently counts only work/custom stages; this must change for v2 mapping, but not in this gate.
- `HistoryUiState` currently reconstructs comparable timed rest descriptors from legacy `TimedCircuitBlock`; v2 trend descriptors are not implemented.

## Timeline Expansion Plan

E14.4-2b-5a has introduced an adapter-owned deterministic timeline model. The adapter rejects unsupported `compositionVersion`, consumes normalized v2 block data, and produces steps from the immutable v2 payload boundary rather than from the mutable editor draft.

Recommended expansion order:

1. If `warmupSec > 0`, emit a single boundary warmup stage before all rounds.
2. For each `roundIndex` from `1..rounds`, iterate normalized `stageGroups` by order.
3. For each stage group instance, emit one executable step per target, ordered by target order.
4. `action` and `custom` targets map to timed work steps. `rest` targets map to timed rest steps.
5. If `restBetweenRoundsSec > 0`, emit one synthetic between-round rest step after each round except the final round.
6. If `cooldownSec > 0`, emit a single boundary cooldown stage after all rounds.

This creates two related units:

- Executable target step: the engine step that ticks, can be skipped, can trigger reminders, and can be recorded.
- Timeline stage instance: the unit used by TimerDial inner progress. Warmup, each stage group instance, each between-round rest, and cooldown each count as one stage instance.

The inner stage count for v2 remains:

```text
(warmupSec > 0 ? 1 : 0)
+ rounds * stageGroups.size
+ (restBetweenRoundsSec > 0 ? max(rounds - 1, 0) : 0)
+ (cooldownSec > 0 ? 1 : 0)
```

## Stable Metadata

Every adapter-expanded executable step should carry stable metadata that can be reconstructed from `WorkoutSession.planSnapshot`:

| Field | Requirement |
|---|---|
| `compositionVersion` | `2`; separates v2 descriptors from legacy descriptors. |
| `compositionBlockId` | Source `TimedCompositionBlock.id`. |
| `timelineStageId` | Deterministic stage instance id including block, boundary/stage/rest kind, round, and stage group where relevant. |
| `timelineStageKind` | Adapter-owned kind such as `warmup`, `stage_group`, `between_round_rest`, or `cooldown`. |
| `stageGroupId` | Source stage group id for repeated stages; deterministic synthetic id for warmup, cooldown, or between-round rest. |
| `targetId` | Source target id for real targets; deterministic synthetic id for warmup, cooldown, or between-round rest. |
| `targetKind` | Source target kind for real targets. Synthetic timeline steps should use adapter-owned boundary/rest kinds without writing them back to the v2 payload. |
| `roundIndex` | 1-based round for repeated stages and between-round rests; absent for warmup/cooldown. |
| `stageGroupIndex` | 1-based normalized stage group position for repeated stages; absent or synthetic for boundaries. |
| `targetIndex` | 1-based normalized target position inside the stage group; `1` for synthetic single-step boundaries/rests. |
| `stageInstanceIndex` | 1-based index across inner TimerDial stage instances. |
| `targetInstanceIndex` | 1-based index across executable target steps. |
| `plannedDurationSec` | Planned duration before runtime extensions. |
| `displayName` | Target name for real targets; localized boundary/rest name for synthetic steps. |
| `colorHex` | Target color for real targets; boundary default or stage/rest color for synthetic steps. |
| `iconKey` | Target or stage icon fallback; optional. |
| `cueSettings` | Resolved from global defaults, optional stage group defaults, then target override. |

Step ids should be deterministic and include enough of this metadata to be reconstructed for session records and E12 descriptors. Runtime-generated random ids should not be used for v2 timeline steps.

## Legacy And V2 Coexistence

- Legacy `TimedCircuitBlock` / `TimedExerciseItem` plans continue through the existing `TimedWorkoutEngine` path.
- v2 plans should later use the adapter-expanded timeline path.
- Old plans and old snapshots must not be silently rewritten into v2.
- Compatibility wrappers may display legacy plans in the composition editor, but only an explicit save / conversion writes the current plan as v2.
- Until engine integration exists, v2 plans must remain not startable and should continue to show `待执行映射完成后可开始`.

## `+15s` Rest Extension Strategy

`WorkoutCommand.ExtendRest(seconds)` should remain unchanged.

Rules for v2:

- It may extend only the current active rest step: either a real target with `targetKind = rest` or a synthetic between-round rest.
- It must not insert a new target or stage.
- It must not mutate `WorkoutPlan`, `WorkoutSession.planSnapshot`, or the v2 payload.
- It must preserve the existing monotonic progress behavior: after extension, current rest progress and total projection must not move backwards.
- Planned TimerDial segment ratios should stay based on original planned durations; the active rest segment can take longer in real time without changing its planned sweep.

## Session Snapshot And Record Impact

Default conclusion for this gate: keep `WorkoutSession.planSnapshot` unchanged and store the original v2 JSON snapshot exactly as the plan was started.

Actual execution should continue to produce actual step records. For the first v2 engine integration, no Room schema or session record model change is required if the adapter guarantees deterministic step ids and descriptors can be reconstructed from `planSnapshot`.

However, if a later story decides records must persist explicit `compositionVersion`, `stageGroupId`, `targetId`, or target metadata fields beyond deterministic ids and snapshot reconstruction, that must be split into a separate migration / compatibility story. This gate does not approve overloading the existing model with silent schema changes.

## WorkoutCommand And WorkoutEvent Impact

Default conclusion: no changes.

The current command surface is sufficient for start, pause, resume, skip, extend rest, and end. The current event surface is sufficient for timed work/rest start, ending reminders, pause/resume, skip, complete, and abandon. If future analytics or sound behavior proves it needs composition metadata in events, record that as a future decision before changing event contracts.

## TimerDial Mapping Inputs

E14.4-2b-6 should not read raw v2 JSON directly. It should consume the adapter-expanded timeline plus the current active stage/target metadata.

Required inputs:

- Stable total stage count from the v2 stage instance formula.
- Completed stage instance count and current stage instance progress for the inner ring.
- Current stage group target list for the outer ring.
- Each target segment's `targetId`, `targetKind`, `plannedDurationSec`, `displayName`, `colorHex`, `progress`, and `isCurrent`.
- Boundary stages and synthetic between-round rests should provide a single segment.
- The 12 o'clock number marker remains the inner total stage count. It must not become target count.
- Rest extension should keep planned ratios stable and rely on monotonic progress clamping.

## E12 Records And Trends Impact

E12 timed comparable descriptors need a v2 branch before v2 sessions are compared:

- Add `compositionVersion` to trend keys.
- Include composition block id, stageGroupId, targetId, targetKind, round index, stage instance index, target instance index, target index/order, planned duration, and a v2 structure signature.
- Legacy and v2 are not comparable by default.
- Compatibility equivalence, if desired, must be explicit and tested.
- Planned rest for v2 must be derived from historical `WorkoutSession.planSnapshot`, including internal rest targets and synthetic between-round rests.
- Extra rest should bind to the active rest target id or synthetic between-round rest id, not to a newly inserted target.
- Existing legacy records with missing metadata should continue to degrade to data-insufficient / structure-different copy rather than creating false trend samples.

## Backward Compatibility

- Existing legacy timed plans remain executable through the current engine.
- Existing historical snapshots remain immutable.
- v2 plans remain editable but not executable until timeline adapter and engine integration are implemented.
- Unsupported v2 versions should fail closed as unsupported, not silently run as a partial timeline.
- The E14.4-2b-4 disabled-start strategy remains correct.

## Recommended Implementation Split

1. **E14.4-2b-5a timeline adapter model/tests**
   - Implemented and pushed as `6888e31`, adapter-only.
   - Adds a pure `TimedCompositionTimeline` / `TimedCompositionTimelineStep` model and `TimedCompositionTimelineAdapter`.
   - Expands normalized v2 payload to deterministic timeline steps and stage instances.
   - Covers warmup, rounds, stageGroups, targets, internal rest, between-round rest, cooldown, unsupported versions, zero-duration boundaries, stable ids, metadata, target / stage instance indexes, order normalization, max-5 target normalization, legacy non-input, and source-boundary guard tests.
   - No engine integration, no TimerDial production mapping, no v2 start enablement, no Room / command / event / session record changes.

2. **E14.4-2b-5b engine integration planning**
   - Documented in `docs/testing/e14-4-2b-5b-engine-integration-planning-gate.md`.
   - Plan to route v2 snapshots through the adapter-expanded timeline at the engine construction boundary in a later implementation story.
   - Keep legacy engine behavior unchanged.
   - Keep `WorkoutCommand` / `WorkoutEvent` unchanged unless a separately approved decision says otherwise.
   - Preserve `+15s` active-rest-only semantics and monotonic progress.
   - Enable v2 start only after coverage proves the mapping.

3. **E14.4-2b-5c session record compatibility tests**
   - Verify `WorkoutSession.planSnapshot` preserves v2 JSON.
   - Verify actual step records and rest extension records remain reconstructable from v2 snapshots.
   - Add E12 descriptor planning/tests for v2 if no schema change is needed.
   - If explicit persisted metadata fields are required, split a separate migration story before implementation.

4. **E14.4-2b-6 TimerDial mapping**
   - Consume adapter-expanded timeline and active stage/target metadata.
   - Change outer ring source to current stage group targets while preserving the production TimerDial UI.
   - Preserve inner 12 o'clock total-stage marker semantics.

5. **Future E14.4-2b-7 compatibility / E12 trend polish if needed**
   - Broader old/new trend compatibility, unsupported snapshot handling, and any explicit migration decisions not covered by 5c.

## Verification Scope

This gate requires docs-only verification:

- app source/test diff must remain empty.
- `git diff --check` must pass.
- staged files must remain empty.
- Search checks must confirm no v2 engine / TimerDial implementation slipped into `feature.workoutsession` or `feature.timer`.
- No Gradle, APK, AVD, smoke, screenshot, logcat, `.local/smoke`, or `.local/verification` output is required or allowed.
