# E14.4-2b-2 Timed Composition Data Model Decision

**Date:** 2026-06-25
**Status:** Decision accepted as planning; E14.4-2b-3 restart model / serializer / editor adapter foundation implemented; E14.4-2b-4 editor UI visual/code gate implemented
**Scope:** Timed composition persistence, compatibility, execution timeline semantics, TimerDial mapping, and E12 history trend impact

**Process note:** Before any further implementation work, read `docs/testing/e14-4-2b-process-reset.md`. The prior E14.4-2b-3 / E14.4-2b-4 local work did not pass review gate and has been rolled back. The accepted E14.4-2b-3 restart starts from the planning documents and implements only model / serializer / editor adapter foundation. The accepted E14.4-2b-4 restart implements only editor UI and editor draft adapter wiring. Do not continue to E14.4-2b-5 / E14.4-2b-6 from the old rolled-back worktree state or from the editor UI story.

## Boundaries

This document records the E14.4-2b-2 decision. The later old E14.4-2b-3 / E14.4-2b-4 local implementation attempts are not accepted and have been rolled back; they must not be treated as completed implementation slices. The accepted E14.4-2b-3 restart implements only the foundation described below. The accepted E14.4-2b-4 restart implements only editor UI and editor draft adapter wiring. Neither slice authorizes engine, TimerDial, Room migration, command/event, session record, sound, or heart-rate behavior changes.

This decision does not change `WorkoutCommand`, `WorkoutEvent`, sound cue semantics, session record semantics, `timedRestExtensionRecords`, heart-rate boundaries, or training interruption logic.

It also keeps the E11.3 boundary: no heart-rate UI, no manual heart-rate input, no unavailable heart-rate placeholder, no average heart-rate trend, no BLE / Huawei SDK / Health Connect / HealthKit / Wear OS integration, and no medical warning or training interruption basis.

## Inputs Read

Planning and project documents:

- `AGENTS.md`
- `skills/bmad-method/SKILL.md`
- `docs/project-status.md`
- `docs/planning/decision-log.md`
- `docs/roadmap-backlog.md`
- `docs/architecture.md`
- `docs/planning/data-contracts.md`
- `docs/planning/e10-training-mode-interaction-plan.md`
- `docs/planning/timer-dial-design-workflow.md`
- `docs/testing/e14-4-2b-timed-composition-timerdial-semantics.md`
- `docs/testing/e14-4-2-plan-edit-detail-visual-proposal.md`
- `DESIGN.md`
- `docs/ui-extension-guide.md`
- `docs/setup.md`

Read-only code model checks:

- `app/src/main/java/com/liujyks/trainflow/core/model/WorkoutPlan.kt`
- `app/src/main/java/com/liujyks/trainflow/core/model/WorkoutSession.kt`
- `app/src/main/java/com/liujyks/trainflow/core/database/entity/WorkoutPlanEntity.kt`
- `app/src/main/java/com/liujyks/trainflow/core/database/entity/WorkoutSessionEntity.kt`
- `app/src/main/java/com/liujyks/trainflow/core/engine/TimedWorkoutEngine.kt`
- `app/src/main/java/com/liujyks/trainflow/feature/workoutsession/TimerDialUiState.kt`
- `app/src/main/java/com/liujyks/trainflow/feature/history/HistoryUiState.kt`
- `app/src/main/java/com/liujyks/trainflow/feature/plans/TimedPlanEditorUiState.kt`

## Decision Summary

TrainFlow should formally adopt the E14.4-2b-1 two-layer timed composition direction as the durable timed-plan model:

- Top configuration keeps `热身`, `放松`, `轮次`, and `轮间休息`.
- `阶段编排` represents only the repeated stages inside each round.
- Each repeated stage contains up to 5 internal targets.
- The default repeated stage has 2 targets: action + rest.
- Stage total duration is derived from its target durations.
- TimerDial production UI is not redesigned.
- TimerDial outer ring maps only the current stage's internal target duration ratios.
- TimerDial inner ring continues to map whole-session stage progress.
- The 12 o'clock number marker continues to show the total expanded execution-stage count.

The recommended persistence strategy is **Option B: a versioned timed composition payload stored inside existing `WorkoutPlan.blocks` JSON**, without changing the Room table shape in the first implementation.

Old single-layer `TimedCircuitBlock` plans remain supported through a compatibility wrapper. Opening or viewing an old plan must not rewrite it. Conversion to the new composition structure should happen only through an explicit user-visible save / conversion path.

## E14.4-2b-3 Restart Implemented Foundation

The prior local E14.4-2b-3 implementation was rolled back and is not accepted. The accepted restart reimplemented this slice from the committed process reset, E14.4-2b-1 visual planning, and E14.4-2b-2 data model decision. Implemented facts:

- Kotlin model now includes versioned timed composition v2 payload types, stage groups, targets, compatibility metadata, normalization, derived duration, order normalization, and the max-5-targets-per-stage-group rule.
- Serializer / deserializer round-trips v2 through existing `WorkoutPlan.blocks` JSON and `WorkoutSession.planSnapshot` JSON, while legacy timed plan JSON round-trip remains unchanged.
- A pure editor draft adapter can wrap old single-layer timed plans for future editor reads, preserves old plans by default, and only writes v2 on explicit export / conversion.
- Room schema, engine execution, TimerDial production behavior, history trend semantics, migration code, commands/events, session record semantics, sound semantics, and heart-rate UI/input/statistics remain unchanged.

Timeline execution expansion, TimerDial outer-ring mapping, E12 trend-key use, and old snapshot rewriting remain unimplemented.

## E14.4-2b-4 Editor UI Visual/Code Gate

E14.4-2b-4 restart implements the accepted editor-only UI over the draft contract:

- editor UI keeps top `热身` / `放松` / `轮次` / `轮间休息` in one compact `基础时间与轮次` card;
- repeated stage groups live under `阶段编排`;
- collapsed stage cards show only swatch, stage name, stage total duration, expand/collapse, and drag handle;
- expanded stage cards expose stage name, color, derived total duration, target list, add target, copy, and delete;
- target rows support collapsed / expanded settings, separate settings and drag handles, and the max-5-targets-per-stage rule;
- save exports editor-side composition v2 payload through the draft adapter;
- v2 `开始训练` remains disabled / gated until execution mapping exists;
- plan detail also keeps saved v2 composition plans non-executable.
- compact bottom navigation labels `训 / 计 / 动 / 录` are retained only as small-screen editor polish; destination and training semantics are unchanged.

Deferred from this slice: complex drag animation, full large-palette reuse for all target colors, advanced cue settings, complete target-kind icon library, engine timeline expansion, TimerDial production mapping, session record semantics, Room schema migration, sound semantics, and heart-rate UI / input / statistics.

## Options Compared

| Criteria | Option A: adapter over existing `TimedCircuitBlock` / `TimedExerciseItem` | Option B: versioned timed composition payload in `WorkoutPlan.blocks` JSON | Option C: new Room entity / table or schema field |
|---|---|---|---|
| Implementation risk | Low at first, but high if treated as real persistence because every edit needs reverse mapping into old fields. | Medium. Requires model / serializer / adapter work, but keeps database table shape stable. | High. Requires schema design, migration, DAO/repository changes, and broader regression. |
| Old plan compatibility | Strong for read/display; weak for arbitrary two-layer edits. | Strong with explicit old-plan adapter and lazy conversion. | Strong only after migration/adapters are written. |
| Old session snapshot readability | Preserved because old snapshots keep old blocks. | Preserved because old snapshots keep old blocks; new snapshots become self-explanatory. | Preserved only if migration does not rewrite snapshots and old readers remain. |
| TimerDial mapping clarity | Partial. Old work/rest pairs can map, but nested target ids and colors are synthetic. | Strong. Current stage id and target ids are explicit. | Strong, but at the cost of schema complexity. |
| E12 statistics / trend impact | Existing keys only; new target-level trends remain fragile. | Best balance. Composition version, stage id, target id, and structure signature can become trend keys. | Strong query potential later, but too much early storage surface. |
| Migration need | No Room migration; still needs adapter tests. | No Room schema migration if stored in existing JSON columns; does need serializer compatibility tests. | Requires Room version bump, migration, schema export, and probably repository tests. |

## Recommended Option

Adopt **Option B**.

Reasoning:

- The E14.4-2b-1 visual structure is real product semantics, not a visual grouping trick.
- Existing `TimedCircuitBlock` and `TimedExerciseItem` cannot durably represent stable internal target ids, per-target colors, target-level cue overrides, and E12 trend keys without brittle reverse mapping.
- Existing Room storage already stores polymorphic plan blocks and plan snapshots as JSON (`workout_plans.blocks_json` and `workout_sessions.plan_snapshot_json`), so the first durable model can extend the JSON contract without adding tables.
- Historical `WorkoutSession.planSnapshot` must remain immutable. A versioned payload lets new sessions explain their exact structure while old sessions keep the old adapter.
- E12 timed comparable trends already depend on strict structure signatures, `stepIndex`, `roundIndex`, `restStageId`, and `previousStageId`. New composition must be explicitly versioned so old and new plans are not accidentally compared.

Room schema migration is **not required** for Option B if the new structure is only a new JSON payload inside existing columns. A Room migration becomes required only if a later story adds a new entity, table, relation, or column.

## User Confirmation Risks

Before implementation, the user should explicitly confirm:

- Old saved plans should not be silently rewritten on open.
- Saving old plans can stay old-compatible if the user only changes fields that map to old structure.
- Adding / editing true internal targets should require an explicit conversion notice before saving as composition v2.
- Conversion is effectively one-way for that current plan revision, although old historical snapshots remain readable.
- Old and new E12 comparable trends should show "结构不同，暂不比较" unless the compatibility mapper proves equivalence.

## Conceptual Data Structure

Recommended conceptual payload inside `WorkoutPlan.blocks`:

```text
TimedCompositionBlock
  id
  kind = "timed_composition"
  order
  compositionVersion = 2
  title?
  warmupSec
  cooldownSec
  rounds
  restBetweenRoundsSec
  stageGroups[]
  cuePolicy?
  compatibility?
```

`stageGroups` is the durable repeated-stage list inside each round. The implementation can name the type `stageGroups` or `stages`; this decision uses `stageGroups` to emphasize that each item owns internal targets and repeats per round.

```text
TimedCompositionStageGroup
  id
  order
  name
  colorHex
  iconKey?
  targets[]
  cueSettings?      // optional stage default, not required for MVP
  compatibility?
```

```text
TimedCompositionTarget
  id
  order
  name
  kind = "action" | "rest" | "custom"
  durationSec
  colorHex
  iconKey?
  cueSettings?
  autoAdvance = true
  compatibility?
```

Conceptual field rules:

- `compositionVersion`: required. Start with `2` to distinguish it from legacy single-layer timed blocks.
- `warmupSec`: composition-level boundary duration; `0` means absent.
- `cooldownSec`: composition-level boundary duration; `0` means absent.
- `rounds`: positive integer, same top-level meaning as today.
- `restBetweenRoundsSec`: composition-level round rest; inserted only between rounds, never after the final round.
- `stageGroups`: repeated stage list. Empty composition is invalid for execution.
- `stage id`: stable within the plan and copied into snapshots.
- `stage name`: user-facing repeated-stage name, soft-limited by UI rules.
- `stage color`: identifies the repeated stage and can be used by center / stage-level UI.
- `target id`: stable within its stage and copied into snapshots.
- `target name`: user-facing target label, soft-limited by UI rules.
- `target kind`: `action`, `rest`, or `custom`; maps to work/rest execution kind.
- `target durationSec`: positive duration. Stage total is `targets.sum(durationSec)`.
- `target color`: maps to TimerDial outer segment color.
- `ordering`: both stage and target order are explicit integers; UI drag commits reorder by id.
- `cue settings`: global defaults remain in `PlanPreferences.cueSettings`; target-level cue settings are the durable override. Stage-level cue settings may exist as a default/template layer, resolved after global and before target, but it should not be required for the first implementation.
- `compatibility metadata`: optional fields may record legacy block id, legacy item id, original stage type, source version, and conversion timestamp for diagnostics and history display. Compatibility metadata must not be required to execute new plans.

## Execution Timeline Mapping

The engine mapping story should expand a composition snapshot into a deterministic `TimedSessionStep` timeline.

Expansion order:

1. If `warmupSec > 0`, add one warmup stage instance before rounds.
2. For each round from `1..rounds`, expand every `stageGroup` in order.
3. Inside each stage group, expand every target in order.
4. If `restBetweenRoundsSec > 0` and the current round is not the final round, insert one between-round rest stage after the round.
5. If `cooldownSec > 0`, add one cooldown stage instance after all rounds.

No between-round rest is inserted after the final round.

Target mapping:

- `target.kind == "action"` maps to `SessionStepKind.TIMED_WORK`.
- `target.kind == "custom"` maps to `SessionStepKind.TIMED_WORK`, with `TimedStageType.CUSTOM` or equivalent metadata.
- `target.kind == "rest"` maps to `SessionStepKind.TIMED_REST`.
- Warmup maps to `TIMED_WORK` with warmup metadata.
- Cooldown maps to `TIMED_WORK` or `STRETCH` only if a later implementation explicitly preserves the current stretch distinction; otherwise it should remain a timed cooldown stage in the timed flow.
- Between-round rest maps to `TIMED_REST` with synthetic round-rest stage / target metadata.

Required step metadata for the future mapping story:

```text
compositionVersion
compositionBlockId
stageGroupId
stageGroupOrder
stageInstanceIndex
stageInstanceKind = warmup | repeated_stage | between_round_rest | cooldown
targetId
targetOrder
targetKind
roundIndex
targetInstanceIndex
plannedDurationSec
```

Existing `SessionStep` does not currently expose these fields. The implementation story should decide whether they live in an internal engine step metadata object first and are projected into records/trends later, or whether `SessionStep` / step records gain explicit metadata in a separate, tested slice. This decision does not authorize changing session record semantics in E14.4-2b-2.

`+15s` rest extension:

- Still only affects the current active rest step.
- Applies to an internal rest target or a synthetic between-round rest target, whichever is active.
- Does not insert a new target.
- Does not modify `WorkoutPlan` or `WorkoutSession.planSnapshot`.
- Continues to record actual extra rest only through `timedRestExtensionRecords`.

## TimerDial Mapping

TimerDial production UI remains the accepted current UI. Only the outer-ring data mapping changes in the later implementation story.

Outer ring:

- The outer ring reads the current stage instance's target list.
- Segment sweep ratios are derived from planned target durations: `target.durationSec / stage.targets.sum(durationSec)`.
- The active target is the thick active arc.
- Completed targets in the same stage instance are shown as elapsed / thin arcs.
- Future targets in the same stage instance remain low-emphasis upcoming arcs.
- If the current stage has only one target, the outer ring is a single segment.
- Warmup, cooldown, and between-round rest can be represented as single-target stages.

Rest extension and outer ratio:

- Segment proportions remain based on planned duration.
- Rest extension must not recalculate the target ratio or resize the rest segment.
- Active rest progress uses a monotonic projected progress floor. If added seconds would mathematically reduce displayed progress, clamp to the previous displayed progress and continue forward from that floor.

Inner ring:

- Inner total stage count is based on expanded stage instances, not internal target count.
- Count formula:

```text
(warmupSec > 0 ? 1 : 0)
+ rounds * stageGroups.size
+ (restBetweenRoundsSec > 0 ? max(rounds - 1, 0) : 0)
+ (cooldownSec > 0 ? 1 : 0)
```

- Example: warmup + 3 rounds x 2 stage groups + 2 between-round rests + cooldown = 10.
- The 12 o'clock marker shows this total count and stays stable for the execution instance.
- Rest extension, pause, skip, completed, or abandoned states must not change the 12 o'clock total count.

Old plan wrapper mapping:

- Old warmup / cooldown map as single-target boundary stages.
- Old `TimedCircuitBlock.items` map as repeated stage groups.
- If an old work item has `restAfterSec`, wrapper can show that stage as action target + rest target.
- If an old item has `stageType == REST`, wrapper can show it as a rest-only stage or group it into the preceding compatible stage only when that relationship is unambiguous.
- `restBetweenRoundsSec` remains top-level round rest, not a movable internal target.
- The wrapper should default to grouping by `stageType` when it improves readability and the old structure is unambiguous; fallback to a single safe compatibility stage when grouping would invent relationships.

## Old Plan Compatibility

Opening an old plan:

- Read the old `TimedCircuitBlock` / `TimedExerciseItem` structure normally.
- Show a compatibility wrapper in the new editor UI state.
- Label the wrapper as old-compatible if needed.
- Do not write back just because the plan was opened.

Default wrapper:

- Prefer stageType-aware grouping for display when the old structure clearly expresses warmup, work, rest, custom, and cooldown roles.
- Use a single outer compatibility stage as fallback for ambiguous old plans.
- Keep `轮次` and `轮间休息` in the top configuration.

Saving behavior:

- Viewing old plan: no rewrite.
- Editing only old-compatible fields: save back the old structure.
- Editing true internal targets or per-target colors beyond the old model: require explicit conversion confirmation and then save as composition v2.
- Do not auto-convert silently.
- If the editor cannot safely save an old ambiguous wrapper, disable save for that unsupported edit and explain conversion is required.

Execution and records:

- Old plans execute with the existing engine mapping until the engine timeline story supports composition v2.
- Old saved plans' ready gate, execution, and records remain unchanged before conversion.
- Old `WorkoutSession.planSnapshot` always renders through the old adapter.
- Converting the current plan never rewrites historical snapshots.

## Room, Serialization, And Migration

Option B storage:

- `workout_plans.blocks_json` stores the new versioned payload.
- `workout_sessions.plan_snapshot_json` stores the same structure in the session snapshot.
- Room table version does not need to change if no entity column / table / relation changes are introduced.

Required non-Room work:

- Serializer / deserializer must read old `TimedCircuitBlock` and new composition v2.
- Unknown timed composition versions must fail closed into an unsupported/read-only state instead of being coerced into old blocks.
- Old JSON must continue to parse.
- New JSON round-trip must preserve stable stage ids and target ids.
- Plan copy must preserve composition semantics while generating new plan-level ids only where already expected.

Failure fallback:

- If a current plan's composition payload cannot be parsed, show it as unsupported timed structure and avoid overwriting it.
- If a historical snapshot cannot parse, history detail should show a safe "snapshot unsupported" explanation instead of using the current plan.
- Do not synthesize trend samples from partially parsed structures.

Migration posture:

- Use lazy conversion, not one-way eager migration.
- Current plan conversion happens only on explicit save / conversion.
- Historical sessions are never migrated or rewritten.
- If a later Option C story adds tables or columns, split it into a dedicated Room migration story with schema export and  migration tests.

## E12 History And Trend Impact

Current E12 timed comparable rest trend keys use:

- structure signature
- stage type
- stage order
- round index
- rest stage id
- previous stage id
- step index

Composition v2 must extend that strictness:

- Include `compositionVersion` in every structure signature.
- Include `compositionBlockId`.
- Include `stageGroupId` and `stageGroupOrder`.
- Include `targetId`, `targetOrder`, and `targetKind` for rest targets.
- Include `roundIndex` and the expanded `stageInstanceIndex`.
- Include previous non-rest target id / stage id for rest-extension context.
- Keep planned duration in the structure signature so a duration edit can be treated as a changed structure when required.

Old and new structures:

- Do not compare old single-layer and new composition v2 by default.
- Allow comparison only if a compatibility mapper proves the old and new signatures are equivalent at stage/target level.
- Otherwise display "结构不同，暂不比较" or data-insufficient copy.

When to show "结构不同，暂不比较":

- Composition version differs and no equivalence mapper exists.
- Stable stage id or target id is missing.
- Target order changed.
- Target kind changed between action/rest/custom.
- Round count or between-round rest changed.
- A rest target has no previous action/custom target relationship.
- A session snapshot is unsupported or partially parsed.

Rest extension trend binding:

- `+15s` extra rest binds to the active rest target id when the target is internal rest.
- Between-round rest binds to a synthetic stable round-rest target id under the composition block.
- Existing `timedRestExtensionRecords` should remain the actual source of `addedSec`.
- Future records may need additional metadata, but the semantic source remains the current active rest step and saved snapshot.

Plan snapshot principle:

- E12 must always use each `WorkoutSession.planSnapshot`.
- E12 must not use the current edited `WorkoutPlan` to reinterpret old sessions.
- Converting a plan to composition v2 affects only future sessions.

## Follow-Up Implementation Split

Recommended sequence:

1. **E14.4-2b-3 serializer / model and editor adapter foundation**
   Restart implemented. Scope is limited to pure model, serializer, compatibility wrapper / editor draft adapter, and focused tests. It does not add UI, execution, TimerDial, Room migration, E12 trend-key consumption, or APK output.

2. **E14.4-2b-4 editor UI visual/code gate**
   Implemented as editor-only UI and editor draft adapter wiring. It does not authorize runtime execution, TimerDial production mapping, Room migration, E12 trend-key consumption, or APK release claims beyond the smoke evidence for this story.

3. **E14.4-2b-5 TimedWorkoutEngine timeline mapping**
   Expand composition v2 into deterministic timeline steps, preserve ready gate, pause/resume, skip, final countdown, sound cue event semantics, and `+15s` active rest semantics.

4. **E14.4-2b-6 TimerDial mapping implementation**
   Keep current TimerDial UI and remap only outer-ring data to current stage targets. Preserve continuous projection, reduce-motion, pause freeze, terminal freeze, final countdown, and rest-extension monotonic progress.

5. **E14.4-2b-7 migration / compatibility / E12 tests**
   Cover old plans, old snapshots, new composition snapshots, unsupported versions, explicit conversion, serializer round-trip, timeline expansion, TimerDial mapping, and E12 trend keys.

Future v2 editor work must remain editor-only for v2 execution. Do not ship UI-only arbitrary nested editing as runtime behavior until timeline and TimerDial mapping stories are implemented.

## Decision Log Entry

This decision closes O-008 as accepted: TrainFlow should adopt a versioned two-layer timed composition model as the long-term data direction, using a JSON payload in existing plan/snapshot storage first, with old-plan compatibility wrappers and explicit lazy conversion.

## Verification Notes

This decision document does not require Gradle, APK, or prototype checks. The required verification for this docs-only round is:

```powershell
git diff --check
```

Also run the requested heart-rate regression text search and confirm any matches are existing boundary / prohibition references, not newly reintroduced first-version UI tasks.
