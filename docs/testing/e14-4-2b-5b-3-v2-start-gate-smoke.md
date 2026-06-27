# E14.4-2b-5b-3 V2 Start Gate Enablement + Smoke

Date: 2026-06-27

## Scope

This slice opens the timed composition v2 start gate only after the minimum engine bridge can expand the v2 payload into existing timed engine steps.

Allowed implementation:

- Enable `开始训练` for adapter-expandable timed composition v2 drafts in the timed composition editor sticky action.
- Enable saved v2 timed composition plans from plan detail / plan entry when the same adapter-expandable condition is true.
- Keep unsupported or truly empty v2 payloads fail-closed.
- Reuse the existing timed workout execution page and ready gate.

Out of scope:

- TimerDial production mapping or outer-ring semantic mapping.
- Room schema or migrations.
- Session record model changes.
- `WorkoutCommand` or `WorkoutEvent` changes.
- Heart-rate UI, manual heart-rate input, average heart-rate trend, BLE, Huawei SDK, Health Connect, HealthKit, or Wear OS.
- E12 trend implementation.
- Debug seed / smoke seed restoration.

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
- `docs/testing/e14-4-2b-5b-engine-integration-planning-gate.md`
- `docs/testing/e14-4-2b-5b-1-engine-adapter-bridge-tests.md`
- `docs/testing/e14-4-2b-5b-2-minimum-engine-bridge.md`
- `docs/setup.md`
- `docs/new-computer-setup.md`
- Android emulator QA skill
- Local `skills/bmad-method/SKILL.md`
- `huashu-design` skill, only for existing UI discipline

## Implementation

- Added a narrow plan startability helper that asks `TimedCompositionTimelineAdapter.expand(...)` whether every v2 block produces executable steps.
- Updated the timed composition editor state so a valid v2 draft can start and no longer reports the old execution-mapping-disabled copy.
- Updated plan detail state so saved valid v2 timed composition plans expose the same normal start action as legacy timed plans.
- Preserved fail-closed behavior for unsupported v2 versions and empty timelines.

## Focused Tests

Covered by unit tests:

- A default v2 editor draft can start after the bridge.
- Saving a v2 draft keeps start available.
- Legacy timed start behavior remains unchanged.
- Unsupported v2 and empty v2 timelines cannot start.
- The old debug / smoke seed entry terms are not restored.

## Android Smoke

Evidence directory:

```text
.local/smoke/e14-4-2b-5b-3-v2-start-gate-smoke/
```

Result: passed on AVD `TrainFlow_Pixel_API_36`.

Smoke coverage:

- `01_editor.xml` shows the v2 plan editor sticky `开始训练` action enabled and clickable.
- The old disabled copy `待执行映射完成后可开始` is absent from the editor UI tree.
- `02_ready.xml` confirms tapping start enters the existing timed workout ready gate.
- `03_running.xml`, `04_paused.xml`, and `05_resumed.xml` cover running, pause, and resume.
- `06_after_skip.xml` confirms skip advances from warmup to the next executable step without crashing.
- `07_rest.xml`, `10_skip_to_rest_1.xml`, `11_rest_extend_confirm_retry.xml`, and `12_rest_extended_retry.xml` cover rest state and rest extension confirmation without crashing.
- UI tree forbidden-term scan covered `COMPOSITION_V2`, `TimedComposition`, `午间 18 分钟间歇`, `HeartRatePanel`, `ManualHeartRate`, `未获取心率`, `手动心率`, `平均心率趋势`, and the old disabled copy. Result: no matches.
- AVD was shut down after smoke; `adb devices` returned an empty device list.

Smoke observation:

- During the run, the existing TimerDial ring fill appeared to advance in visible one-second steps. That is a TimerDial continuous progress / animation follow-up, not part of this v2 start gate slice. No TimerDial files were changed in this task.

## Verification

Executed:

```powershell
. .\.local\env.ps1
.\gradlew.bat app:testDebugUnitTest --tests "*TimedComposition*" --no-daemon --console=plain
.\gradlew.bat app:testDebugUnitTest --no-daemon --console=plain
.\gradlew.bat app:assembleDebug app:lintDebug --no-daemon --console=plain
git diff --check
git diff --cached --name-only
rg -n "COMPOSITION_V2|TimedCompositionEditorSmokePlanId|withTimedCompositionEditorSmokePlan|initialSelectedPlanId|午间 18 分钟间歇" app/src/main app/src/test
rg -n "TimedCompositionTimeline|TimedCompositionTimelineAdapter|timelineStageId|targetInstanceIndex" app/src/main/java/com/liujyks/trainflow/feature/timer app/src/main/java/com/liujyks/trainflow/ui
rg -n "TimedCompositionTimeline|TimedCompositionTimelineAdapter|timelineStageId|targetInstanceIndex" app/src/main app/src/test
rg -n "HeartRatePanel|ManualHeartRate|manualHeartRate|averageHeartRateTrend|heartRateUnavailableText|未获取心率|手动心率" app/src/main app/src/test docs DESIGN.md DEV_STORY_PROMPT_TEMPLATE.md CODE_REVIEW_PROMPT_TEMPLATE.md
```

Results:

- Focused `*TimedComposition*` tests passed.
- Full debug unit tests passed.
- `assembleDebug` and `lintDebug` passed.
- `git diff --check` passed.
- Pre-commit staged files were empty before precise staging.
- Old debug / smoke seed entry search returned no matches.
- `feature/timer` does not exist; `ui` route search returned no timeline metadata matches.
- Engine bridge boundary search matched only the core model adapter, minimum engine bridge, start gate helper, and tests.
- Heart-rate search matched existing boundary docs / regression-test references only; this task did not restore production heart-rate UI/input/statistics.
- The required forbidden design-skill search returned no matches.

## Boundary Notes

- No TimerDial files are changed.
- No Room schema / migration files are changed.
- No session record model files are changed.
- `WorkoutCommand` and `WorkoutEvent` are unchanged.
- Heart-rate UI/input/statistics remain removed for the first version.
- `.local/verification` is not used.
- `.local/smoke` evidence is local-only and must not be staged or committed.
