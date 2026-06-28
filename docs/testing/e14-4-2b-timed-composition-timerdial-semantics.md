# E14.4-2b Timed Composition Editor And TimerDial Ring Semantics

**Date:** 2026-06-24
**Status:** Visual / semantic gate retained; E14.4-2b-1 visual prototype retained; E14.4-2b-2 data model decision retained as planning; E14.4-2b-3 restart model / serializer / editor adapter foundation implemented; E14.4-2b-4 restart editor UI visual/code gate implemented; E14.4-2b-5 engine timeline planning gate completed as docs-only; E14.4-2b-5a timeline adapter model/tests implemented; E14.4-2b-5b engine bridge implemented through split gates; E14.4-2b-5c session record compatibility verified; E14.4-2b-6 TimerDial mapping planning gate completed docs-only; TimerDial visual correction overlay retained
**Scope:** Timed two-level composition, TimerDial ring semantics, data impact, compatibility, and implementation split

**Process note:** Before further implementation work, read `docs/testing/e14-4-2b-process-reset.md`. The prior E14.4-2b-3 / E14.4-2b-4 local Kotlin / Compose / test implementation did not pass review gate and has been rolled back to the visual-gate baseline. Restarted E14.4-2b-3 covers only model / serializer / editor adapter foundation, and restarted E14.4-2b-4 covers only the editor UI visual/code gate. E14.4-2b-5 / 5a / 5b / 5c have since closed the timeline, bridge, start gate, and session-record compatibility path. E14.4-2b-6 is closed only as a docs-only TimerDial mapping planning gate; TimerDial production mapping remains forbidden until the 6a test-first split is accepted.

## Boundaries

This document is retained as the visual / semantic gate plus visual artifact record. The prior E14.4-2b-3 model / serializer / editor adapter implementation and E14.4-2b-4 editor-only Compose UI implementation were local, did not pass review gate, and are not accepted product code. The accepted E14.4-2b-3 restart adds only the data foundation and compatibility adapter; the accepted E14.4-2b-4 restart adds only the editor UI visual/code gate. Later split gates added the adapter timeline, minimum engine bridge, v2 start gate, and session record compatibility coverage. None of those gates implements TimerDial production mapping.

This gate intentionally does not change Kotlin, Compose, Room, tests, workout engines, `WorkoutCommand`, `WorkoutEvent`, session record semantics, sound cue semantics, history deletion, plan snapshot records, or APK output.

This gate also keeps the E11.3 health boundary: no heart-rate UI, no manual heart-rate input, no unavailable heart-rate placeholder, no average heart-rate trend, no BLE / Huawei SDK / Health Connect / HealthKit / Wear OS integration, and no medical warning or training interruption logic.

## E14.4-2b-2 Data Model Decision Result

The follow-up data model decision is recorded in:

```text
docs/testing/e14-4-2b-timed-composition-data-model-decision.md
```

Accepted direction:

- Formally adopt the two-layer timed composition model as the long-term data direction.
- Use a versioned timed composition payload first, still stored inside existing `WorkoutPlan.blocks` JSON and `WorkoutSession.planSnapshot` JSON.
- Do not add a Room table, Room column, Room schema migration, Kotlin implementation, Compose implementation, engine mapping, TimerDial mapping, tests, or APK in E14.4-2b-2.
- Keep legacy `TimedCircuitBlock` / `TimedExerciseItem` support through compatibility wrappers.
- Opening or viewing an old plan does not write it back.
- User-visible save / conversion is required before a legacy plan becomes composition v2.
- Historical `WorkoutSession.planSnapshot` remains immutable and renders through its original adapter.
- `restBetweenRoundsSec` remains top-level round configuration and is inserted between rounds only during timeline expansion.
- TimerDial outer ring maps current stage targets by planned duration ratio; rest extension keeps planned segment ratios stable and uses monotonic progress clamping.
- TimerDial inner total count is computed from the expanded execution stage timeline: warmup + rounds * stageGroups + between-round rests + cooldown.
- E12 timed trend keys must include composition version, stage id, target id, round / stage instance, and structure signature; old and new structures do not compare unless a compatibility mapper proves equivalence.

This closes the prior open question about whether E14.4-2b should become durable product semantics. It should now proceed as an explicit model / serializer / adapter implementation story, not as a UI-only wrapper pretending to be persistence.

## E14.4-2b-3 Restart Implemented Foundation

The prior E14.4-2b-3 local implementation attempted the durable payload foundation but was rolled back and is not accepted. The accepted restart implements only the model / serializer / editor adapter foundation:

- versioned timed composition v2 is represented as a pure Kotlin payload carried by existing plan / snapshot JSON;
- old timed plans must not be silently rewritten and default adapter export preserves the source plan;
- explicit export / conversion is required before a legacy plan writes composition v2;
- execution, TimerDial, Compose editor UI, `WorkoutCommand`, `WorkoutEvent`, session record, Room schema, sound semantics, and heart-rate UI/input/statistics remain untouched.

## E14.4-2b-4 Restart Implemented Editor UI Gate

The prior E14.4-2b-4 local editor UI implementation was stopped, rolled back, and is not accepted. A later clean restart implemented only the editor UI visual/code gate and was pushed as `d8d784d`; it must not be treated as engine or TimerDial support.

Accepted restart boundary:

- keep the current timed editor structure;
- keep warmup / cooldown / rounds / between-round rest as top configuration;
- keep repeated stage composition below that configuration;
- allow repeated stages to expand into compact internal targets;
- cap internal targets at 5 per repeated stage;
- save editor-side v2 payload;
- keep v2 start disabled until explicit engine timeline stories exist; later E14.4-2b-5b-3 replaced this with adapter-expandable / fail-closed start gating.

Accepted production code now supports v2 execution through the minimum engine bridge, but no accepted production code currently implements v2 TimerDial mapping.

## E14.4-2b-1 Visual Prototype Result

The visual prototype / mock for the timed composition editor and TimerDial outer-ring semantic sketch is now available at:

```text
.local/smoke/e14-4-2b-timed-composition-timerdial-semantics/index.html
```

The corrected complete visual design draft, combining the accepted editor direction with the corrected existing-UI TimerDial overlay, is:

```text
.local/smoke/e14-4-2b-complete-visual-design/index.html
```

This complete draft should be the primary E14.4-2b-1 discussion artifact. The older `timed-composition-timerdial-semantics` mock remains useful for historical comparison, and the `timerdial-existing-ui-overlay` mock remains useful as a narrow TimerDial correction, but the complete draft is the one that keeps both sides together:

- Current editor structure: `轮次` and `轮间休息` stay in the current top-side position.
- `热身` and `放松` are independent stage time settings above the repeated stage composition. They are not ordinary rows inside the repeated stage list.
- Stage composition remains below the warmup / cooldown and round controls, and represents only stages repeated inside each round.
- Stage cards default collapsed for long-plan readability.
- Expanded stages reveal compact internal targets / goals.
- Each repeated stage can contain up to 5 internal targets. The common default is 2 targets: work / action and rest.
- Color entries show swatches directly at the selection location instead of Chinese color-name text.
- Old plans can be displayed through a compatibility wrapper without silent conversion.
- TimerDial keeps the accepted production UI and only overlays the outer ring by current-stage internal target duration ratio.
- The TimerDial drawing inside the complete HTML draft is a simplified semantic explanation. It is not a replacement visual spec and must not be used to change the original production TimerDial UI or animations.

This is a local HTML/CSS/JS review artifact only. It does not import remote dependencies, does not change Android production code, does not write Kotlin / Compose / Room / tests, does not generate an APK, and does not change `WorkoutPlan`, `TimedCircuitBlock`, `TimedExerciseItem`, `TimerDial`, workout engines, `WorkoutCommand`, `WorkoutEvent`, session record semantics, Room schema, or sound cue semantics.

### TimerDial Existing UI Overlay Correction

The first TimerDial mock in `.local/smoke/e14-4-2b-timed-composition-timerdial-semantics/index.html` was too far from the accepted production TimerDial UI. It should be treated as a semantic sketch only, not as a visual direction.

The corrected overlay mock is:

```text
.local/smoke/e14-4-2b-timerdial-existing-ui-overlay/index.html
```

This corrected mock uses the current accepted TimerDial production UI as the visual baseline and only overlays the outer ring. It is still local mock-only and does not enter production code.

Baseline used for this correction:

- User-provided normal TimerDial screenshot: `C:/Users/25073/Downloads/Screenshot_2026-06-21-22-30-15-56_168a3d1b6f3b71..jpg`.
- Current TimerDial baseline descriptions in `docs/testing/e14-2-timer-dial-real-device-proportion-restore.md`, `DESIGN.md`, and `docs/planning/timer-dial-design-workflow.md`.
- Current production TimerDial code:
  - `app/src/main/java/com/liujyks/trainflow/feature/workoutsession/TimerDial.kt`.
  - `app/src/main/java/com/liujyks/trainflow/feature/workoutsession/TimerDialTokens.kt`.
  - `app/src/main/java/com/liujyks/trainflow/feature/workoutsession/TimerDialUiState.kt`.
  - `app/src/main/java/com/liujyks/trainflow/feature/workoutsession/TimedWorkoutSessionRoute.kt`.

Important correction: `.local/smoke/e14-2-runtime/running.png` exists, but its yellow center-circle visual is **not** the accepted visual baseline for this task. It is not inherited by the overlay mock.

Production-code anchors checked for this correction:

- `TimerDialVisualVariant.OFFICIAL_FLOW` remains the production variant.
- Normal layout keeps `dialSizeDp = 320`, `centerSizeDp = 180`, `innerInsetDp = 42`, `outerMaxStrokeDp = 14`, and `innerBaseStrokeDp = 24`; the overlay must not resize those relationships.
- `TimedWorkoutExecutionScreen` keeps a non-appbar top plan title, a separate large total-remaining block, `TimerDialPauseMorph`, and the bottom control row.
- Existing outer segments start at 12 o'clock (`-90f`) and use duration-based `rawSweep` with a small multi-segment gap; the new inner-target overlay should reuse that direction instead of inventing a new radial system.
- The 12 o'clock marker currently comes from `TimerDialInnerMarkerRole.TOTAL_COUNT` / `totalWorkoutStageCount`, so changing its meaning requires a later explicit decision.

The corrected mock keeps these production UI elements unchanged:

- Full-screen dark training execution page.
- No appbar.
- No phone shell.
- No explanatory text inside the simulated phone UI.
- Top plan / training name.
- Top total remaining time position and hierarchy.
- TimerDial size, round geometry, and visual center.
- Center circle size, position, and countdown hierarchy.
- Inner ring nodes and whole-session progress semantics.
- 12 o'clock number marker position, size, and visual layer.
- Bottom three large controls and bottom safe-area layout.
- Existing TimerDial animations, including `TimerDialPauseMorph`, continuous progress projection, reduce-motion behavior, final-countdown pulse, rest-extension monotonic progress, center-dial touch feedback, marker / ring / center color transitions, and bottom-control motion.

The only changed visual area is the outer ring:

- Baseline frame: original single outer-ring progress segment.
- Action/rest frame: `动作 45s / 休息 15s`, split as `3 : 1`.
- Same-ratio longer frame: `动作 90s / 休息 30s`, also split as `3 : 1`; this confirms the outer ring is ratio-based and not a fixed 60-second circle.
- Running state: active inner stage / target uses the thick active arc.
- Rest state: active rest inner stage / target uses the thick active arc; completed targets recede to thin / elapsed arcs.
- Paused state: production paused semantics remain; outer ring, inner ring, center countdown, and projection freeze.
- Completed / target-complete state: outer ring freezes and does not go backward.

Deprecated from the previous mock:

- Appbar.
- Phone shell.
- Explanatory copy inside the mock phone screen.
- Small pill controls.
- New ring proportions.
- New layout and visual center.
- Any implication that the 12 o'clock number marker should be visually redesigned in this pass.

### User Correction And Scope Narrowing

After reviewing the first visual prototype, the user clarified that E14.4-2b-1 must **not** expand beyond the already accepted TrainFlow UI direction:

- Keep the original timed editor UI structure. The top-side position for **round count** and **rest between rounds** must remain where it is today.
- Keep the existing stage composition area below that top configuration. This task is not a broad plan-editor redesign.
- The new work is that each existing timed stage can expand to contain more internal targets / goals.
- The TimerDial UI should remain the original accepted UI. This task only changes how the **outer ring** is segmented: because a stage can contain more internal targets, the outer ring should split by each target's planned duration ratio.
- Color selection entries should display the color itself at the selection location. They should not display Chinese color names such as red / orange / green.

This correction supersedes any wording in the first prototype that implied a new replacement editor layout, a redesigned dial, or a new 12 o'clock marker decision. The compact folded stage card and expanded internal-target row density can still be used because the user confirmed that direction feels compact without being cramped.

### Editor Visual Decisions Still Active

The TimerDial correction does **not** remove the already discussed editor direction. These editor decisions remain active:

- Keep the current timed editor UI structure.
- Keep `热身` and `放松` as independent stage time settings.
- Keep `轮次` and `轮间休息` in their current top-side position.
- Keep `阶段编排` below those controls, and treat it as the list of repeated stages inside each round.
- The new capability is inside each repeated timed stage: a stage can expand to contain more internal targets / goals.
- Each repeated stage has a maximum of 5 internal targets. The default recommendation remains 2 targets, action + rest, while the extra slots allow user-designed patterns.
- Stage cards can default collapsed and use the compact, not-cramped density from the first E14.4-2b-1 prototype.
- Expanded stages can show compact internal-target rows with target name, duration, color swatch, drag entry, edit / copy / delete, and add-target entry.
- Stage total duration remains visually derived from the sum of its internal target durations.
- Folded / expanded stage card headers should show only the derived total duration, such as `60s`. Do not show inline equations such as `2:30 = 45s + 60s + 30s + 15s`, and do not put target-count / remaining-slot copy in the card header, because long plans become noisy and overflow-prone. Target count, target details, and remaining capacity can be shown after expansion.
- Text containment rule: stage names should be designed around a soft limit of 10 CJK characters / 20 ASCII characters; target names around 6 CJK characters / 14 ASCII characters. Future implementation should apply a character limit or warning, single-line ellipsis in list rows, fixed operation columns, and `minmax(0, 1fr)` style layout so action buttons cannot push text outside the card.
- Color selection locations should show the color swatch itself; do not show Chinese color names as the primary field text.
- TimerDial then maps the current active stage's internal targets to the existing outer ring by planned duration ratio.

The corrected TimerDial overlay mock is a narrow artifact for the execution dial only. It does not replace the composition-editor mock or the accepted editor layout direction.

### Prototype Coverage

The prototype covers four review views / states:

1. **Stage composition collapsed list:** a 720 x 1280 mobile viewport mock with warmup, cooldown, rounds, and round rest combined into one compact top settings card, then the repeated stage composition list below. Each repeated stage shows only stage name, color swatch, total duration, collapsed state, and drag entry.
2. **Stage expanded + internal targets:** an expanded repeated stage with derived total duration, stage name, color swatch-only entry, compact internal-target rows, per-target duration, color swatch, explicit fold / unfold control, drag entry, edit / copy / delete actions, and an add-target entry capped at 5 targets. The default example is `动作 45s / 休息 15s`.
3. **Old-plan compatibility wrapper:** a switchable mock for old single-layer timed plans. Scheme A wraps all old stages in one compatibility stage. Scheme B groups old stages by `stageType` into warmup / work / rest / cooldown display stages. Both are labeled as visual wrappers only; viewing the old plan does not rewrite persisted data.
4. **TimerDial outer-ring semantic sketch:** an SVG-based dark TimerDial mock that keeps the original accepted dial structure, while the outer ring shows the active timed stage's internal targets by planned duration ratio. The active target is thicker, completed targets remain visible as elapsed / thin arcs, the inner ring still expresses whole-session progress, and the center circle keeps the existing current stage / countdown / pause-resume role.

It also includes explicit rest-extension / paused / completed notes:

- `+15s` remains the current active rest-step extension.
- Rest extension must keep an outer-ring and inner-ring progress floor and must not visually go backward.
- Paused freezes outer ring, inner ring, center countdown, and continuous projection.
- Completed freezes at completion and does not keep animating.

### Recommended Visual Direction

Use the visual direction represented by the prototype:

- Keep the current timed editor information architecture, but separate the layers precisely: warmup / cooldown are independent stage time settings; round count and rest between rounds stay in the top area; repeated stage composition stays below.
- Repeated timed stages behave like a training playlist and default collapsed.
- Expanding a stage reveals compact internal targets rather than nested large cards.
- Collapsed repeated-stage cards should show only the total duration under the name. Target count, target names, and remaining capacity stay inside the expanded view.
- Internal target rows also have folded and expanded states. Their fold / unfold affordance must be separate from the drag handle so the user can configure a target without accidentally starting drag.
- Cap each repeated stage at 5 internal targets. Use 2 internal targets, action + rest, as the normal default.
- Stage and internal-target drag handles are visually separated; only one drag layer should be active at a time.
- Stage color and target color use swatch-only entries at the selection location. Do not show Chinese color names in the editor fields.
- Stage total duration is always a derived value from internal target durations.
- The editor remains a light, information-dense planning surface; TimerDial remains the already accepted dark dial UI, with only the outer-ring segmentation updated.

This direction intentionally uses Option A as a compatibility / visual validation layer only. It should not be mistaken for final persistence semantics.

### 12 O'clock Number Marker Recommendation

User correction: this task should not redesign the dial or make a new visible 12 o'clock marker decision. Keep the original accepted TimerDial marker behavior unless a later implementation story proves a concrete conflict.

The first prototype compared three interpretations, but they are now secondary notes rather than a new recommendation:

| Option | Meaning | Pros | Cons | Recommendation |
|---|---|---|---|---|
| Outer target total count | Visible marker shows total outer targets in this execution timeline. | Clear division of labor in the early draft. | Expanded rounds need a precise total-count rule, and the user clarified not to redesign the dial. | Historical draft only; not current recommendation. |
| Current target index / total | Visible marker shows `current / total`, such as `2/4`. | Strong orientation during execution. | Wider marker, more visual noise on small screens; may compete with center stage info. | Good accessibility label or secondary text, not default visible marker. |
| Current target inner-stage count | Visible marker shows stage count inside the active target. | Explains the outer ring quickly. | Repeats information already visible in the outer ring and changes meaning on target switch. | Not recommended as default. |

Updated recommendation: **keep the original UI marker semantics for now**. The 12 o'clock marker belongs to the inner whole-session stage-progress UI, not to the outer ring. Current production code maps that marker from `TimerDialInnerMarkerRole.TOTAL_COUNT` / `totalWorkoutStageCount`.

Example using the clarified editor structure:

```text
热身
第 1 轮：阶段一、阶段二、轮间休息
第 2 轮：阶段一、阶段二、轮间休息
第 3 轮：阶段一、阶段二
放松
```

This expands to 10 total workout stages, so the 12 o'clock marker shows `10`. If 6 stages are completed and stage 7 is running, the inner ring shows the completed-stage marker at `6` and the current total-progress brush between stage 6 and 7. This is independent of the outer ring target count.

### Old-Plan Compatibility Wrapper Recommendation

Use **Scheme B: group by `stageType`** as the default old-plan visual wrapper only when old plans need a compatibility explanation. Keep Scheme A available as the safest fallback when the old structure is ambiguous. This wrapper should not pull the production editor away from the current round / round-rest / stage-composition layout.

Rules:

- Opening or viewing an old single-layer plan must not rewrite it.
- Before the data model story, saving from wrapper mode should preserve old structure or be disabled for unsupported arbitrary nesting.
- After explicit composition model approval, saving / converting must be user-visible; no silent conversion.
- Historical `WorkoutSession.planSnapshot` remains immutable and renders through the old adapter.

### Data Model Decision Questions Answered

E14.4-2b-2 has now answered these questions in `docs/testing/e14-4-2b-timed-composition-data-model-decision.md`:

1. Is Option B `explicit timed composition model` accepted as durable product semantics?
2. Is the model a new `PlanBlock` subtype, a versioned timed payload, or another JSON contract inside existing persisted plan data?
3. What stable ids exist for existing timed stage, internal target, target instance / round, and converted old structures?
4. How does `restBetweenRoundsSec` remain in its current top-side editor position while execution still expands it correctly between rounds?
5. How should internal target color persist separately from stage color and plan-level color?
6. How should old single-layer plans save before and after conversion?
7. How should old `WorkoutSession.planSnapshot` and new composition snapshots render side by side?
8. Do serializer / adapter changes require Room schema version changes, schema export updates, or only JSON compatibility tests?
9. How do E12 comparable trend keys include composition version, stage id, internal target id, round / target-instance index, and old-plan compatibility?
10. Does TimerDial need any marker metadata beyond the original UI once the outer ring is segmented by internal target duration ratio?

### Still Not Allowed To Enter Code Implementation

The prototype does not authorize implementation of:

- New `WorkoutPlan`, `TimedCircuitBlock`, or `TimedExerciseItem` fields.
- TimerDial production remapping.
- Workout engine changes.
- `WorkoutCommand` / `WorkoutEvent` changes.
- Session record or plan snapshot semantic changes.
- Room schema changes or migrations.
- Sound cue semantic changes.
- Any production health-device, health-data, or medical judgment feature.

## Inputs Read

- `AGENTS.md`
- `skills/bmad-method/SKILL.md`
- `huashu-design` skill instructions
- `docs/project-status.md`
- `docs/planning/decision-log.md`
- `docs/planning/product-brief.md`
- `docs/planning/prd.md`
- `docs/planning/ux-design.md`
- `docs/planning/data-contracts.md`
- `docs/roadmap-backlog.md`
- `docs/architecture.md`
- `docs/readiness-report.md`
- `DESIGN.md`
- `docs/ui-extension-guide.md`
- `docs/planning/e10-training-mode-interaction-plan.md`
- `docs/planning/timer-dial-design-workflow.md`
- `docs/testing/e14-3-ui-quality-audit.md`
- `docs/testing/e14-4-2-plan-edit-detail-visual-proposal.md`
- `docs/setup.md`
- `.local/smoke/e14-4-2-plan-edit-detail-visual-proposal/index-v2.html`
- Read-only source inspection of `WorkoutPlan`, `WorkoutSession`, `TimerDialUiState`, and timed comparable rest trend mapping to assess data impact.

## Current Problem / Opportunity

E14.4-2 made timed stage cards more usable, but the current timed plan is still conceptually single-layer. A user can list stages such as `热身`, `工作`, `休息`, and `放松`, and the engine can execute them, but the editor cannot clearly express a higher-level training intention such as:

```text
轮次 3
轮间休息 60s

热身
高强度工作
  01 开始 10s
  02 加速 10s
  03 休息 10s
放松
```

The single-layer model is enough for a simple interval timer, but it becomes weak when a stage needs more internal targets than a single flat row can express. It also makes TimerDial ambiguous: if the editor allows a stage to expand into internal targets, the dial must decide whether its outer ring describes the current target, the whole workout, or the old work/rest cycle.

This affects TimerDial because the current production semantic is:

- Outer ring: current work + rest cycle.
- Inner ring: whole workout progress by workout-stage cycles.
- 12 o'clock marker: total workout-stage count.

The proposed two-level composition changes only the outer ring's segmentation job. The original TimerDial UI remains good and should be preserved; the outer ring should split by the active stage's internal target duration ratios when that stage contains multiple targets.

## Desired Two-Level Concept

Terms used in this gate:

| Term | Meaning |
|---|---|
| Independent stage settings | `热身` and `放松`. They are independent time settings and still contribute to the executed total-stage timeline. |
| Round settings | `轮次` and `轮间休息`. They stay in the current top-side editor area. Round rest is inserted between rounds during execution, not edited as a normal repeated-stage row. |
| Repeated timed stage / composition stage | A stage row inside `阶段编排`, such as `阶段一 · 高强度工作` or `阶段二 · 冲刺组合`. It repeats according to round count. |
| Internal target / segment | A timed target inside a stage, such as `开始 10s`, `加速 10s`, or `休息 10s`. |
| Stage duration | Sum of all internal target durations. It is derived, not manually edited separately. |
| Stage color | The existing stage color for editor scanning and current-stage identity. |
| Internal target color | The target segment color for TimerDial outer-ring segments. It should be shown as a swatch-only entry in the editor. |

Default editor template should be:

```text
热身 60s
放松 90s
轮次 3
轮间休息 60s

阶段编排
  阶段一：动作 45s / 休息 15s
  阶段二：动作 90s / 休息 30s
```

Repeated stages can be named, reordered, deleted, copied, and colored as today. Internal targets should be named, reordered, deleted, copied, colored, and have editable duration, with a maximum of 5 targets per repeated stage.

Stage cards should keep the compact, foldable pattern from the current plan-editor direction. Internal targets may keep sequence labels such as `01 开始 / 02 加速 / 03 休息`, because the user is editing a smaller ordered structure inside one stage.

## Option A: UI-Only Compatibility Wrapper

Option A introduces a UI-state adapter that wraps the existing `WorkoutPlan.blocks` into current stage rows plus internal targets without changing persisted models.

Mapping idea:

- Existing `WarmupBlock` remains a stage named from `title` or `热身`, with one or more internal targets in UI state.
- Existing `CooldownBlock` / `StretchBlock` remains a stage named `放松` or `拉伸`.
- Existing `TimedCircuitBlock.items` remain the current editable stage sequence. A stage can expand into internal targets in UI state when the visual adapter can do so truthfully.
- Existing `restBetweenRoundsSec` remains in the current round / rest-between-rounds top area. It should not be freely moved into the stage list unless a later model story says otherwise.
- Existing `RestBlock` remains a rest stage with one internal target.

Pros:

- No Room schema change in the visual/prototype phase.
- Can validate the editor layout, target collapse/expand behavior, color hierarchy, and TimerDial mapping before model work.
- Old plans can open without migration.
- Historical `WorkoutSession.planSnapshot` remains unchanged and explainable.

Cons:

- It cannot safely persist arbitrary internal target color unless the old structure has a truthful field for it.
- It cannot represent nested composition as a durable contract. The wrapper must reverse-map every edit into old `WarmupBlock` / `TimedCircuitBlock` / `RestBlock` shapes.
- It is easy to create misleading UI if `restBetweenRoundsSec` is shown as a freely movable stage even though the current UI treats it as top-side round configuration.
- It does not give E12 a stable composition target id, so trend comparison still depends on existing stage / step / rest ids.

Best use:

- A visual prototype or early editor UI-state spike only.
- A compatibility layer for opening old plans and old snapshots.
- Not the final persistence model if arbitrary two-level editing is approved.

## Option B: Explicit Timed Composition Model

Option B introduces an explicit two-level timed composition contract, with stable stage ids and stable internal target ids.

Possible shape for a future model story:

```text
TimedCompositionBlock
  id
  order
  title
  targets[]

TimedCompositionStage
  id
  order
  title
  colorHex
  repeatPolicy? or roundMembership?
  targets[]

TimedCompositionTarget
  id
  order
  title
  stageType
  iconKey
  colorHex
  durationSec
  cueSettings?
  autoAdvance
```

This is only a contract sketch, not code. The model story must decide whether the new contract is a new `PlanBlock` subtype, a versioned timed payload inside existing `TimedCircuitBlock`, or a separate timed-composition field in a JSON payload.

Pros:

- Correctly represents the user-facing concept.
- Gives TimerDial stable target and stage ids.
- Makes plan snapshots self-explanatory: history can show the exact composition the user trained with.
- Enables reliable compatibility rules and trend grouping because composition version and ids can be part of the structure signature.
- Avoids encoding target semantics into unrelated fields.

Cons:

- Requires model / serialization decisions.
- May require Room migration if entity fields, schema export, serializers, or type adapters need to change. If blocks remain JSON in the same table, a table schema migration may not be required, but model and data compatibility tests still are.
- Requires old-plan adapter, old-snapshot adapter, and likely E12 comparison-key updates.
- Higher implementation cost and broader regression surface.

Best use:

- Final implementation target if two-level timed composition is confirmed.
- Required before treating internal target color, arbitrary target nesting, or target-level history as durable product semantics.

## Option C: Visual Grouping Only

Option C keeps the current single-layer plan and only visually groups adjacent stages in the editor. TimerDial remains unchanged.

Pros:

- Very low risk.
- No persistence change.
- No TimerDial migration.

Cons:

- Does not solve the semantic mismatch. The editor would imply targets that execution does not understand.
- Cannot support true target-level drag, delete, rename, or color.
- TimerDial outer ring would still show old current-cycle semantics, so the execution page would not reflect the editor's grouping.

Best use:

- Not recommended except as a temporary copy / layout cleanup if E14.4-2b is postponed.

## Comparison

| Criteria | Option A: UI wrapper | Option B: explicit model | Option C: visual grouping |
|---|---|---|---|
| Persistence risk | Low | Medium / high | Low |
| Editor semantic truth | Medium | High | Low |
| TimerDial semantic fit | Medium if mapped carefully | High | Low |
| Old plan compatibility | High | High with adapter | High |
| Long-term maintainability | Medium | High | Low |
| Trend key stability | Existing only | Best, with versioned keys | Existing only |
| Implementation cost | Medium | High | Low |

## Recommendation

Recommend **Option B as the product target**, but do not jump straight into a one-shot implementation.

Recommended sequence:

1. Use **Option A as a compatibility and visual-prototype layer** to validate the editor layout and TimerDial mapping without writing new persistence.
2. Run a dedicated **data model decision story** before production editor implementation.
3. If the user confirms two-level composition as durable plan semantics, implement **Option B with compatibility adapters**.
4. Keep old plan snapshots read through the old adapter. Do not rewrite historical `WorkoutSession.planSnapshot`.

Why:

- The two-level concept is real product semantics, not decoration.
- The current `WorkoutPlan.blocks` can approximate some cases, but it cannot fully represent stages with arbitrary nested internal targets and per-target color without fragile reverse mapping.
- E12 timed comparable trends already use strict structure signatures, `stepIndex`, `roundIndex`, `restStageId`, and `previousStageId`. A silent structure rewrite would create false comparisons or unexplained data gaps.
- TimerDial outer-ring semantics should follow the editor's composition model. That needs stable UI state semantics before code.

Risks:

- Model drift if UI-only wrapper ships as if it were final.
- Old plans may be silently rewritten if save behavior is not explicit.
- Rest extension can make visual progress appear to go backward unless the ring mapping preserves a progress floor.
- E12 comparable rest trends can break if target / stage ids are regenerated or if old and new structures share a misleading signature.

User confirmation needed:

- Whether Option B is accepted as the long-term target.
- Whether old single-layer plans should save back as old structure until the user explicitly edits composition, or convert on first save in the new editor.
- How to keep `轮次` and `轮间休息` in their current top-side editor position while still letting stage internals expand.
- Whether TimerDial needs any new marker metadata at all, since the user has clarified that the original dial UI should remain.

## TimerDial Semantics Draft

### Outer Ring

The outer ring should keep the original dial UI and express the **current timed stage's internal target structure**.

Important: this is a semantic mapping only. It does **not** authorize redesigning the production TimerDial UI, changing the Canvas geometry, changing the center circle, changing the inner ring, moving the 12 o'clock marker, replacing bottom controls, or changing the existing animation system.

Rules:

- Segments are split by internal target planned duration ratio.
- The outer ring is normalized to the current stage's target structure; it does not mean a fixed 60-second circle.
- Example: `动作 45s / 休息 15s` displays as 3:1. `动作 90s / 休息 30s` also displays as 3:1.
- The active internal target is shown as the thick active arc.
- Completed internal targets remain visible as elapsed / thin arcs.
- Future internal targets remain as thin upcoming arcs or low-emphasis segments.
- When execution moves to the next timed stage, the outer ring switches to that stage's internal target structure.
- If a timed stage has only one internal target, the outer ring becomes a single active segment.
- The outer ring should not display all stages in the workout. That job remains with the existing whole-session progress treatment.
- The implementation story must preserve existing production animation behavior: pause / resume morph, continuous projection, terminal freeze, reduce-motion snap / disable behavior, final countdown pulse, and rest-extension monotonic progress.

### Inner Ring

The inner ring continues to express **whole-session progress**.

Recommended mapping:

- Base progress is derived from the expanded total-stage execution timeline and real engine state.
- Expanded total-stage count includes warmup, each repeated stage instance in every round, round-rest instances between rounds, and cooldown.
- Example: warmup + 3 rounds x 2 repeated stages + 2 round rests + cooldown = 10 total stages.
- It remains monotonic through pause, resume, skip, and rest extension.
- It must not use visual-only fake progress.
- Completed / abandoned terminal states freeze.
- Ready state before `StartSession` does not progress.

### Center Circle

The center circle keeps the original accepted UI role and expresses the **current active stage / internal target** as compactly as the current design allows.

It should show:

- Current stage icon. In mocks, use a real pause / resume icon treatment, not text such as `Ⅱ`, because that can read as a Roman numeral instead of the existing pause control.
- Current stage label or compact sequence number.
- Current stage remaining time.
- Pause / resume affordance.

It should not show heart rate, trend data, action-library teaching content, or target settings.

### 12 O'clock Number Marker

Updated recommendation after user correction:

- Keep the original accepted 12 o'clock marker behavior for now.
- Do not introduce a new visible marker semantic in this visual prototype.
- If target-level orientation is needed, use accessibility text or secondary copy such as current internal target index / target count, but do not redesign the dial around it.

### Rest, Round Rest, And Cooldown

Rest can appear in two ways:

- As an internal target inside a work stage, such as `开始 / 加速 / 休息`.
- As existing `轮间休息` configuration in the current top-side editor position.

Both are valid, but they must map differently:

- Internal rest contributes to the current stage's outer-ring segmentation.
- Existing `轮间休息` remains round-level configuration unless a later model story changes it.
- `放松` / `Cooldown` is a timed stage with one or more internal targets.

### Rest Extension

`+15s` remains "extend current active rest step". It does not insert a new stage, does not modify the original plan, and does not rewrite the plan snapshot.

Visual progress rules:

- The active rest segment may extend its remaining time, but the displayed segment progress must keep a progress floor at the moment of extension.
- The inner ring total progress must not decrease.
- If duration-ratio math would reduce progress after extension, the UI mapping should clamp to the previous displayed progress and continue forward from there.
- `timedRestExtensionRecords` remains the only actual record of extra rest.

### Paused And Completed

- Paused freezes outer ring, inner ring, center countdown, and continuous projection.
- Completed sets total progress to complete and freezes visual state.
- Abandoned freezes at the last real state and must not animate to completion.

### Semantics That Must Stay Unchanged

- Center circle remains pause / resume.
- Bottom controls remain skip / `+15s` / end, with end confirmation.
- `+15s` remains only active rest extension.
- Sound, vibration, and final countdown continue to consume existing `WorkoutEvent` / cue settings.
- TimerDial motion remains UI projection only; it does not drive engine time, commands, records, or events.

## Data / Compatibility Impact

### Current Model Assessment

Current `WorkoutPlan.blocks` can represent:

- Top-level warmup / cooldown / rest blocks.
- Timed circuit blocks with rounds, `restBetweenRoundsSec`, and `TimedExerciseItem` stages.
- Stage-level name, type, icon, color, duration, rest-after, cue, and auto-advance.

It cannot fully represent:

- Arbitrary internal targets under each existing timed stage as a durable, explicit contract.
- Internal target color and stable target ids for history and trend grouping.
- Round-level `restBetweenRoundsSec` as a normal movable stage without changing its existing meaning.
- A stage-level total duration derived from internal targets while preserving a stable stage identity.

### Room

This gate does not change Room.

Future model story must determine:

- If existing `workout_plans.blocks_json` and `workout_sessions.plan_snapshot_json` can store a new polymorphic block without database table changes.
- Whether serializers / adapters need versioning to read old blocks and new composition blocks.
- Whether Room schema version changes are needed only because serialized JSON shape changes, or only if entity columns / DAO relations change.

Do not assume "JSON field" means "no migration work". It may avoid table migration, but compatibility tests and schema export review are still required.

### Plan Snapshot

Historical snapshots must remain immutable.

Recommendations:

- Old `WorkoutSession.planSnapshot` should continue to render through the old adapter.
- New composition snapshots should store stable target and stage ids.
- Editing a current plan must never rewrite old session snapshots.
- If the same plan id changes from old structure to composition structure, historical sessions still explain the old structure through their saved snapshot.

### Historical Trend Keys

Current timed comparable rest trends compare strict keys from old snapshot structure:

- structure signature
- round index
- step index
- rest stage id
- previous stage id

Future composition must update comparison rules:

- Include composition version in the structure signature.
- Prefer stable target id + stage id + round / target-instance index.
- Do not compare old and new structures unless a compatibility mapper proves they are equivalent.
- If ids are missing or structure changed, show data insufficient rather than making a hard comparison.

### Old Plan Display / Editing / Execution

Old saved single-layer timed plans should:

- Open normally.
- Display through a compatibility wrapper that preserves the current round / rest-between-rounds / stage composition layout.
- Execute through the existing engine until an implementation story explicitly changes execution mapping.
- Preserve current snapshot behavior.
- Not be silently rewritten just because the user viewed the editor.

Recommended old-plan wrapper:

| Old structure | Two-level display |
|---|---|
| `WarmupBlock` | Stage `热身`, internal target from `items` or block duration |
| `TimedCircuitBlock` | Existing editable stage sequence, with each stage optionally expandable into internal targets |
| `restBetweenRoundsSec` | Keep in top-side round / rest-between-rounds position |
| `RestBlock` | Rest stage with one internal target |
| `CooldownBlock` / `StretchBlock` | Stage `放松` / `拉伸`, internal target from `items` or duration |

Save behavior recommendation:

- Viewing old plan: no rewrite.
- Editing old plan in compatibility mode before model story: save back old structure only.
- Editing old plan after composition model is approved: ask or clearly indicate that saving converts the current plan to the new composition structure.
- Existing sessions remain old snapshots regardless of current plan conversion.

## Editor Visual / Interaction Draft

Timed stage collapsed state:

```text
[color] 阶段一 · 高强度工作          60s
                                      [drag]
```

Timed stage expanded state:

```text
阶段一 · 高强度工作
[阶段名称] [color swatch only]

阶段内目标
01 动作      45s   [color] [设置] [drag]
02 休息      15s   [color] [设置] [drag]

[增加目标 · 最多 5 个]
外圈预览：按阶段内目标时长占比分段
```

Rules:

- Timed stage cards default collapsed except the newly added or currently edited stage.
- Internal targets show only after expansion.
- Stage drag handle stays on the stage card header.
- Internal target drag handle stays inside the expanded target row.
- Internal target fold / unfold control stays separate from the drag handle.
- Use the same E14.4-2 finger-anchored placeholder preview model: finger anchor + placeholder preview + commit on release.
- Stage and target drag must not be active at the same time.
- Color entry shows the color swatch directly. Do not show Chinese color names in the field.
- On small screens, internal target rows should be compact. Duration is editable through a stepper/input row; color opens the palette instead of showing inline color text.
- Stage list rows must not depend on long text. Keep stage header copy to name + total duration, enforce or warn on long names, and ellipsize single-line overflow.

## E14.4-2b-6 TimerDial Mapping Planning Result

The focused planning gate is recorded in:

```text
docs/testing/e14-4-2b-6-timerdial-mapping-planning-gate.md
```

Accepted mapping plan:

- Current production TimerDial mapping remains legacy work/rest-cycle based; v2 outer-ring mapping is not implemented yet.
- Existing `TimerDialUiState` already expresses total progress, current stage progress, outer segments, inner marker data, the 12 o'clock total-count marker, center countdown / pause control content, and E14.5 smooth identity / anchor inputs.
- V2 mapping inputs should come from adapter-expanded timeline metadata, including `timelineStageId`, `timelineStageKind`, `stageInstanceIndex`, `targetInstanceIndex`, `stageGroupId`, `targetId`, `targetKind`, `roundIndex`, `stageGroupIndex`, `targetIndex`, `plannedDurationSec`, `displayName`, `colorHex`, and work/rest flags.
- Inner ring continues to express total stage progress for the whole workout.
- The 12 o'clock number marker continues to express total inner stage count, not target count.
- V2 total stage count should be warmup + rounds * stageGroups + between-round rests + cooldown, ignoring absent zero-duration boundaries.
- Outer ring for a v2 `stageGroup` expresses 1-5 targets by planned duration ratio: 1 target is full ring, 2 targets split by duration ratio, 3-5 targets split by each target's `plannedDurationSec`.
- `action`, `custom`, and `rest` targets all participate in the target-ratio outer ring.
- Warmup, cooldown, and synthetic between-round rest are not stageGroup targets and should use single-segment current-stage / legacy-like fallback.
- `+15s` does not recalculate planned ratio, insert a target, create a sixth segment, mutate the snapshot, or change session record shape; progress must remain monotonic.
- E14.5 continuous progress remains independent. Tick-updated progress / remaining inputs must stay out of smooth animation identity.
- No Room migration, session record model change, engine change, timeline adapter change, command change, or event change is required by this mapping plan.

## Follow-Up Implementation Split

Recommended story split:

| Slice | Goal | Notes |
|---|---|---|
| E14.4-2b-1 visual prototype / mock | Validate current-stage cards, internal target rows, color swatch-only entries, drag separation, current top round / round-rest placement, and TimerDial outer-ring segmentation sketch. | Can use `.local/smoke/e14-4-2b-timed-composition-timerdial-semantics/`; no production code. |
| E14.4-2b-2 data model decision | Completed: chose versioned timed composition payload inside existing plan/snapshot JSON first. | Updated data contracts and decision log. |
| E14.4-2b-3 serializer / model and editor adapter foundation | Restart implemented. | Model / serializer / compatibility draft adapter only; no UI, engine, TimerDial, Room migration, APK, or smoke output. |
| E14.4-2b-4 editor UI visual/code gate | Restart implemented and pushed. | Saved editor-side v2 payload and originally kept v2 start disabled; E14.4-2b-5b-3 later replaced that with adapter-expandable / fail-closed start gating. |
| E14.4-2b-5 engine timeline planning gate | Docs-only complete. | Plans expansion, stable metadata, legacy/v2 coexistence, rest extension, records, commands/events, TimerDial input, and E12 impact. |
| E14.4-2b-5a timeline adapter model/tests | Expand composition v2 into deterministic timeline steps in a pure adapter. | No production engine or TimerDial mapping. |
| E14.4-2b-5b engine integration | Implemented through split gates. | Preserves command, event, sound cue, ready gate, pause, skip, and `+15s` semantics. |
| E14.4-2b-5c session record compatibility tests | Implemented. | Verified v2 snapshot, actual step records, rest extension records, and history mapper compatibility without new persisted metadata. |
| E14.4-2b-6 TimerDial mapping planning gate | Docs-only complete. | Planned current state, v2 inputs, inner count, outer 1-5 target ratio, fallback stages, rest extension, E14.5 boundary, tests, split, and rollback. |
| E14.4-2b-6a TimerDial mapping model/state tests | Next. | Test first; no direct production mapping changes. |
| E14.4-2b-6b TimerDial production mapping implementation | Keep the original TimerDial UI and map only the outer ring to the active stage's internal targets by duration ratio. | Must preserve pause freeze, terminal freeze, reduce-motion, final countdown, E14.5 identity, and rest extension monotonic progress. |
| E14.4-2b-6c smoke / visual QA | Verify rendered behavior. | Cover v2 1 target, 2 targets, 3-5 targets, work/rest/custom, between-round rest, warmup, cooldown, legacy plan, pause/resume, rest extension, and reduce-motion. |
| E14.4-2b-7 migration / compatibility / E12 trend polish | Cover old plans, old snapshots, new composition, unsupported versions, current plan conversion, serializer round-trip, timeline expansion, TimerDial mapping, and E12 trend keys if not already covered. | Required before any schema or broad trend behavior change. |

Future v2 TimerDial work must start with E14.4-2b-6a tests. Do not fake TimerDial support and do not jump straight to production mapping.

## Gate Conclusion

This gate retains two-layer timed composition as a product and planning direction. E14.4-2b-2 remains the accepted data model planning decision. E14.4-2b-3 has been restarted and implemented only as a model / serializer / editor adapter foundation. E14.4-2b-4 has been restarted and implemented only as an editor UI visual/code gate. E14.4-2b-5 through E14.4-2b-5c have closed the adapter timeline, engine bridge, v2 start, and session record compatibility path. E14.4-2b-6 has now completed a docs-only TimerDial mapping planning gate; the next allowed step is E14.4-2b-6a TimerDial mapping model/state tests, not direct production TimerDial mapping.
