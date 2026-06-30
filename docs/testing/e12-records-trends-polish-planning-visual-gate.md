# E12 records / trends polish planning and visual gate

**Date:** 2026-06-30
**Scope:** planning / audit / visual proposal only
**Status:** Ready for main-thread review before implementation

## 1. Scope

This gate prepares the E12 records / trends polish implementation. It does not change Kotlin, Compose, Room, engine, tests, resources, APK packaging, TimerDial, `WorkoutCommand`, `WorkoutEvent`, or `WorkoutSession` semantics.

The focus is the records page as a training data tool: clear record explanation, trustworthy trend grouping, chart semantics, mobile-first visual hierarchy, and explicit empty / insufficient-data states.

## 2. Inputs read

Required repo context:

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
- `docs/testing/e14-4-2b-closeout.md`
- `docs/testing/e14-4-2b-5c-session-record-compatibility.md`
- `docs/testing/e14-6-2b-completion-recap-page-compose.md`
- `docs/testing/e14-6-3e-stage-style-timerdial-visual-qa.md`
- `docs/setup.md`
- `docs/new-computer-setup.md`

Additional context read because it anchors product / UX boundaries:

- `docs/planning/product-brief.md`
- `docs/planning/prd.md`
- `docs/planning/ux-design.md`
- `docs/readiness-report.md`

Skills read:

- `huashu-design` plus relevant `references/content-guidelines.md`, `references/verification.md`, and `references/critique-guide.md`
- `skills/bmad-method/SKILL.md`
- `test-android-apps:android-emulator-qa/SKILL.md` only to confirm this round is not an install / smoke run

Forbidden local design skill:

- The repository-local design skill that was explicitly disallowed for this turn was not used.

## 3. Source audit

Focused code areas:

- `app/src/main/java/com/liujyks/trainflow/feature/history/HistoryUiState.kt`
- `app/src/main/java/com/liujyks/trainflow/feature/history/HistoryRoute.kt`
- `app/src/main/java/com/liujyks/trainflow/core/model/WorkoutSession.kt`
- `app/src/main/java/com/liujyks/trainflow/core/model/TimedCompositionTimeline.kt`
- `app/src/main/java/com/liujyks/trainflow/core/data/WorkoutPlanSnapshotStorageJson.kt`
- `app/src/main/java/com/liujyks/trainflow/core/data/WorkoutSessionRepository.kt`
- `app/src/main/java/com/liujyks/trainflow/core/database/entity/WorkoutSessionEntity.kt`
- `app/src/main/java/com/liujyks/trainflow/core/database/dao/WorkoutSessionDao.kt`
- `app/src/main/java/com/liujyks/trainflow/feature/workoutsession/WorkoutSessionRecordMappers.kt`
- `app/src/main/java/com/liujyks/trainflow/feature/workoutsession/TimedWorkoutSummaryUiState.kt`
- `app/src/main/java/com/liujyks/trainflow/feature/workoutsession/StrengthWorkoutSummaryUiState.kt`

Focused tests:

- `app/src/test/java/com/liujyks/trainflow/feature/history/HistoryUiStateTest.kt`
- `app/src/test/java/com/liujyks/trainflow/feature/workoutsession/WorkoutSessionRecordMappersTest.kt`
- `app/src/test/java/com/liujyks/trainflow/feature/workoutsession/TimedCompositionSessionRecordCompatibilityTest.kt`
- `app/src/test/java/com/liujyks/trainflow/core/model/TimedCompositionTimelineAdapterTest.kt`
- `app/src/test/java/com/liujyks/trainflow/core/data/TimedCompositionStorageJsonTest.kt`
- `app/src/test/java/com/liujyks/trainflow/core/data/WorkoutSessionRepositoryTest.kt`

## 4. Current capability

Records already consume real Room `WorkoutSession` data. Existing E12 work covers:

- true persisted session list, date grouping, selected session detail, and cleanup confirmation flow;
- total session count, completed / abandoned counts, total elapsed, effective elapsed, paused elapsed, planned rest, actual rest, extra rest, and mode breakdown;
- aggregate daily trends by `startedAt` date with no fake line when fewer than two date points exist;
- legacy timed comparable rest trend for same structure / rest stage / stage order / round / step index / restStageId / previousStageId;
- strength comparable set trend for same `exerciseId`, same `sourceSetPlanId` when present, and only `sourceSetPlanId == null` fallback to `setOrder + setKind`;
- tests that ensure no average heart-rate trend, no medical copy, no automatic strength judgement, and no fake samples.

Current records are stored without a schema gap for this polish:

- `WorkoutSessionEntity` stores status, plan snapshot JSON, started / ended timestamps, total / effective / paused elapsed.
- `session_step_records` store step id, step kind, skipped flag, actual duration, and currently unused metadata columns.
- `timed_rest_extension_records` store extra-rest position and added seconds.
- `strength_set_records` store planned / actual set values, active duration, actual rest, effort, source set id, and substitution source.

## 5. Main audit findings

### Data semantics

1. Existing timed comparable trend mapper is legacy `TimedCircuitBlock`-centric. `HistoryUiState.toTimedComparableRestDescriptors()` rebuilds descriptors from `RestBlock`, `TimedCircuitBlock`, warmup / stretch / cooldown legacy timed items, and a legacy-style structure signature.
2. Existing v2 timed composition history compatibility tests only require "can read without crash" and currently expect the v2 timed comparable trend to have no groups. That is correct as a fail-closed baseline, but it is not enough for E12 polish.
3. `WorkoutSession.timedDetailRows()` counts planned timed steps only from `TimedCircuitBlock`; v2 composition sessions need a clearer stage / target explanation based on `TimedCompositionTimelineAdapter`.
4. `plannedRestSec()` currently sums legacy `RestBlock`, legacy `TimedCircuitBlock` rest, and strength rest. It does not yet describe v2 internal rest targets and synthetic between-round rests via the v2 timeline adapter.
5. Strength comparable semantics are already strong and test-covered. The polish should improve presentation, not loosen comparison keys.

### UI and visual hierarchy

1. `HistoryRoute` currently renders a long vertical sequence of `HistoryCard` sections: stats, chart header, four chart cards, mode breakdown, timed comparable card, strength comparable card, cleanup, date list, detail, then basic trend reference cards.
2. The current lightweight chart canvas has a baseline, lines, dots, and legend rows, but no explicit y-axis ticks / unit label or x-axis date labels inside the chart surface.
3. `actionTrend` and `volumeTrend` are useful legacy strength summaries, but visually compete with the newer strength comparable trend and should be nested under the strength record surface or treated as secondary references.
4. Cleanup sits above the record list. For a training data tool, destructive management should be lower priority than overview, filters, recent sessions, and trend interpretation.
5. Empty and data-insufficient copy exists, but should be promoted into chart-level states with consistent visual affordances rather than being only body text inside a card.

## 6. Data semantics proposal

### Common session semantics

- `completed`: terminal training record that finished normally.
- `abandoned`: terminal training record ended early; it still participates in count, elapsed, pause, rest, skipped, and detail explanations, but should remain visually distinct from completed.
- `skipped`: step or set was skipped during execution. It is not a failure label and should not imply judgement.
- `pause`: time in `pausedElapsedSec`; separate from extra rest and actual rest.
- `rest extension`: extra timed rest from `timedRestExtensionRecords.addedSec`; it does not rewrite planned rest, does not become pause time, and does not create a new target.
- `total elapsed`: wall-clock elapsed or persisted `totalElapsedSec`.
- `effective elapsed`: persisted active/effective time; excludes pause.
- `planned rest`: reconstructed from the historical session snapshot, never from the current editable plan.
- `actual rest`: timed rest step actual duration or strength set `actualRestAfterSec`.

### Legacy timed session

Legacy timed records are sessions whose historical snapshot uses `TimedCircuitBlock` / legacy timed item blocks. Their comparable rest key may continue to use:

- legacy structure signature;
- `stageType == REST`;
- stage order;
- round index;
- step index;
- rest stage id;
- previous stage id;
- planned rest duration.

Legacy and v2 timed composition must not share a trend curve unless a future equivalence mapper proves that a legacy item and a v2 target are the same training intent and structure instance.

### Timed composition v2 session

V2 records should be explained by expanding historical `TimedCompositionBlock` snapshots through `TimedCompositionTimelineAdapter`. The v2 comparable key should include at least:

- `compositionVersion`;
- `compositionBlockId`;
- `timelineStageKind`;
- `stageGroupId`;
- `targetId`;
- `targetKind`;
- `roundIndex`;
- `stageGroupIndex`;
- `targetIndex`;
- `stageInstanceIndex`;
- `targetInstanceIndex`;
- `plannedDurationSec`;
- a v2 structure signature derived from the ordered timeline, not from the current plan.

V2 planned rest should include rest targets and synthetic between-round rests that the adapter emits as rest steps. Warmup and cooldown are work-like boundary stages unless represented as rest targets by the model.

If a historical v2 snapshot is unsupported, empty, or cannot be expanded safely, records should remain visible in the recent list and detail page, but comparable trend grouping should show data insufficient / unsupported composition state instead of mixing it with legacy or other v2 structures.

### Strength session

Strength trend semantics should remain:

- only `WorkoutMode.STRENGTH`;
- same `exerciseId`;
- prefer same `sourceSetPlanId`;
- use `setOrder + setKind` only when `sourceSetPlanId` is absent;
- if `sourceSetPlanId` exists but cannot be found in the relevant snapshot block, show data insufficient, not fallback;
- substitutions form a separate trend under actual `exerciseId` and must label `substitutedFromExerciseId`;
- planned weight / reps come from record fields or the historical snapshot;
- actual weight / reps, active duration, actual rest, and effort come from true set records.

## 7. Information architecture proposal

Recommended mobile-first order:

1. **Overview strip**
   Compact summary, not a stack of large cards: sessions, completed / abandoned, total elapsed, effective elapsed, paused, planned rest, actual rest, extra rest.
2. **Filters**
   Segmented controls for all / timed / strength / follow-along and completed / abandoned / all. A small time-window control may be introduced only if it filters existing records honestly.
3. **Recent sessions**
   Dense list first, because records are the main object. Each row should show mode, status, date, duration, key summary, pause, skipped, rest extension where present.
4. **Selected session detail**
   Inline detail for the selected row. For timed v2, show composition v2 stage / target interpretation; for legacy timed, show legacy timed structure; for strength, show confirmed set summary and planned-vs-actual.
5. **Aggregate trends**
   Daily count/status, elapsed, rest, and mode distribution. These are broad trends and should be visually separate from comparable per-stage / per-set trends.
6. **Comparable timed trends**
   Show legacy timed and v2 timed composition as separate groups. If only one family has enough samples, show only that family. If both exist, do not overlay them.
7. **Comparable strength trends**
   Same exercise / same source-set or fallback group rows, with plan / actual / rest / effort.
8. **Data quality notes**
   A compact "not included" section: insufficient dates, unsupported v2 version, missing step records, malformed rest extension position, missing planned set values, missing actuals.
9. **History management**
   Cleanup remains available but lower priority and clearly destructive.

## 8. Chart UI proposal

All trend charts should expose:

- x-axis label and visible date ticks;
- y-axis unit label: sessions, seconds/minutes, sets, reps, kg, kg-reps, or percent;
- y-axis min at 0 for count and duration charts;
- legend with series color and latest visible value;
- empty state for no records;
- data-insufficient state for fewer than two comparable points;
- data-quality explanation when records are excluded;
- no fake interpolation or smoothing when the data is sparse.

Recommended chart types:

- Daily sessions: compact line or bar chart, y-axis in sessions.
- Completed / abandoned: paired bars or two-line chart, y-axis in sessions.
- Elapsed time: line chart with total / effective / paused, y-axis in minutes.
- Rest: line chart with planned / actual / extra, y-axis in minutes or seconds depending range.
- Mode distribution: horizontal bars with counts and percentage.
- Timed comparable rest: small multiple rows per comparable key; each row can show planned vs actual vs extra as aligned bars rather than a large decorative line chart.
- Strength comparable set: table-like trend rows plus optional sparkline for actual weight / reps when at least two comparable samples exist.

## 9. Visual direction

The records page should feel like a training data workstation:

- light, calm, high-density enough for repeated scanning;
- restrained typography, no hero section, no decorative image, no marketing layout;
- no nested cards and no long chain of identical large cards;
- sections use full-width bands, dividers, compact tables, and row groups;
- status chips are functional, not ornamental;
- chart labels must fit on small screens;
- legend and units should stay visible without needing explanatory tutorial text.

Local visual proposal:

- `.local/smoke/e12-records-trends-polish-visual-gate/index.html`

The HTML preview is a mock for visual hierarchy only. It is not production UI, not a data fixture, and should not be committed.

## 10. Recommended implementation split after approval

1. **E12.4a v2 timed record interpretation**
   Add v2 snapshot expansion helpers for record detail, planned rest, and comparable rest descriptors. Keep legacy mapper separate.
2. **E12.4b trend key hardening**
   Introduce explicit legacy timed and v2 timed comparable key families. Add tests proving no legacy/v2 mixing.
3. **E12.4c records page IA polish**
   Reorder records page around overview, filters, recent sessions, detail, trends, data quality, then cleanup.
4. **E12.4d chart visual polish**
   Add explicit axis labels, units, date ticks, legend consistency, empty / insufficient states, and mobile text overflow checks.
5. **E12.4e visual QA**
   Run Android screenshot-level audit only after implementation. Store screenshots and logs under `.local/smoke`.

## 11. Later stories

Postpone:

- equivalence mapper that proves selected legacy timed stages equal selected v2 targets;
- advanced time window analytics;
- export / share;
- cloud sync;
- multi-plan comparative coaching;
- persisted heart-rate summaries and average heart-rate trends, unless a later accepted health-data decision changes the product boundary;
- device integrations such as BLE, Huawei SDK, Health Connect, HealthKit, or Wear OS;
- medical advice, risk alerts, or training interruption decisions.

## 12. Acceptance gate for implementation start

Implementation can start after main-thread confirmation of:

- this page order and chart hierarchy;
- strict legacy timed vs v2 timed composition separation;
- v2 trend key fields;
- whether E12.4a should be code-first data semantics or UI-first IA polish.

Until then, this round remains docs / audit / visual proposal only.
